package cc.attodao.mob_life.client.mixin.player;

import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ClientPlayerDimensionsMixin {
  @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
  private void mobLife$useMorphDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
    Entity self = (Entity) (Object) this;
    if (!(self instanceof Player)) {
      return;
    }

    EntityDimensions dimensions = ClientMorphState.dimensions();
    if (dimensions != null) {
      cir.setReturnValue(dimensions);
    }
  }
}
