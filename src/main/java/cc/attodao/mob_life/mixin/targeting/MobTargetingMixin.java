package cc.attodao.mob_life.mixin.targeting;

import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.targeting.MorphPredation;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobTargetingMixin {
  @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
  private void mobLife$ignoreNaturalMorphs(
      LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
    if (!(target instanceof ServerPlayer player) || !ServerMorphManager.hasMobForm()) {
      return;
    }

    Mob mob = (Mob) (Object) this;
    MorphType morph = ServerMorphManager.activeMorph();
    boolean predator = MorphPredation.isPredatorForPlayer(mob, player, morph);
    if (MorphConfigManager.get(morph)
        .combat()
        .avoidedBy()
        .contains(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString())) {
      cir.setReturnValue(false);
      return;
    }
    if ((!predator && mob instanceof Enemy)
        && !MorphPredation.isWithinMorphDetectionRange(mob, player, morph)) {
      cir.setReturnValue(false);
    }
  }
}
