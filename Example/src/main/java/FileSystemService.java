import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FileSystemService {

    @Inject
    FileUtils fileUtils;

    @ConsumeEvent("app.selectFile")
    @Blocking
    public JsonObject selectFile(JsonObject payload) {
        String selectedPath = fileUtils.openFile(payload.getString("path"));
        return new JsonObject().put("path", selectedPath);
    }
}