package cc.attodao.mob_life.client.state;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public final class ClientMorphBodyYawState {
  private static final Map<Integer, Float> BODY_YAWS = new HashMap<>();

  private ClientMorphBodyYawState() {}

  public static void update(int entityId, float bodyYaw) {
    BODY_YAWS.put(entityId, bodyYaw);
  }

  public static void apply(Player player) {
    if (player instanceof LocalPlayer) {
      return;
    }
    Float target = BODY_YAWS.get(player.getId());
    if (target != null) {
      player.yBodyRot = target;
    }
  }

  public static void clear() {
    BODY_YAWS.clear();
  }
}
