package fr.xamez.moka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WindowManager {

    private static final Logger LOG = Logger.getLogger(WindowManager.class);

    @Inject
    MokaAppConfig config;

    public Shell createAndConfigureShell(Display display) {
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
}