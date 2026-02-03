package fr.xamez.moka;

import fr.xamez.moka.tool.MokaMenuBar;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.jboss.logging.Logger;

import java.nio.file.Path;

@ApplicationScoped
public class MokaApp {

    private static final Logger LOG = Logger.getLogger(MokaApp.class);

    @Inject
    MokaAppConfig config;

    @Inject
    SwtUtils swtUtils;

    @Inject
    Vertx vertx;

    @Inject
    Router router;

    @Inject
    Instance<MokaMenuBar> menuBarInstance;

    private Shell shell;

    public Shell getShell() {
        return shell;
    }

    void onStart(@Observes StartupEvent ev) {
        swtUtils.loadNativeLibrary();
        setupBridge();
        setupStaticServing();
    }

    private void setupBridge() {
        router.route("/_moka/bridge").handler(ctx -> {
            ctx.response()
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .putHeader("Access-Control-Allow-Headers", "Content-Type");
            ctx.next();
        });

        router.post("/_moka/bridge").handler(BodyHandler.create()).handler(ctx -> {
            JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                ctx.response().setStatusCode(400).end("Missing body");
                return;
            }

            vertx.eventBus().request(body.getString("address"), body.getValue("payload", new JsonObject()), reply -> {
                if (reply.succeeded()) {
                    Object res = reply.result().body();
                    ctx.json(res != null ? res : new JsonObject());
                } else {
                    ctx.response().setStatusCode(500).end(new JsonObject().put("error", reply.cause().getMessage()).encode());
                }
            });
        });
    }

    private void setupStaticServing() {
        if (config.devUrl().isPresent()) return;

        StaticHandler staticHandler;
        if (config.staticPath().isEmpty()) {
            staticHandler = StaticHandler.create();
        } else {
            String absPath = Path.of(config.staticPath().get()).toAbsolutePath().toString();
            staticHandler = StaticHandler.create(FileSystemAccess.ROOT, absPath);
        }
        staticHandler.setCachingEnabled(config.enableBrowserCache());
        router.route("/*").handler(staticHandler);

        router.route().last().handler(ctx -> {
            boolean isNotRoot = !"/".equals(ctx.normalizedPath());
            String header = ctx.request().getHeader("Accept");
            if (header == null) header = "";
            boolean isHtmlRequest = header.contains("text/html");
            boolean responseNotEnded = !ctx.response().ended();

            if (responseNotEnded && isHtmlRequest && isNotRoot) {
                ctx.reroute("/");
            } else {
                ctx.next();
            }
        });
    }

    private void setupMenu(Shell shell) {
        if (menuBarInstance.isUnsatisfied()) {
            return;
        }
        MokaMenuBar mokaMenuBar = menuBarInstance.get();
        Menu bar = new Menu(shell, SWT.BAR);
        shell.setMenuBar(bar);

        for (MokaMenuBar.Menu mokaMenu : mokaMenuBar.getMenus()) {
            MenuItem header = new MenuItem(bar, SWT.CASCADE);
            header.setText(mokaMenu.getLabel());

            Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
            header.setMenu(subMenu);

            fillMenu(subMenu, mokaMenu);
        }
    }

    private void fillMenu(Menu swtMenu, MokaMenuBar.Menu mokaMenu) {
        for (MokaMenuBar.Item item : mokaMenu.getItems()) {
            if (item instanceof MokaMenuBar.Action(String label, Runnable callback)) {
                MenuItem mi = new MenuItem(swtMenu, SWT.PUSH);
                mi.setText(label);
                mi.addListener(SWT.Selection, e -> callback.run());
            } else if (item instanceof MokaMenuBar.Separator) {
                new MenuItem(swtMenu, SWT.SEPARATOR);
            } else if (item instanceof MokaMenuBar.Menu sub) {
                MenuItem mi = new MenuItem(swtMenu, SWT.CASCADE);
                mi.setText(sub.getLabel());
                Menu subSub = new Menu(swtMenu);
                mi.setMenu(subSub);
                fillMenu(subSub, sub);
            }
        }
    }

    private static final String JS_INJECTION = """
            if (!window.invoke) {
                window.__moka_callbacks = {};
                window.invoke = (address, payload) => new Promise((resolve, reject) => {
                    const requestId = Date.now().toString(36) + Math.random().toString(36).slice(2);
                    window.__moka_callbacks[requestId] = { resolve, reject };
                    window.__moka_invoke(address, JSON.stringify(payload || {}), requestId);
                });
            }
            """;

    public void run() {
        Display display = new Display();
        try {
            Shell shell = createAndConfigureShell(display);
            setupMenu(shell);
            setupBrowser(shell);

            shell.open();
            runEventLoop(display, shell);
        } catch (Throwable t) {
            LOG.error("Fatal error occurred in Moka application", t);
        } finally {
            shutdown(display);
        }
    }


    private Shell createAndConfigureShell(Display display) {
        int style;

        if (config.decorated()) {
            style = SWT.TITLE | SWT.MIN | SWT.CLOSE;
            if (config.resizable()) {
                style |= SWT.RESIZE | SWT.MAX;
            }
        } else {
            style = SWT.NO_TRIM;
        }

        Shell shell = new Shell(display, style);
        this.shell = shell;
        shell.setText(config.title());
        shell.setSize(config.width(), config.height());
        shell.setFullScreen(config.fullscreen());
        shell.setMaximized(config.maximized());

        if (config.center()) {
            centerShell(display, shell);
        }

        if (config.setActive()) {
            shell.getDisplay().asyncExec(shell::forceActive);
        }

        shell.setLayout(new FillLayout());
        setupIcon(display, shell);

        return shell;
    }

    private void centerShell(Display display, Shell shell) {
        Monitor primary = display.getPrimaryMonitor();
        Rectangle bounds = primary.getBounds();
        Rectangle rect = shell.getBounds();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 2;
        shell.setLocation(x, y);
    }

    private void setupIcon(Display display, Shell shell) {
        var iconStream = MokaApp.class.getResourceAsStream(config.iconUrl());
        if (iconStream != null) {
            Image image = new Image(display, iconStream);
            shell.setImage(image);
        } else {
            LOG.warnv("Icon resource not found: {0}", config.iconUrl());
        }
    }

    private void setupBrowser(Shell shell) {
        Browser browser = new Browser(shell, SWT.NONE);
        new MokaBridge(browser, vertx.eventBus());

        registerBrowserEvents(browser);

        String port = System.getProperty("quarkus.http.port", "8080");
        String targetUrl = config.devUrl().orElse("http://localhost:" + port);
        LOG.infof("Loading front: %s", targetUrl);

        browser.setUrl(targetUrl);
    }

    private void registerBrowserEvents(Browser browser) {
        browser.addOpenWindowListener(event -> {
            Shell newShell = new Shell(Display.getDefault());
            newShell.setLayout(new FillLayout());
            Browser newBrowser = new Browser(newShell, SWT.NONE);

            event.browser = newBrowser;
            newBrowser.addLocationListener(new LocationAdapter() {
                @Override
                public void changing(LocationEvent locationEvent) {
                    LOG.info("Opening external link: " + locationEvent.location);
                    Program.launch(locationEvent.location);
                    locationEvent.doit = false;
                    newShell.dispose();
                }
            });
        });

        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                browser.execute(JS_INJECTION);
            }
        });
    }

    private void runEventLoop(Display display, Shell shell) {
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
        LOG.info("Event loop exited, initiating shutdown...");
    }

    private void shutdown(Display display) {
        if (!display.isDisposed()) {
            display.dispose();
        }
        LOG.info("Application exited.");
        Quarkus.asyncExit();
        System.exit(0);
    }
}
