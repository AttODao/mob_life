package cc.attodao.mob_life.client.mixin.render;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {
	@Accessor("speedOld")
	float mobLife$getSpeedOld();

	@Accessor("speedOld")
	void mobLife$setSpeedOld(float value);

	@Accessor("speed")
	float mobLife$getSpeed();

	@Accessor("speed")
	void mobLife$setSpeed(float value);

	@Accessor("position")
	float mobLife$getPosition();

	@Accessor("position")
	void mobLife$setPosition(float value);
}
