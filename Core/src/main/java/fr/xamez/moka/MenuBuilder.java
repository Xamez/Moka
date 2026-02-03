package fr.xamez.moka;

import fr.xamez.moka.tool.MokaMenuBar;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

@ApplicationScoped
public class MenuBuilder {

    @Inject
    Instance<MokaMenuBar> menuBarInstance;

    public void setupMenu(Shell shell) {
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
}