package cc.attodao.mob_life.world;

import cc.attodao.mob_life.config.MobLifeConfig;
import cc.attodao.mob_life.morph.MorphDefinition;
import java.util.Optional;

public final class PendingWorldSelection {

  private static MorphDefinition pending;
  private static boolean worldCreationStarted;

  private PendingWorldSelection() {}

  public static synchronized void setForNextWorld(MorphDefinition definition) {
    pending = definition;
    worldCreationStarted = false;
  }

  public static synchronized Optional<MorphDefinition> peek() {
    return Optional.ofNullable(pending);
  }

  public static synchronized MorphDefinition peekOrDefault() {
    return pending != null ? pending : MorphDefinition.of(MobLifeConfig.defaultMorph());
  }

  public static synchronized Optional<MorphDefinition> consume() {
    MorphDefinition result = pending;
    pending = null;
    worldCreationStarted = false;
    return Optional.ofNullable(result);
  }

  public static synchronized MorphDefinition consumeOrDefault() {
    MorphDefinition result =
        pending != null ? pending : MorphDefinition.of(MobLifeConfig.defaultMorph());
    pending = null;
    worldCreationStarted = false;
    return result;
  }

  public static synchronized void markWorldCreationStarted() {
    if (pending != null) {
      worldCreationStarted = true;
    }
  }

  public static synchronized void clearIfWorldCreationNotStarted() {
    if (!worldCreationStarted) {
      pending = null;
    }
  }

  public static synchronized void clear() {
    pending = null;
    worldCreationStarted = false;
  }
}
