import fr.xamez.moka.tool.MokaMenuBar;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class MenuConfig {

    @Inject
    FileUtils fileUtils;

    @Produces
    @ApplicationScoped
    public MokaMenuBar menuBar() {
        return MokaMenuBar.create()
                .add(MokaMenuBar.Menu.create("File")
                        .item("New", () -> System.out.println("New Clicked"))
                        .item("Open", () -> fileUtils.openFile("."))
                        .separator()
                        .item("Exit", () -> System.exit(0)))
                .add(MokaMenuBar.Menu.create("Edit")
                        .item("Copy", () -> System.out.println("Copy"))
                        .submenu(MokaMenuBar.Menu.create("Advanced")
                                .item("Reset", () -> System.out.println("Reset"))))
                .add(MokaMenuBar.Menu.create("Help")
                        .item("About", () -> System.out.println("About Clicked")));
    }
}