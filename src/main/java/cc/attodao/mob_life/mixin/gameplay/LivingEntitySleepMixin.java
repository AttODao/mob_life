package cc.attodao.mob_life.mixin.gameplay;

import cc.attodao.mob_life.gameplay.sleep.MorphSleep;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySleepMixin {
	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;checkBedExists()Z"
			)
	)
	private boolean mobLife$allowSoftSleepingSurface(
			LivingEntity entity
	) {
		return entity instanceof Player player
				? MorphSleep.isValidSleepingSurface(player)
				: entity.getSleepingPos()
						.map(pos -> entity.level()
								.getBlockState(pos)
								.getBlock()
								instanceof net.minecraft.world.level.block.BedBlock)
						.orElse(false);
	}
}
