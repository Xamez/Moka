import fr.xamez.moka.MokaApp;
import fr.xamez.moka.tool.NativeFileSelector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class FileUtils {

    @Inject
    MokaApp mokaApp;

    public String openFile(String path, List<String> filterExtensions, List<String> filterNames) {
        final String[] result = {null};
        mokaApp.getShell().getDisplay().syncExec(() -> {
            result[0] = NativeFileSelector.selectFile(mokaApp.getShell(), filterExtensions, filterNames, path);
        });
        return result[0];
    }

    public String openFile(String path) {
        return openFile(path, List.of(), List.of());
    }

}