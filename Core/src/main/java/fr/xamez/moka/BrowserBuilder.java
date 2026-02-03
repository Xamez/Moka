package fr.xamez.moka;

import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jboss.logging.Logger;

@ApplicationScoped
public class BrowserBuilder {

    private static final Logger LOG = Logger.getLogger(BrowserBuilder.class);

    @Inject
    MokaAppConfig config;

    @Inject
    Vertx vertx;

    public void setupBrowser(Shell shell) {
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
                browser.execute(MokaBridge.getInjectionScript());
            }
        });
    }
}