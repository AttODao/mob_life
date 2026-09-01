package cc.attodao.mob_life.mixin.awkwardness;

import cc.attodao.mob_life.gameplay.awkwardness.AwkwardnessHolder;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerAwkwardnessMixin implements AwkwardnessHolder {
  @Unique private float mobLife$awkwardness;
  @Unique private float mobLife$healthBeforeDamage;
  @Unique private float mobLife$absorptionBeforeDamage;
  @Unique private boolean mobLife$capturingDamage;

  @Override
  public float mobLife$getAwkwardness() {
    return mobLife$awkwardness;
  }

  @Override
  public void mobLife$setAwkwardness(float awkwardness) {
    mobLife$awkwardness = awkwardness;
  }

  @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$readAwkwardness(ValueInput input, CallbackInfo ci) {
    mobLife$awkwardness = input.getFloatOr("MobLifeAwkwardness", MorphAwkwardness.MINIMUM);
  }

  @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$writeAwkwardness(ValueOutput output, CallbackInfo ci) {
    output.putFloat("MobLifeAwkwardness", mobLife$awkwardness);
  }

  @Inject(method = "hurtServer", at = @At("HEAD"))
  private void mobLife$captureDamageBeforeMitigation(
      net.minecraft.server.level.ServerLevel level,
      DamageSource damageSource,
      float damage,
      CallbackInfoReturnable<Boolean> cir) {
    if ((Object) this instanceof ServerPlayer player && ServerMorphManager.hasMobForm()) {
      mobLife$healthBeforeDamage = player.getHealth();
      mobLife$absorptionBeforeDamage = player.getAbsorptionAmount();
      mobLife$capturingDamage = true;
    }
  }

  @Inject(method = "hurtServer", at = @At("RETURN"))
  private void mobLife$increaseAwkwardnessAfterDamage(
      net.minecraft.server.level.ServerLevel level,
      DamageSource damageSource,
      float damage,
      CallbackInfoReturnable<Boolean> cir) {
    if ((Object) this instanceof ServerPlayer player && mobLife$capturingDamage) {
      mobLife$capturingDamage = false;
      if (!ServerMorphManager.hasMobForm()) {
        return;
      }
      float finalDamage =
          Math.max(0.0F, mobLife$healthBeforeDamage - player.getHealth())
              + Math.max(0.0F, mobLife$absorptionBeforeDamage - player.getAbsorptionAmount());
      if (cir.getReturnValueZ() && finalDamage > 0.0F) {
        ServerMorphManager.increaseAwkwardnessFromDamage(player, finalDamage);
      }
    }
  }

  @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
  private float mobLife$scaleExhaustion(float amount) {
    return (Object) this instanceof ServerPlayer player && ServerMorphManager.hasMobForm()
        ? amount * MorphAwkwardness.exhaustionMultiplier(player)
        : amount;
  }
}
