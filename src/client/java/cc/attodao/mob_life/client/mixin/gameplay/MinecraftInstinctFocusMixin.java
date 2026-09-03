package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftInstinctFocusMixin {
  @Shadow @Nullable public HitResult hitResult;

  @Shadow @Nullable public Entity crosshairPickEntity;

  @Inject(method = "pick", at = @At("TAIL"))
  private void mobLife$clearInstinctFocus(float partialTick, CallbackInfo ci) {
    if (!ClientInstinctState.active()) {
      return;
    }
    crosshairPickEntity = null;
    Vec3 location = hitResult != null ? hitResult.getLocation() : Vec3.ZERO;
    hitResult = BlockHitResult.miss(location, Direction.DOWN, BlockPos.containing(location));
  }
}
