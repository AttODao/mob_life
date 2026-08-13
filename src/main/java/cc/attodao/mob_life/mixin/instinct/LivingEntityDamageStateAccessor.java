package cc.attodao.mob_life.mixin.instinct;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the recent-damage hooks consulted by vanilla panic and target goals. */
@Mixin(LivingEntity.class)
public interface LivingEntityDamageStateAccessor {
  @Accessor("lastDamageSource")
  void mobLife$setLastDamageSource(DamageSource source);

  @Accessor("lastDamageStamp")
  void mobLife$setLastDamageStamp(long gameTime);

  @Invoker("resolveMobResponsibleForDamage")
  void mobLife$invokeResolveMobResponsibleForDamage(DamageSource source);
}
