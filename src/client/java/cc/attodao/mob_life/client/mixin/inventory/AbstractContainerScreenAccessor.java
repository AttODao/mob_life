package cc.attodao.mob_life.client.mixin.inventory;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
  @Accessor("imageHeight")
  @Mutable
  void mobLife$setImageHeight(int imageHeight);

  @Accessor("topPos")
  void mobLife$setTopPos(int topPos);

  @Accessor("titleLabelX")
  void mobLife$setTitleLabelX(int titleLabelX);

  @Accessor("titleLabelY")
  void mobLife$setTitleLabelY(int titleLabelY);
}
