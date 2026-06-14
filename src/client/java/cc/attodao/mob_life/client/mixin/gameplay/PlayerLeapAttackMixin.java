package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.gameplay.combat.MorphLeapAttack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerLeapAttackMixin {

  @Inject(method = "attack", at = @At("HEAD"))
  private void mobLife$predictLeapAttack(Entity target, CallbackInfo ci) {
    if ((Object) this instanceof LocalPlayer player) {
      MorphLeapAttack.tryLeap(player, target, ClientMorphState.morph());
    }
  }
}
