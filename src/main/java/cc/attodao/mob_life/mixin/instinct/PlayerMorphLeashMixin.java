package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.targeting.MorphRelations;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMorphLeashMixin implements Leashable {
  @Unique private Leashable.LeashData mobLife$leashData;

  @Override
  public Leashable.LeashData getLeashData() {
    return mobLife$leashData;
  }

  @Override
  public void setLeashData(Leashable.LeashData leashData) {
    mobLife$leashData = leashData;
  }

  @Override
  public boolean canBeLeashed() {
    Player player = (Player) (Object) this;
    MorphType morph = MorphRelations.morphOf(player);
    if (morph == null) {
      return false;
    }
    Entity proxy = morph.entityType().create(player.level(), EntitySpawnReason.COMMAND);
    return proxy instanceof Mob mob && mob.canBeLeashed();
  }

  @Override
  public boolean supportQuadLeash() {
    MorphType morph = MorphRelations.morphOf((Player) (Object) this);
    return morph != null && morph.isEquine();
  }

  @Override
  public Vec3[] getQuadLeashOffsets() {
    Player player = (Player) (Object) this;
    MorphType morph = MorphRelations.morphOf(player);
    if (morph != null && morph.isEquine()) {
      return Leashable.createQuadLeashOffsets(player, 0.04, 0.52, 0.23, 0.87);
    }
    return Leashable.super.getQuadLeashOffsets();
  }

  @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$readMorphLeash(ValueInput input, CallbackInfo ci) {
    readLeashData(input);
  }

  @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$writeMorphLeash(ValueOutput output, CallbackInfo ci) {
    writeLeashData(output, mobLife$leashData);
  }
}
