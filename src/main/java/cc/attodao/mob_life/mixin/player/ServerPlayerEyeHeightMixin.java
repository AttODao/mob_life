package cc.attodao.mob_life.mixin.player;

import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ServerPlayerEyeHeightMixin {
	@Inject(method = "getEyeHeight()F", at = @At("HEAD"), cancellable = true)
	private void mobLife$useMorphEyeHeight(CallbackInfoReturnable<Float> cir) {
		if (!((Object) this instanceof ServerPlayer)) {
			return;
		}

		MorphType morph = ServerMorphManager.activeMorph();
		if (morph != null && !morph.isPlayer()) {
			cir.setReturnValue(morph.entityType().getDimensions().eyeHeight());
		}
	}
}
