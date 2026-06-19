package cc.attodao.mob_life.world;

import cc.attodao.mob_life.config.MobLifeConfig;
import cc.attodao.mob_life.morph.MorphDefinition;
import java.util.Optional;

public final class PendingWorldSelection {

  public record PendingSelection(MorphDefinition definition, boolean preRandomized) {}

  private static PendingSelection pending;
  private static boolean worldCreationStarted;

  private PendingWorldSelection() {}

  public static synchronized void setForNextWorld(MorphDefinition definition) {
    setForNextWorld(definition, false);
  }

  public static synchronized void setForNextWorld(
      MorphDefinition definition, boolean preRandomized) {
    pending = new PendingSelection(definition, preRandomized);
    worldCreationStarted = false;
  }

  public static synchronized Optional<MorphDefinition> peek() {
    return Optional.ofNullable(pending).map(PendingSelection::definition);
  }

  public static synchronized Optional<PendingSelection> peekSelection() {
    return Optional.ofNullable(pending);
  }

  public static synchronized MorphDefinition peekOrDefault() {
    return pending != null
        ? pending.definition()
        : MorphDefinition.of(MobLifeConfig.defaultMorph());
  }

  public static synchronized Optional<MorphDefinition> consume() {
    return consumeSelection().map(PendingSelection::definition);
  }

  public static synchronized Optional<PendingSelection> consumeSelection() {
    PendingSelection result = pending;
    pending = null;
    worldCreationStarted = false;
    return Optional.ofNullable(result);
  }

  public static synchronized MorphDefinition consumeOrDefault() {
    MorphDefinition result =
        pending != null ? pending.definition() : MorphDefinition.of(MobLifeConfig.defaultMorph());
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
