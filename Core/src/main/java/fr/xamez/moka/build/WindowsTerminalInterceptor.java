package fr.xamez.moka.build;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import jakarta.annotation.Priority;

@Priority(200)
public class WindowsTerminalInterceptor implements ConfigSourceInterceptor {

    private static final String PROPERTY_NAME = "quarkus.native.additional-build-args";
    private static final String WINDOWS_ARGS = "-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS,-H:NativeLinkerOption=/ENTRY:mainCRTStartup";

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        ConfigValue originalValue = context.proceed(name);

        if (PROPERTY_NAME.equals(name) && isWindows()) {
            String newValue = WINDOWS_ARGS;

            if (originalValue != null && originalValue.getValue() != null && !originalValue.getValue().isEmpty()) {
                newValue = originalValue.getValue() + "," + WINDOWS_ARGS;
            }

            if (originalValue == null) {
                return ConfigValue.builder()
                        .withName(name)
                        .withValue(newValue)
                        .build();
            }

            return originalValue.withValue(newValue);
        }

        return originalValue;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

}