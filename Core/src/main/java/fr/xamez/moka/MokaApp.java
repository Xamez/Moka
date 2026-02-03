package fr.xamez.moka;

import fr.xamez.moka.tool.NativeNotification;
import fr.xamez.moka.tool.NotificationType;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MokaApp {

    private static final Logger LOG = Logger.getLogger(MokaApp.class);

    @Inject
    SwtUtils swtUtils;

    @Inject
    WindowManager windowManager;

    @Inject
    MenuBuilder menuBuilder;

    @Inject
    BrowserBuilder browserBuilder;

    @Inject
    BridgeRouter bridgeRouter;

    @Inject
    NativeNotification nativeNotification;

    @Inject
    MokaAppConfig config;

    private Shell shell;

    public Shell getShell() {
        return shell;
    }

    void onStart(@Observes StartupEvent ev) {
        swtUtils.loadNativeLibrary();
        bridgeRouter.setup();
    }

    public void run() {
        Display display = new Display();
        Display.setAppName(config.name());
        Display.setAppVersion(config.version());
        try {
            Shell shell = windowManager.createAndConfigureShell(display);
            this.shell = shell;

            menuBuilder.setupMenu(shell);
            browserBuilder.setupBrowser(shell);

            shell.open();
            runEventLoop(display, shell);
        } catch (Throwable t) {
            LOG.error("Fatal error occurred in Moka application", t);
        } finally {
            shutdown(display);
        }
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

    public void showNotification(String title, String message, NotificationType type) {
        nativeNotification.show(shell, title, message, type);
    }
}