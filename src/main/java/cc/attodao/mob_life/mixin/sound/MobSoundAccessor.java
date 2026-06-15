package cc.attodao.mob_life.mixin.sound;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mob.class)
public interface MobSoundAccessor {
  @Invoker("getAmbientSound")
  SoundEvent mobLife$getAmbientSound();
}
