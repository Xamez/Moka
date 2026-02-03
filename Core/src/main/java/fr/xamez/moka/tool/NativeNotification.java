package fr.xamez.moka.tool;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NativeNotification {

    private static final Logger LOG = Logger.getLogger(NativeNotification.class);

    public void show(Shell shell, String title, String message, NotificationType type) {
        if (shell == null || shell.isDisposed()) return;

        Display display = shell.getDisplay();
        display.asyncExec(() -> {
            Tray tray = display.getSystemTray();
            if (tray == null) {
                LOG.warn("System does not support system tray.");
                return;
            }

            TrayItem item = new TrayItem(tray, SWT.NONE);
            item.setImage(shell.getImage());

            ToolTip tip = new ToolTip(shell, SWT.BALLOON | type.style);
            tip.setText(title);
            tip.setMessage(message);
            tip.setAutoHide(true);

            item.setToolTip(tip);
            tip.setVisible(true);

            display.timerExec(10000, () -> {
                if (!item.isDisposed()) item.dispose();
                if (!tip.isDisposed()) tip.dispose();
            });
        });
    }
}