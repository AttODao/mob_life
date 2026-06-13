package cc.attodao.mob_life.mixin.player;

import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ServerPlayerDimensionsMixin {
	@Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
	private void mobLife$useMorphDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		Entity self = (Entity) (Object) this;
		if (!(self instanceof ServerPlayer)) {
			return;
		}

		MorphType morph = ServerMorphManager.activeMorph();
		if (morph != null && !morph.isPlayer()) {
			cir.setReturnValue(morph.entityType().getDimensions());
		}
	}
}
