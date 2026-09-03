package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class InstinctState {
  private InstinctState() {}

  public static InstinctStateData get(Player player) {
    return ((InstinctStateHolder) player).mobLife$getInstinctState();
  }

  public static boolean isActive(Player player) {
    return get(player).active();
  }

  public static boolean isSupported(MorphType morph) {
    if (morph == null || morph.isPlayer()) {
      return false;
    }
    MorphConfig.Instinct instinct = MorphConfigManager.get(morph).instinct();
    return instinct.supported()
        && instinct.profile().equals("mob_life:" + morph.id())
        && InstinctProfiles.supportsProfile(instinct.profile());
  }

  public static boolean enter(ServerPlayer player, MorphType morph) {
    if (!isSupported(morph) || player.isSpectator()) {
      return false;
    }
    get(player).enter();
    return true;
  }

  public static void exit(ServerPlayer player) {
    get(player).exit();
  }
}
