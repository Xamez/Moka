package fr.xamez.moka.tool;

import fr.xamez.moka.MokaApp;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class NotificationService {

    @Inject
    EventBus eventBus;

    @Inject
    MokaApp mokaApp;

    void onStart(@Observes StartupEvent ev) {
        eventBus.consumer("moka.notification", message -> {
            JsonObject body = (JsonObject) message.body();
            String title = body.getString("title", "Moka");
            String content = body.getString("message", "");
            String stringType = body.getString("type", "INFORMATION");
            NotificationType type;
            try {
                type = NotificationType.valueOf(stringType.toUpperCase());
            } catch (IllegalArgumentException e) {
                type = NotificationType.INFORMATION;
            }

            mokaApp.showNotification(title, content, type);
            message.reply(new JsonObject().put("status", "ok"));
        });
    }
}