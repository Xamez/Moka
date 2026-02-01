import fr.xamez.moka.MokaApp;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

@QuarkusMain
public class Main {
    public static void main(String... args) {
        Quarkus.run(App.class, args);
        Quarkus.waitForExit();
    }

    public static class App implements QuarkusApplication {

        @Inject
        MokaApp mokaApp;

        @Override
        public int run(String... args) {
            mokaApp.run();
            return 0;
        }
    }
}