package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.gameplay.targeting.MorphRelations;
import cc.attodao.mob_life.morph.MorphType;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public final class InstinctAiContext {
  private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

  private InstinctAiContext() {}

  public static <T> T run(
      ServerPlayer owner,
      MorphType morph,
      Mob proxy,
      float explorationYaw,
      boolean hungryPriority,
      Supplier<T> action) {
    Context previous = CURRENT.get();
    Context context = new Context(owner, morph, proxy, explorationYaw, hungryPriority);
    CURRENT.set(context);
    try {
      return action.get();
    } finally {
      if (previous != null) {
        CURRENT.set(previous);
      } else {
        CURRENT.remove();
      }
    }
  }

  public static void run(
      ServerPlayer owner,
      MorphType morph,
      Mob proxy,
      float explorationYaw,
      boolean hungryPriority,
      Runnable action) {
    run(
        owner,
        morph,
        proxy,
        explorationYaw,
        hungryPriority,
        () -> {
          action.run();
          return null;
        });
  }

  public static ServerPlayer owner(Mob proxy) {
    Context context = CURRENT.get();
    return context != null && context.proxy == proxy ? context.owner : null;
  }

  public static MorphType morph(Mob proxy) {
    Context context = CURRENT.get();
    return context != null && context.proxy == proxy ? context.morph : null;
  }

  public static ServerPlayer owner() {
    Context context = CURRENT.get();
    return context != null ? context.owner : null;
  }

  public static MorphType morph() {
    Context context = CURRENT.get();
    return context != null ? context.morph : null;
  }

  public static boolean hasHungryPriority() {
    Context context = CURRENT.get();
    return context != null && context.hungryPriority;
  }

  public static float explorationYaw(Mob proxy) {
    Context context = CURRENT.get();
    return context != null && context.proxy == proxy ? context.explorationYaw : Float.NaN;
  }

  public static boolean ignoresPlayer(Player player) {
    Context context = CURRENT.get();
    return context != null
        && player != null
        && context.owner.level() == player.level()
        && MorphRelations.morphOf(player) != null;
  }

  public static void suppressDrops(Entity entity) {
    Context context = CURRENT.get();
    if (context != null) {
      context.suppressedDrops.add(entity);
    }
  }

  public static boolean suppressesDrops(Entity entity) {
    Context context = CURRENT.get();
    return context != null && context.suppressedDrops.contains(entity);
  }

  private static final class Context {
    final ServerPlayer owner;
    final MorphType morph;
    final Mob proxy;
    final float explorationYaw;
    final boolean hungryPriority;
    final Set<Entity> suppressedDrops = new HashSet<>();

    Context(
        ServerPlayer owner,
        MorphType morph,
        Mob proxy,
        float explorationYaw,
        boolean hungryPriority) {
      this.owner = owner;
      this.morph = morph;
      this.proxy = proxy;
      this.explorationYaw = explorationYaw;
      this.hungryPriority = hungryPriority;
    }
  }
}
