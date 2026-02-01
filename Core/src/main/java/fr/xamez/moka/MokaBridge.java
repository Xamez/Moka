package fr.xamez.moka;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.jboss.logging.Logger;

public class MokaBridge extends BrowserFunction {
    private static final Logger LOG = Logger.getLogger(MokaBridge.class);
    private final EventBus eventBus;

    public MokaBridge(Browser browser, EventBus eventBus) {
        super(browser, "__moka_invoke");
        this.eventBus = eventBus;
    }

    @Override
    public Object function(Object[] args) {
        if (args == null || args.length < 3) return null;

        String address = (String) args[0];
        String payload = (String) args[1];
        String requestId = (String) args[2];

        try {
            eventBus.request(address, new JsonObject(payload), reply -> {
                getBrowser().getDisplay().asyncExec(() -> {
                    if (getBrowser().isDisposed()) return;

                    String responseData;
                    boolean success = reply.succeeded();

                    if (success) {
                        responseData = Json.encode(reply.result().body());
                    } else {
                        responseData = new JsonObject().put("error", reply.cause().getMessage()).encode();
                    }

                    resolvePromise(requestId, success, responseData);
                });
            });
        } catch (Exception e) {
            LOG.error("Bridge dispatch failed", e);
            resolvePromise(requestId, false, "{\"error\": \"Invalid JSON payload\"}");
        }
        return null;
    }

    private void resolvePromise(String requestId, boolean success, String jsonData) {
        getBrowser().execute(String.format(
                "const cb = window.__moka_callbacks['%s']; if (cb) { cb.%s(%s); delete window.__moka_callbacks['%s']; }",
                requestId,
                success ? "resolve" : "reject",
                jsonData != null ? jsonData : "{}",
                requestId
        ));
    }
}