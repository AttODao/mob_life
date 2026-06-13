package cc.attodao.mob_life.client.mixin.world;

import net.minecraft.client.gui.layouts.GridLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(GridLayout.class)
public interface GridLayoutAccessor {
	@Accessor("children")
	List<?> mobLife$getChildren();
}
