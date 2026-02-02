package fr.xamez.moka;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Display;
import org.jboss.logging.Logger;

public class MokaBridge extends BrowserFunction {
    private static final Logger LOG = Logger.getLogger(MokaBridge.class);
    private final EventBus eventBus;
    private final Browser browser;

    public MokaBridge(Browser browser, EventBus eventBus) {
        super(browser, "__moka_invoke");
        this.browser = browser;
        this.eventBus = eventBus;
    }

    @Override
    public Object function(Object[] args) {
        if (args == null || args.length < 3) return null;

        String address = (String) args[0];
        String payload = (String) args[1];
        String requestId = (String) args[2];

        Display display = browser.getDisplay();

        try {
            eventBus.request(address, new JsonObject(payload), reply -> {
                display.asyncExec(() -> {
                    try {
                        if (browser.isDisposed()) return;

                        String responseData;
                        boolean success = reply.succeeded();

                        if (success) {
                            responseData = Json.encode(reply.result().body());
                        } else {
                            responseData = new JsonObject().put("error", reply.cause().getMessage()).encode();
                        }

                        resolvePromise(requestId, success, responseData);
                    } catch (Exception e) {
                        LOG.error("Error in asyncExec processing reply", e);
                    }
                });
            });
        } catch (Exception e) {
            LOG.error("Bridge dispatch failed", e);
            resolvePromise(requestId, false, "{\"error\": \"Invalid JSON payload\"}");
        }
        return null;
    }

    private void resolvePromise(String requestId, boolean success, String jsonData) {
        if (browser.isDisposed()) return;

        String status = success ? "resolve" : "reject";
        String data = jsonData != null ? jsonData : "{}";

        String script = """
                (function() {
                  var cb = window.__moka_callbacks['%1$s'];
                  if (cb) {
                    cb.%2$s(%3$s);
                    delete window.__moka_callbacks['%1$s'];
                  } else {
                    console.error('MokaBridge: Callback not found for %1$s');
                  }
                })();
                """.formatted(requestId, status, data);

        try {
            boolean result = browser.execute(script);
            if (!result) {
                LOG.errorf("Browser execution failed for req: %s", requestId);
            }
        } catch (Exception e) {
            LOG.error("Failed to execute javascript", e);
        }
    }
}