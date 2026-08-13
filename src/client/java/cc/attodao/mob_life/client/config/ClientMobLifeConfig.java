package cc.attodao.mob_life.client.config;

import cc.attodao.mob_life.MobLife;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Local-only rendering and HUD preferences. */
public final class ClientMobLifeConfig {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path PATH =
      FabricLoader.getInstance().getConfigDir().resolve("mob_life-client.json");
  private static final Path LEGACY_PATH =
      FabricLoader.getInstance().getConfigDir().resolve("mob_life.json");
  private static final Config DEFAULT_CONFIG = new Config(true, false);

  private static volatile Config config = DEFAULT_CONFIG;

  private ClientMobLifeConfig() {}

  public static synchronized void load() {
    if (Files.exists(PATH)) {
      config = loadFrom(PATH);
      return;
    }

    config = Files.exists(LEGACY_PATH) ? loadFrom(LEGACY_PATH) : DEFAULT_CONFIG;
    save();
  }

  public static boolean shaderEnabled() {
    return config.shaderEnabled();
  }

  public static boolean defaultShaderEnabled() {
    return DEFAULT_CONFIG.shaderEnabled();
  }

  public static synchronized void setShaderEnabled(boolean shaderEnabled) {
    config = new Config(shaderEnabled, config.showAwkwardnessDebug());
  }

  public static boolean showAwkwardnessDebug() {
    return config.showAwkwardnessDebug();
  }

  public static boolean defaultShowAwkwardnessDebug() {
    return DEFAULT_CONFIG.showAwkwardnessDebug();
  }

  public static synchronized void setShowAwkwardnessDebug(boolean showAwkwardnessDebug) {
    config = new Config(config.shaderEnabled(), showAwkwardnessDebug);
  }

  public static synchronized void save() {
    JsonObject root = new JsonObject();
    JsonObject rendering = new JsonObject();
    rendering.addProperty("shader_enabled", config.shaderEnabled());
    root.add("rendering", rendering);
    JsonObject debug = new JsonObject();
    debug.addProperty("show_awkwardness_debug", config.showAwkwardnessDebug());
    root.add("debug", debug);

    try {
      Files.createDirectories(PATH.getParent());
      try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
        GSON.toJson(root, writer);
      }
    } catch (IOException exception) {
      MobLife.LOGGER.warn("Failed to save client config {}", PATH, exception);
    }
  }

  private static Config loadFrom(Path path) {
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
      JsonObject rendering = object(root, "rendering");
      JsonObject debug = object(root, "debug");
      return new Config(
          bool(rendering, "shader_enabled", DEFAULT_CONFIG.shaderEnabled()),
          bool(debug, "show_awkwardness_debug", DEFAULT_CONFIG.showAwkwardnessDebug()));
    } catch (IOException | RuntimeException exception) {
      MobLife.LOGGER.warn("Failed to load client config {}", path, exception);
      return DEFAULT_CONFIG;
    }
  }

  private static JsonObject object(JsonObject root, String key) {
    JsonElement element = root.get(key);
    return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
  }

  private static boolean bool(JsonObject object, String key, boolean fallback) {
    JsonElement element = object.get(key);
    try {
      return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
    } catch (RuntimeException exception) {
      return fallback;
    }
  }

  private record Config(boolean shaderEnabled, boolean showAwkwardnessDebug) {}
}
