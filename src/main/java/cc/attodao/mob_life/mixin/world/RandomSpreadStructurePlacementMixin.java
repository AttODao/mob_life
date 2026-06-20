package cc.attodao.mob_life.mixin.world;

import cc.attodao.mob_life.world.MorphInitialStructures;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomSpreadStructurePlacement.class)
public abstract class RandomSpreadStructurePlacementMixin {

  @Inject(method = "isPlacementChunk", at = @At("HEAD"), cancellable = true)
  private void mobLife$forceInitialSpawnStructureChunk(
      ChunkGeneratorStructureState state,
      int sourceX,
      int sourceZ,
      CallbackInfoReturnable<Boolean> cir) {
    if (MorphInitialStructures.shouldForceStructurePlacement(
        (RandomSpreadStructurePlacement) (Object) this, state, sourceX, sourceZ)) {
      cir.setReturnValue(true);
    }
  }
}
