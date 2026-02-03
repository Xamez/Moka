package fr.xamez.moka;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Path;

@ApplicationScoped
public class BridgeRouter {

    @Inject
    MokaAppConfig config;

    @Inject
    Vertx vertx;

    @Inject
    Router router;

    public void setup() {
        setupBridge();
        setupStaticServing();
    }

    private void setupBridge() {
        router.route("/_moka/bridge").handler(ctx -> {
            ctx.response()
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .putHeader("Access-Control-Allow-Headers", "Content-Type");
            ctx.next();
        });

        router.post("/_moka/bridge").handler(BodyHandler.create()).handler(ctx -> {
            JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                ctx.response().setStatusCode(400).end("Missing body");
                return;
            }

            vertx.eventBus().request(body.getString("address"), body.getValue("payload", new JsonObject()), reply -> {
                if (reply.succeeded()) {
                    Object res = reply.result().body();
                    ctx.json(res != null ? res : new JsonObject());
                } else {
                    ctx.response().setStatusCode(500).end(new JsonObject().put("error", reply.cause().getMessage()).encode());
                }
            });
        });
    }

    private void setupStaticServing() {
        if (config.devUrl().isPresent()) return;

        StaticHandler staticHandler;
        if (config.staticPath().isEmpty()) {
            staticHandler = StaticHandler.create();
        } else {
            String absPath = Path.of(config.staticPath().get()).toAbsolutePath().toString();
            staticHandler = StaticHandler.create(FileSystemAccess.ROOT, absPath);
        }
        staticHandler.setCachingEnabled(config.enableBrowserCache());
        router.route("/*").handler(staticHandler);

        router.route().last().handler(ctx -> {
            boolean isNotRoot = !"/".equals(ctx.normalizedPath());
            String header = ctx.request().getHeader("Accept");
            if (header == null) header = "";
            boolean isHtmlRequest = header.contains("text/html");
            boolean responseNotEnded = !ctx.response().ended();

            if (responseNotEnded && isHtmlRequest && isNotRoot) {
                ctx.reroute("/");
            } else {
                ctx.next();
            }
        });
    }
}