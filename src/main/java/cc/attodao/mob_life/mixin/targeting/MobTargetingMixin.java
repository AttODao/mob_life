package cc.attodao.mob_life.mixin.targeting;

import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobTargetingMixin {
  @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
  private void mobLife$ignoreNaturalMorphs(
      LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
    if ((Object) this instanceof Monster
        && target instanceof ServerPlayer player
        && ServerMorphManager.hasMobForm()) {
      Mob mob = (Mob) (Object) this;
      if (InstinctManager.isEnabled(player)) {
        if (mob.getTarget() != player) {
          cir.setReturnValue(false);
        }
        return;
      }
      if (MorphConfigManager.get(ServerMorphManager.activeMorph())
          .combat()
          .avoidedBy()
          .contains(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString())) {
        cir.setReturnValue(false);
        return;
      }
      double detectionRange =
          mob.getAttributeValue(Attributes.FOLLOW_RANGE)
              * MorphAwkwardness.hostileDetectionScale(player)
              * MorphConfigManager.get(ServerMorphManager.activeMorph())
                  .combat()
                  .hostileDetectionMultiplier();
      if (detectionRange <= 0.0 || mob.distanceToSqr(player) > detectionRange * detectionRange) {
        cir.setReturnValue(false);
      }
    }
  }
}
