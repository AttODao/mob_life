package cc.attodao.mob_life.client.config;

import cc.attodao.mob_life.MobLife;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class MobLifeClientConfig {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path PATH =
      FabricLoader.getInstance().getConfigDir().resolve("mob_life-client.json");

  private static boolean showAwkwardnessDebug;

  private MobLifeClientConfig() {}

  public static void load() {
    if (!Files.exists(PATH)) {
      save();
      return;
    }

    try (Reader reader = Files.newBufferedReader(PATH)) {
      JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
      if (json.has("showAwkwardnessDebug")) {
        showAwkwardnessDebug = json.get("showAwkwardnessDebug").getAsBoolean();
      }
    } catch (IOException | RuntimeException exception) {
      MobLife.LOGGER.warn("Failed to load client config {}", PATH, exception);
    }
  }

  public static boolean showAwkwardnessDebug() {
    return showAwkwardnessDebug;
  }

  public static void setShowAwkwardnessDebug(boolean show) {
    showAwkwardnessDebug = show;
    save();
  }

  private static void save() {
    JsonObject json = new JsonObject();
    json.addProperty("showAwkwardnessDebug", showAwkwardnessDebug);

    try {
      Files.createDirectories(PATH.getParent());
      try (Writer writer = Files.newBufferedWriter(PATH)) {
        GSON.toJson(json, writer);
      }
    } catch (IOException exception) {
      MobLife.LOGGER.warn("Failed to save client config {}", PATH, exception);
    }
  }
}
