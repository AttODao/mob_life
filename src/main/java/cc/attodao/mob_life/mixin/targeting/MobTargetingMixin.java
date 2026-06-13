package cc.attodao.mob_life.mixin.targeting;

import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobTargetingMixin {
	@Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
	private void mobLife$ignoreNaturalMorphs(
			LivingEntity target,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (
				(Object) this instanceof Monster
						&& target instanceof ServerPlayer player
						&& ServerMorphManager.hasMobForm()
						&& !MorphAwkwardness.canBeTargetedByHostiles(player)
		) {
			cir.setReturnValue(false);
		}
	}
}
