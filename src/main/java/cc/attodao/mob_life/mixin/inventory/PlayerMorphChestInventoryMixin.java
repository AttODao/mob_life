package cc.attodao.mob_life.mixin.inventory;

import cc.attodao.mob_life.gameplay.inventory.MorphChestInventory;
import cc.attodao.mob_life.gameplay.inventory.MorphChestInventoryHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMorphChestInventoryMixin implements MorphChestInventoryHolder {
  @Unique private final MorphChestInventory mobLife$morphChestInventory = new MorphChestInventory();

  @Override
  public MorphChestInventory mobLife$getMorphChestInventory() {
    return mobLife$morphChestInventory;
  }

  @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$readMorphChestInventory(ValueInput input, CallbackInfo ci) {
    mobLife$morphChestInventory.load(input);
  }

  @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$writeMorphChestInventory(ValueOutput output, CallbackInfo ci) {
    mobLife$morphChestInventory.save(output);
  }

  @Inject(method = "dropEquipment", at = @At("TAIL"))
  private void mobLife$dropMorphChestInventory(ServerLevel level, CallbackInfo ci) {
    mobLife$morphChestInventory.dropAll((Player) (Object) this);
  }
}
