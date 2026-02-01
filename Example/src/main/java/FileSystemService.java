import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

@ApplicationScoped
public class FileSystemService {

    @ConsumeEvent("app.directories")
    public JsonObject listDirectory(JsonObject payload) {
        String requestedPath = payload.getString("path");
        Path start = (requestedPath == null || requestedPath.isBlank())
                ? Paths.get(System.getProperty("user.home"))
                : Paths.get(requestedPath);

        Path absolutePath = start.toAbsolutePath().normalize();

        if (!Files.exists(absolutePath) || !Files.isDirectory(absolutePath)) {
            return new JsonObject()
                    .put("error", true)
                    .put("message", "File not found or is not a directory: " + absolutePath);
        }

        JsonArray items = new JsonArray();

        try (Stream<Path> stream = Files.list(absolutePath)) {
            stream
                    .sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p))
                            .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                    .forEach(path -> {
                        boolean isDir = Files.isDirectory(path);
                        items.add(new JsonObject()
                                .put("name", path.getFileName().toString())
                                .put("type", isDir ? "DIR" : "FILE")
                                .put("path", path.toString())
                                .put("icon", isDir ? "📁" : "📄")
                        );
                    });
        } catch (IOException e) {
            return new JsonObject().put("error", true).put("message", "Failed to read directory: " + e.getMessage());
        }

        return new JsonObject()
                .put("current", absolutePath.toString())
                .put("separator", java.io.File.separator)
                .put("items", items);
    }
}