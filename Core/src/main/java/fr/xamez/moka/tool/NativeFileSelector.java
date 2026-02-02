package fr.xamez.moka.tool;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import java.util.List;

public class NativeFileSelector {

    public static String selectFile(Shell parentShell, List<String> filterExtensions, List<String> filterNames, String initialPath) {
        FileDialog dialog = new FileDialog(parentShell, SWT.OPEN);

        dialog.setFilterExtensions(filterExtensions.toArray(new String[0]));
        dialog.setFilterNames(filterNames.toArray(new String[0]));

        dialog.setFilterPath(initialPath);

        return dialog.open();
    }
}