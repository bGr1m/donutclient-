package dev.donut.client.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FreecamSettingsTest {
    @TempDir Path directory;

    @Test
    void freecamNeverReenablesFromSavedOrHandEditedConfiguration() throws IOException {
        Path path = directory.resolve("donut.json");
        ConfigStore store = new ConfigStore(path);
        ClientConfig config = new ClientConfig();
        config.freecam.enabled = true;
        config.freecam.favorite = true;
        config.freecam.speed = 2.25;
        config.freecam.boostMultiplier = 4.5;

        store.save(config);

        var json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertFalse(json.getAsJsonObject("freecam").has("enabled"));
        assertTrue(config.freecam.enabled, "Saving must not switch off the live camera");
        ClientConfig loaded = store.load();
        assertFalse(loaded.freecam.enabled);
        assertTrue(loaded.freecam.favorite);
        assertEquals(2.25, loaded.freecam.speed);
        assertEquals(4.5, loaded.freecam.boostMultiplier);

        Files.writeString(path, """
                {"freecam":{"enabled":true,"favorite":true,"speed":1.5,"boostMultiplier":2}}
                """);
        loaded = store.load();
        assertFalse(loaded.freecam.enabled, "A JSON enabled field must not activate a camera on join");
        assertTrue(loaded.freecam.favorite);
        assertEquals(1.5, loaded.freecam.speed);
        assertEquals(2, loaded.freecam.boostMultiplier);
    }

    @Test
    void olderAndNullConfigurationsRestoreDisabledDefaults() throws IOException {
        Path path = directory.resolve("donut.json");
        ConfigStore store = new ConfigStore(path);
        for (String json : new String[] {"{}", "{\"freecam\":null}"}) {
            Files.writeString(path, json);
            ClientConfig loaded = store.load();
            assertNotNull(loaded.freecam);
            assertFalse(loaded.freecam.enabled);
            assertFalse(loaded.freecam.favorite);
            assertEquals(1, loaded.freecam.speed);
            assertEquals(3, loaded.freecam.boostMultiplier);
        }
    }

    @Test
    void finiteSettingsClampToSupportedMovementBounds() {
        FreecamSettings low = new FreecamSettings();
        low.speed = -10;
        low.boostMultiplier = -10;
        FreecamSettings high = new FreecamSettings();
        high.speed = 100;
        high.boostMultiplier = 100;

        low.sanitize();
        high.sanitize();

        assertEquals(0.1, low.speed);
        assertEquals(1, low.boostMultiplier);
        assertEquals(5, high.speed);
        assertEquals(10, high.boostMultiplier);
    }

    @Test
    void nonFiniteValuesAreRepairedBeforeSerialization() throws IOException {
        Path path = directory.resolve("donut.json");
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            ClientConfig config = new ClientConfig();
            config.freecam.speed = invalid;
            config.freecam.boostMultiplier = invalid;
            config.freecam.favorite = true;

            assertDoesNotThrow(() -> new ConfigStore(path).save(config));
            ClientConfig loaded = new ConfigStore(path).load();

            assertEquals(1, loaded.freecam.speed, "speed: " + invalid);
            assertEquals(3, loaded.freecam.boostMultiplier, "boost multiplier: " + invalid);
            assertTrue(loaded.freecam.favorite);
            assertFalse(loaded.freecam.enabled);
            String json = Files.readString(path);
            assertFalse(json.contains("NaN"));
            assertFalse(json.contains("Infinity"));
        }
    }
}
