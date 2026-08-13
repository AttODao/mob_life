package cc.attodao.mob_life.client.mixin.inventory;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import net.minecraft.client.KeyMapping;
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
    mobLife$captureEscapeAction(client.options.keyAttack, InstinctManager.ESCAPE_ATTACK);
    mobLife$captureEscapeAction(client.options.keyUse, InstinctManager.ESCAPE_USE);
    mobLife$discard(client.options.keyInventory);
    mobLife$discard(client.options.keySwapOffhand);
    mobLife$discard(client.options.keyDrop);
    mobLife$discard(client.options.keyPickItem);
    for (net.minecraft.client.KeyMapping key : client.options.keyHotbarSlots) {
      mobLife$discard(key);
    }
  }

  private static void mobLife$captureEscapeAction(KeyMapping key, int input) {
    boolean pressed = false;
    while (key.consumeClick()) {
      pressed = true;
    }
    ClientInstinctState.recordEscapeAction(input, key.isDown(), pressed);
  }

  private static void mobLife$discard(KeyMapping key) {
    while (key.consumeClick()) {}
    key.setDown(false);
  }
}
