package cc.attodao.mob_life.client.mixin.world;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenInvoker {
  @Invoker("onCreate")
  void mobLife$onCreate();
}
