package fr.xamez.moka.tool;

import org.eclipse.swt.SWT;

public enum NotificationType {

    ERROR(SWT.ICON_ERROR),
    INFORMATION(SWT.ICON_INFORMATION),
    QUESTION(SWT.ICON_QUESTION),
    WARNING(SWT.ICON_WARNING),
    WORKING(SWT.ICON_WORKING);

    public final int style;

    NotificationType(int style) {
        this.style = style;
    }

}