package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Confirms a successful container-opening interaction before its client screen packet arrives. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerInstinctGuiMixin {

  @Inject(method = "openMenu", at = @At("RETURN"))
  private void mobLife$preserveIdleForOpenedContainer(
      MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
    if (cir.getReturnValue().isPresent()) {
      InstinctManager.openedServerContainer((ServerPlayer) (Object) this);
    }
  }
}
