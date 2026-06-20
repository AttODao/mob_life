package cc.attodao.mob_life.mixin.world;

import cc.attodao.mob_life.world.ChunkGeneratorStructureStateAccess;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkGeneratorStructureState.class)
public abstract class ChunkGeneratorStructureStateMixin
    implements ChunkGeneratorStructureStateAccess {

  @Shadow @Final private BiomeSource biomeSource;

  @Override
  public BiomeSource mobLife$getBiomeSource() {
    return biomeSource;
  }
}
