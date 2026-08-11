package cc.attodao.mob_life.client.screen;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import cc.attodao.mob_life.morph.MorphType;
import com.mojang.authlib.GameProfile;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.server.ServerLinks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

final class MorphPreviewFactory {
  private static final int PREVIEW_ENTITY_ID_BASE = 1_000_000;

  private final Screen returnScreen;
  private Level previewLevel;

  MorphPreviewFactory(Screen returnScreen) {
    this.returnScreen = returnScreen;
  }

  EnumMap<MorphType, LivingEntity> build(Minecraft minecraft, List<MorphType> morphTypes) {
    EnumMap<MorphType, LivingEntity> previews = new EnumMap<>(MorphType.class);
    Level level = previewEntityLevel(minecraft);
    if (level == null) {
      return previews;
    }

    for (MorphType morph : morphTypes) {
      previews.put(morph, createPreview(minecraft, level, morph));
    }
    return previews;
  }

  private LivingEntity createPreview(Minecraft minecraft, Level level, MorphType morph) {
    if (morph.isPlayer()) {
      return createPlayerPreview(minecraft, level);
    }

    Entity candidate = MorphEntityFactory.create(MorphDefinition.of(morph), level);
    if (!(candidate instanceof LivingEntity livingPreview)) {
      return null;
    }

    return assignPreviewId(livingPreview, morph);
  }

  private LivingEntity createPlayerPreview(Minecraft minecraft, Level level) {
    if (minecraft.player instanceof LivingEntity livingPreview) {
      return livingPreview;
    }
    if (level instanceof ClientLevel clientLevel) {
      PreviewPlayer preview = new PreviewPlayer(clientLevel, profile(minecraft));
      preview.setPos(0.0, 0.0, 0.0);
      return assignPreviewId(preview, MorphType.PLAYER);
    }
    return null;
  }

  private Level previewEntityLevel(Minecraft minecraft) {
    if (minecraft == null) {
      return null;
    }

    if (minecraft.level != null) {
      return minecraft.level;
    }

    if (previewLevel == null) {
      previewLevel = createPreviewLevel(minecraft);
    }
    return previewLevel;
  }

  private Level createPreviewLevel(Minecraft minecraft) {
    try {
      if (!(returnScreen instanceof CreateWorldScreen createWorldScreen)) {
        return null;
      }

      WorldCreationContext worldCreationContext = createWorldScreen.getUiState().getSettings();
      RegistryAccess.Frozen registryAccess = worldCreationContext.worldgenLoadContext();
      var dimensionLookup = registryAccess.lookupOrThrow(Registries.DIMENSION_TYPE);
      Holder<DimensionType> dimensionType =
          dimensionLookup
              .get(BuiltinDimensionTypes.OVERWORLD)
              .orElseGet(() -> dimensionLookup.getOrThrow(BuiltinDimensionTypes.OVERWORLD_CAVES));

      WorldSessionTelemetryManager telemetryManager =
          new WorldSessionTelemetryManager(
              TelemetryEventSender.DISABLED,
              false,
              Duration.ZERO,
              "mob_life_preview",
              UUID.randomUUID());
      CommonListenerCookie cookie =
          new CommonListenerCookie(
              new LevelLoadTracker(),
              profile(minecraft),
              telemetryManager,
              registryAccess,
              worldCreationContext.dataConfiguration().enabledFeatures(),
              "",
              null,
              null,
              Map.of(),
              new ChatComponent.State(List.of(), List.of(), List.of()),
              Map.of(),
              ServerLinks.EMPTY,
              Map.of(),
              false);
      ClientPacketListener listener =
          new ClientPacketListener(
              minecraft,
              new Connection(net.minecraft.network.protocol.PacketFlow.CLIENTBOUND),
              cookie);
      ClientLevelData data =
          new ClientLevelData(net.minecraft.world.Difficulty.NORMAL, false, false);
      return new ClientLevel(
          listener,
          data,
          Level.OVERWORLD,
          dimensionType,
          3,
          3,
          minecraft.levelExtractor,
          false,
          0L,
          63);
    } catch (RuntimeException exception) {
      MobLife.LOGGER.warn("Failed to create morph selection preview level", exception);
      return null;
    }
  }

  private static GameProfile profile(Minecraft minecraft) {
    if (minecraft.getUser() != null) {
      return new GameProfile(minecraft.getUser().getProfileId(), minecraft.getUser().getName());
    }
    return new GameProfile(UUID.randomUUID(), "preview");
  }

  private static <T extends LivingEntity> T assignPreviewId(T preview, MorphType morph) {
    // 26.2 render-state extraction requires an ID even for inventory previews.
    preview.setId(PREVIEW_ENTITY_ID_BASE + morph.ordinal());
    return preview;
  }

  private static final class PreviewPlayer extends RemotePlayer {
    private PreviewPlayer(ClientLevel level, GameProfile gameProfile) {
      super(level, gameProfile);
    }

    @Override
    protected PlayerInfo getPlayerInfo() {
      return null;
    }
  }
}
