package fr.xamez.moka.config;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import jakarta.annotation.Priority;

@Priority(250)
public class LogConfigurationInterceptor implements ConfigSourceInterceptor {

    private static final String QUARKUS_LOG_ENABLED = "quarkus.log.file.enabled";
    private static final String QUARKUS_LOG_PATH = "quarkus.log.file.path";

    private static final String MOKA_LOG_ENABLED = "moka.app.enable-log-file";
    private static final String MOKA_LOG_PATH = "moka.app.log-file-path";
    private static final String MOKA_APP_NAME = "moka.app.name";

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {

        if (QUARKUS_LOG_ENABLED.equals(name)) {
            ConfigValue mokaEnabled = context.proceed(MOKA_LOG_ENABLED);
            if (mokaEnabled != null) {
                return mokaEnabled;
            }
        }

        if (QUARKUS_LOG_PATH.equals(name)) {
            ConfigValue userPath = context.proceed(MOKA_LOG_PATH);
            if (userPath != null && userPath.getValue() != null && !userPath.getValue().isEmpty()) {
                return ConfigValue.builder()
                        .withName(name)
                        .withValue(userPath.getValue())
                        .withConfigSourceName("Moka User Config")
                        .build();
            }

            ConfigValue existingQuarkusPath = context.proceed(name);
            if (existingQuarkusPath != null && existingQuarkusPath.getValue() != null && !existingQuarkusPath.getValue().equals("quarkus.log")) {
                return existingQuarkusPath;
            }

            ConfigValue appNameVal = context.proceed(MOKA_APP_NAME);
            String appName = (appNameVal != null) ? appNameVal.getValue() : "moka-app";

            String newPath = getDefaultLogPath(appName);
            return ConfigValue.builder()
                    .withName(name)
                    .withValue(newPath)
                    .withConfigSourceName("Moka Default Log Path")
                    .build();
        }

        return context.proceed(name);
    }

    private String getDefaultLogPath(String appName) {
        String os = System.getProperty("os.name").toLowerCase();
        String safeAppName = appName.replaceAll("[^a-zA-Z0-9.-]", "_");

        if (os.contains("win")) {
            return System.getenv("LOCALAPPDATA") + "\\" + safeAppName + "\\logs\\app.log";
        } else if (os.contains("mac")) {
            return System.getProperty("user.home") + "/Library/Logs/" + safeAppName + "/app.log";
        } else { // linux/unix
            return System.getProperty("user.home") + "/.cache/" + safeAppName + "/app.log";
        }
    }
}