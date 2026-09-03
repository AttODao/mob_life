package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.targeting.MorphRelations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PlayerMorphLeashRideMixin {
  @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At("RETURN"))
  private void mobLife$dropMorphLeashWhenBoarding(
      Entity vehicle, boolean force, boolean checkVehicle, CallbackInfoReturnable<Boolean> cir) {
    if (cir.getReturnValueZ()
        && (Object) this instanceof ServerPlayer player
        && MorphRelations.morphOf(player) != null
        && ((Leashable) player).isLeashed()) {
      ((Leashable) player).dropLeash();
    }
  }
}
