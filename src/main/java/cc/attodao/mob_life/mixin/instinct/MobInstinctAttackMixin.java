package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.combat.MorphAttackDamage;
import cc.attodao.mob_life.gameplay.instinct.InstinctAiContext;
import cc.attodao.mob_life.gameplay.instinct.InstinctPreyManager;
import cc.attodao.mob_life.gameplay.instinct.InstinctProfiles;
import cc.attodao.mob_life.gameplay.instinct.MorphInstinct;
import cc.attodao.mob_life.morph.MorphType;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobInstinctAttackMixin {
  @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
  private void mobLife$attackAsInstinctPlayer(
      ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
    Mob proxy = (Mob) (Object) this;
    ServerPlayer owner = InstinctAiContext.owner(proxy);
    MorphType morph = InstinctAiContext.morph(proxy);
    if (owner == null || morph == null) {
      return;
    }
    if (target instanceof LivingEntity living
        && InstinctProfiles.isNaturalPrey(morph, living)
        && !InstinctProfiles.isEnabledPrey(living)) {
      proxy.setTarget(null);
      cir.setReturnValue(false);
      return;
    }

    float damage = (float) MorphAttackDamage.fromMorph(morph, proxy);
    Optional<InstinctPreyManager.FoodValue> food = Optional.empty();
    if (target instanceof LivingEntity living
        && !(living instanceof net.minecraft.world.entity.player.Player)
        && InstinctProfiles.isNaturalPrey(morph, living)) {
      food = InstinctPreyManager.get(living.getType());
      food.ifPresent(ignored -> InstinctAiContext.suppressDrops(living));
    }

    boolean hurt =
        damage > 0.0F && target.hurtServer(level, level.damageSources().mobAttack(owner), damage);
    if (hurt) {
      proxy.setLastHurtMob(target);
      owner.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
      if (target instanceof LivingEntity living && living.isDeadOrDying() && food.isPresent()) {
        MorphInstinct.onPreyKilled(owner, living, food.get());
      }
    }
    cir.setReturnValue(hurt);
  }
}
