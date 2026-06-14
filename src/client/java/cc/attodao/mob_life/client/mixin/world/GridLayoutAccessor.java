package cc.attodao.mob_life.client.mixin.world;

import java.util.List;
import net.minecraft.client.gui.layouts.GridLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GridLayout.class)
public interface GridLayoutAccessor {
  @Accessor("children")
  List<?> mobLife$getChildren();
}
