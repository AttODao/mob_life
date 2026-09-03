package cc.attodao.mob_life.mixin.instinct;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityDamageAccessor {
  @Accessor("lastDamageSource")
  void mobLife$setLastDamageSource(@Nullable DamageSource source);

  @Accessor("lastDamageStamp")
  void mobLife$setLastDamageStamp(long stamp);
}
