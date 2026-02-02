package fr.xamez.moka;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.swt.internal.Library;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@ApplicationScoped
public class SwtUtils {

    public void loadNativeLibrary() {
        try {
            String fileName = getSwtFileName();

            Path tempDir = Files.createTempDirectory("moka-native");
            tempDir.toFile().deleteOnExit();
            Path targetPath = tempDir.resolve(fileName);

            try (InputStream is = MokaApp.class.getResourceAsStream("/" + fileName)) {
                if (is == null) {
                    throw new IllegalStateException("SWT native library not found in classpath: " + fileName);
                }
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            System.setProperty("swt.library.path", tempDir.toAbsolutePath().toString());

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize native SWT library", e);
        }
    }

    private String getSwtFileName() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String version = Library.getVersionString();

        if (os.contains("win")) {
            return "swt-win32-" + version + ".dll";
        } else if (os.contains("mac")) {
            String macArch = (arch.contains("aarch64") || arch.contains("arm")) ? "aarch64" : "x86_64";
            return "libswt-cocoa-macosx-" + macArch + "-" + version + ".dylib";
        } else {
            String linuxArch = arch.equals("amd64") ? "x86_64" : arch;
            return "libswt-gtk-linux-" + linuxArch + "-" + version + ".so";
        }
    }

}