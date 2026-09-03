package cc.attodao.mob_life.mixin.combat;

import cc.attodao.mob_life.gameplay.combat.MorphLeapAttack;
import cc.attodao.mob_life.gameplay.instinct.InstinctAiContext;
import cc.attodao.mob_life.gameplay.instinct.MorphInstinct;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMorphCombatMixin {
  @Inject(method = "killedEntity", at = @At("HEAD"), cancellable = true)
  private void mobLife$skipInstinctPlayerKillStat(
      ServerLevel level,
      LivingEntity entity,
      DamageSource source,
      CallbackInfoReturnable<Boolean> cir) {
    if (InstinctAiContext.owner() == (Object) this) {
      cir.setReturnValue(true);
    }
  }

  @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
  private void mobLife$applyMorphAttackRules(Entity target, CallbackInfo ci) {
    if ((Object) this instanceof ServerPlayer player && MorphInstinct.blocksActions(player)) {
      ci.cancel();
      return;
    }
    if ((Object) this instanceof ServerPlayer player && ServerMorphManager.hasMobForm()) {
      MorphLeapAttack.tryLeap(player, target, ServerMorphManager.activeMorph());
      if (!ServerMorphManager.activeMorphHasAttackAi()) {
        ServerMorphManager.adjustAwkwardness(player, 12.0F);
      }
    }
  }

  @ModifyArg(
      method = "attack",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
      index = 1)
  private float mobLife$applyMorphAttackDamage(float damage) {
    if ((Object) this instanceof ServerPlayer player && ServerMorphManager.hasMobForm()) {
      return player.getAttributeValue(
                  net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
              > 0.0
          ? damage
          : 0.0F;
    }
    return damage;
  }
}
