package cc.attodao.mob_life.client.mixin.inventory;

import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {

  @Shadow @Final private RecipeBookComponent<?> recipeBookComponent;

  @Inject(method = "initButton", at = @At("HEAD"), cancellable = true)
  private void mobLife$hideMorphRecipeBookButton(CallbackInfo ci) {
    if (mobLife$isMorphInventory()) {
      mobLife$hideRecipeBook();
      ci.cancel();
    }
  }

  @Inject(method = "containerTick", at = @At("TAIL"))
  private void mobLife$keepMorphRecipeBookClosed(CallbackInfo ci) {
    if (mobLife$isMorphInventory()) {
      mobLife$hideRecipeBook();
    }
  }

  private boolean mobLife$isMorphInventory() {
    MorphType morph = ClientMorphState.morph();
    return (Object) this instanceof InventoryScreen && morph != null;
  }

  private void mobLife$hideRecipeBook() {
    ((RecipeBookComponentInvoker) recipeBookComponent).mobLife$setVisible(false);
  }
}
