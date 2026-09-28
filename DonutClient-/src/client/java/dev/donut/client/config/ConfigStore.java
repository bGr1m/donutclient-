package dev.donut.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigStore {
    private static final Logger LOGGER = LoggerFactory.getLogger("Donut/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;

    public ConfigStore(Path path) { this.path = path; }

    public ClientConfig load() {
        if (!Files.exists(path)) return new ClientConfig();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ClientConfig result = GSON.fromJson(reader, ClientConfig.class);
            if (result == null) throw new IllegalArgumentException("Empty configuration");
            result.sanitize();
            return result;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot read Donut config; using defaults", e);
            try {
                Files.copy(path, path.resolveSibling(path.getFileName() + ".invalid-" + System.currentTimeMillis()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException backupError) {
                LOGGER.warn("Could not back up invalid configuration", backupError);
            }
            return new ClientConfig();
        }
    }

    public void save(ClientConfig config) {
        config.sanitize();
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(config), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Could not save Donut configuration to {}", path, e);
        }
    }
}
