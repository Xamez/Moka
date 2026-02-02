package fr.xamez.moka;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "moka.app")
public interface MokaAppConfig {

    @WithDefault("Moka App")
    String name();

    @WithDefault("Moka Application")
    String title();

    @WithDefault("1024")
    int width();

    @WithDefault("768")
    int height();

    @WithDefault("false")
    boolean fullscreen();

    @WithDefault("true")
    boolean setActive();

    @WithDefault("/moka.ico")
    String iconUrl();

    @WithDefault("true")
    boolean center();

    @WithDefault("true")
    boolean resizable();

    @WithDefault("true")
    boolean decorated();

    @WithDefault("false")
    boolean maximized();

    @WithDefault("true")
    boolean enableLogFile();

    Optional<String> logFilePath();

    @WithDefault("true")
    boolean enableBrowserCache();

    Optional<String> devUrl();

    Optional<String> staticPath();

}