package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerInstinctMovementMixin {
  @Inject(method = "travel", at = @At("HEAD"))
  private void mobLife$applyInstinctMovement(Vec3 input, CallbackInfo ci) {
    if ((Object) this instanceof LocalPlayer player && ClientInstinctState.enabled()) {
      ClientInstinctState.applyNativeMovement(player);
    }
  }
}
