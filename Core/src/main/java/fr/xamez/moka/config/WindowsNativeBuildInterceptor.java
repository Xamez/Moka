package fr.xamez.moka.config;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import jakarta.annotation.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Priority(200)
public class WindowsNativeBuildInterceptor implements ConfigSourceInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNativeBuildInterceptor.class);

    private static final String PROPERTY_NAME = "quarkus.native.additional-build-args";
    private static final String SUBSYSTEM_ARGS = "-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS,-H:NativeLinkerOption=/ENTRY:mainCRTStartup";

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        ConfigValue originalValue = context.proceed(name);

        if (!PROPERTY_NAME.equals(name) || !isWindows()) {
            return originalValue;
        }

        List<String> mokaArgs = new ArrayList<>();

        mokaArgs.add(SUBSYSTEM_ARGS);

        Path resFile = Paths.get("icon.res").toAbsolutePath();
        if (Files.exists(resFile)) {
            String formattedResFile = resFile.toString().replace("\\", "\\\\");
            LOG.info("Setting application icon from '{}'", resFile);
            mokaArgs.add("-H:NativeLinkerOption=" + formattedResFile);
        } else {
            LOG.warn("'icon.res' not found at {}. Application icon will not be set.", resFile);
        }

        String mokaArgsString = String.join(",", mokaArgs);
        String finalValue;

        if (originalValue != null && originalValue.getValue() != null && !originalValue.getValue().isEmpty()) {
            if (originalValue.getValue().contains(mokaArgs.getFirst())) {
                return originalValue;
            }
            finalValue = originalValue.getValue() + "," + mokaArgsString;
        } else {
            finalValue = mokaArgsString;
        }

        if (originalValue == null) {
            return ConfigValue.builder()
                    .withName(name)
                    .withValue(finalValue)
                    .build();
        }

        return originalValue.withValue(finalValue);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}