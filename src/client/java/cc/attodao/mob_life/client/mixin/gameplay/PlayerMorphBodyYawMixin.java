package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientLocomotionController;
import cc.attodao.mob_life.client.state.ClientMorphBodyYawState;
import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMorphBodyYawMixin {
  @Inject(method = "tick", at = @At("TAIL"))
  private void mobLife$applySynchronizedBodyYaw(CallbackInfo ci) {
    if (ClientMorphState.morph() == null) {
      return;
    }

    Player player = (Player) (Object) this;
    if (player instanceof LocalPlayer) {
      if (!ClientInstinctState.active()) {
        player.yBodyRot = ClientLocomotionController.get().bodyYaw();
      }
    } else {
      ClientMorphBodyYawState.apply(player);
    }
  }
}
