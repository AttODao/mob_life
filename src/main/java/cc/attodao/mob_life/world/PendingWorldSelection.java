package cc.attodao.mob_life.world;

import cc.attodao.mob_life.config.ServerMobLifeConfig;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphType;
import java.util.Optional;

public final class PendingWorldSelection {

  public record PendingSelection(MorphType morph, MorphVariantRequest variantRequest) {
    public PendingSelection {
      variantRequest = variantRequest != null ? variantRequest : MorphVariantRequest.empty();
    }

    public MorphDefinition provisionalDefinition() {
      return variantRequest.toPendingDefinition(morph);
    }
  }

  private static PendingSelection pending;
  private static boolean worldCreationStarted;

  private PendingWorldSelection() {}

  public static synchronized void setForNextWorld(MorphDefinition definition) {
    setForNextWorld(
        definition.type(), MorphVariantRequest.fromNbt(definition.type(), definition.nbt()));
  }

  public static synchronized void setForNextWorld(
      MorphType morph, MorphVariantRequest variantRequest) {
    pending = new PendingSelection(morph, variantRequest);
    worldCreationStarted = false;
  }

  public static synchronized Optional<MorphDefinition> peek() {
    return Optional.ofNullable(pending).map(PendingSelection::provisionalDefinition);
  }

  public static synchronized Optional<PendingSelection> peekSelection() {
    return Optional.ofNullable(pending);
  }

  public static synchronized MorphDefinition peekOrDefault() {
    return pending != null
        ? pending.provisionalDefinition()
        : MorphDefinition.of(ServerMobLifeConfig.defaultMorph());
  }

  public static synchronized Optional<MorphDefinition> consume() {
    return consumeSelection().map(PendingSelection::provisionalDefinition);
  }

  public static synchronized Optional<PendingSelection> consumeSelection() {
    PendingSelection result = pending;
    pending = null;
    worldCreationStarted = false;
    return Optional.ofNullable(result);
  }

  public static synchronized MorphDefinition consumeOrDefault() {
    MorphDefinition result =
        pending != null
            ? pending.provisionalDefinition()
            : MorphDefinition.of(ServerMobLifeConfig.defaultMorph());
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
