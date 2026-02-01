package fr.xamez.moka;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "moka.app")
public interface MokaAppConfig {

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

    Optional<String> devUrl();

    Optional<String> staticPath();
}