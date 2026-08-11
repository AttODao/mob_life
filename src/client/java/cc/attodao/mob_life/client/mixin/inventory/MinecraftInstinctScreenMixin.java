package cc.attodao.mob_life.client.mixin.inventory;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftInstinctScreenMixin {
  @Inject(method = "handleKeybinds", at = @At("HEAD"))
  private void mobLife$discardInstinctActionKeys(CallbackInfo ci) {
    if (!ClientInstinctState.enabled()) {
      return;
    }
    Minecraft client = (Minecraft) (Object) this;
    mobLife$discard(client.options.keyInventory);
    mobLife$discard(client.options.keySwapOffhand);
    mobLife$discard(client.options.keyDrop);
    mobLife$discard(client.options.keyUse);
    mobLife$discard(client.options.keyPickItem);
    for (net.minecraft.client.KeyMapping key : client.options.keyHotbarSlots) {
      mobLife$discard(key);
    }
  }

  private static void mobLife$discard(net.minecraft.client.KeyMapping key) {
    while (key.consumeClick()) {}
    key.setDown(false);
  }
}
