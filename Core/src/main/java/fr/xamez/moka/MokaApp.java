package fr.xamez.moka;

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
import jakarta.inject.Inject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.internal.Library;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@ApplicationScoped
public class MokaApp {

    private static final Logger LOG = Logger.getLogger(MokaApp.class);

    @Inject
    MokaAppConfig config;

    @Inject
    Vertx vertx;

    @Inject
    Router router;

    void onStart(@Observes StartupEvent ev) {
        loadNativeLibrary();
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

        if (config.staticPath().isEmpty()) {
            router.route("/*").handler(StaticHandler.create());
        } else {
            String absPath = Path.of(config.staticPath().get()).toAbsolutePath().toString();
            router.route("/*").handler(StaticHandler.create(FileSystemAccess.ROOT, absPath));
        }

        router.route().last().handler(ctx -> {
            if (!ctx.response().ended() && ctx.request().getHeader("Accept").contains("text/html")) {
                ctx.reroute("/");
            } else {
                ctx.next();
            }
        });
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
        Shell shell = new Shell(display);

//        shell.addListener(SWT.Close, event -> {
//            LOG.info("Exit requested, shutting down...");
//            event.doit = true;
//        });

        shell.setText(config.title());
        shell.setSize(config.width(), config.height());
        shell.setFullScreen(config.fullscreen());
        if (config.setActive()) shell.getDisplay().asyncExec(shell::forceActive);
        shell.setLayout(new FillLayout());

        try {
            Browser browser = new Browser(shell, SWT.EDGE);
            new MokaBridge(browser, vertx.eventBus());

            browser.addProgressListener(new ProgressAdapter() {
                @Override
                public void completed(ProgressEvent event) {
                    browser.execute(JS_INJECTION);
                }
            });

            String port = System.getProperty("quarkus.http.port", "8080");
            String targetUrl = config.devUrl().orElse("http://localhost:" + port);
            LOG.infof("Loading front: %s", targetUrl);

            browser.setUrl(targetUrl);
            shell.open();

            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) display.sleep();
            }

            LOG.info("Event loop exited, initiating shutdown...");

        } catch (Exception e) {
            LOG.error("WebView error", e);
        } finally {
            if (!display.isDisposed()) {
                display.dispose();
            }
            LOG.info("Application exited.");
            Quarkus.asyncExit();
        }
    }

    private void loadNativeLibrary() {
        try {
            String fileName = getSwtFileName();

            Path tempDir = Files.createTempDirectory("moka-native");
            tempDir.toFile().deleteOnExit();
            Path targetPath = tempDir.resolve(fileName);

            try (InputStream is = MokaApp.class.getResourceAsStream("/" + fileName)) {
                if (is == null) {
                    throw new IllegalStateException("SWT native library not found in classpath: " + fileName);
                }
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            System.setProperty("swt.library.path", tempDir.toAbsolutePath().toString());

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize native SWT library", e);
        }
    }

    private String getSwtFileName() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String version = Library.getVersionString();

        if (os.contains("win")) {
            return "swt-win32-" + version + ".dll";
        } else if (os.contains("mac")) {
            String macArch = (arch.contains("aarch64") || arch.contains("arm")) ? "aarch64" : "x86_64";
            return "libswt-cocoa-macosx-" + macArch + "-" + version + ".dylib";
        } else {
            String linuxArch = arch.equals("amd64") ? "x86_64" : arch;
            return "libswt-gtk-linux-" + linuxArch + "-" + version + ".so";
        }
    }
}