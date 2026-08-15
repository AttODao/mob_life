package cc.attodao.mob_life.network;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.config.ServerMobLifeConfig;
import cc.attodao.mob_life.gameplay.ability.MorphAbility;
import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import cc.attodao.mob_life.gameplay.sleep.MorphSleep;
import cc.attodao.mob_life.server.ServerMorphManager;
import cc.attodao.mob_life.world.MorphVariantRequest;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class MobLifeNetworking {

  private MobLifeNetworking() {}

  public static void registerPayloads() {
    PayloadTypeRegistry.clientboundPlay()
        .register(WorldMorphSelectionPromptPayload.TYPE, WorldMorphSelectionPromptPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(ServerConfigPayload.TYPE, ServerConfigPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(MorphSelectionPayload.TYPE, MorphSelectionPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(AwkwardnessPayload.TYPE, AwkwardnessPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(GrassEatingStatePayload.TYPE, GrassEatingStatePayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(InstinctControlPayload.TYPE, InstinctControlPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay().register(OutlinePayload.TYPE, OutlinePayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(ChargedJumpPayload.TYPE, ChargedJumpPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(SleepRequestPayload.TYPE, SleepRequestPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(AbilityRequestPayload.TYPE, AbilityRequestPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(WorldMorphSelectionSubmitPayload.TYPE, WorldMorphSelectionSubmitPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(InstinctEscapeInputPayload.TYPE, InstinctEscapeInputPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(InstinctInterventionPayload.TYPE, InstinctInterventionPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(ClientGuiStatePayload.TYPE, ClientGuiStatePayload.CODEC);
    ServerPlayNetworking.registerGlobalReceiver(
        ChargedJumpPayload.TYPE,
        (payload, context) -> {
          MinecraftServer server = context.server();
          ServerPlayer player = context.player();
          server.execute(
              () -> {
                if (!InstinctManager.isEnabled(player)) {
                  ServerMorphManager.performChargedJump(player, payload.chargeAmount());
                }
              });
        });
    ServerPlayNetworking.registerGlobalReceiver(
        SleepRequestPayload.TYPE,
        (payload, context) -> {
          MinecraftServer server = context.server();
          ServerPlayer player = context.player();
          server.execute(
              () -> {
                if (!InstinctManager.isEnabled(player)) {
                  MorphSleep.requestSleep(player);
                }
              });
        });
    ServerPlayNetworking.registerGlobalReceiver(
        AbilityRequestPayload.TYPE,
        (payload, context) -> {
          MinecraftServer server = context.server();
          ServerPlayer player = context.player();
          server.execute(
              () -> {
                if (!InstinctManager.isEnabled(player)) {
                  MorphAbility.request(player);
                }
              });
        });
    ServerPlayNetworking.registerGlobalReceiver(
        WorldMorphSelectionSubmitPayload.TYPE,
        (payload, context) -> {
          MinecraftServer server = context.server();
          server.execute(
              () ->
                  ServerMorphManager.completeWorldSelection(
                      server, context.player(), payload.morphId(), payload.variantRequest()));
        });
    ServerPlayNetworking.registerGlobalReceiver(
        InstinctEscapeInputPayload.TYPE,
        (payload, context) ->
            context
                .server()
                .execute(() -> InstinctManager.attemptEscape(context.player(), payload.flags())));
    ServerPlayNetworking.registerGlobalReceiver(
        InstinctInterventionPayload.TYPE,
        (payload, context) ->
            context
                .server()
                .execute(
                    () ->
                        InstinctManager.intervene(
                            context.player(), payload.flags(), payload.viewYaw())));
    ServerPlayNetworking.registerGlobalReceiver(
        ClientGuiStatePayload.TYPE,
        (payload, context) ->
            context
                .server()
                .execute(() -> InstinctManager.setClientGuiOpen(context.player(), payload.open())));
  }

  public record WorldMorphSelectionPromptPayload(List<MorphConfigEntry> configs)
      implements CustomPacketPayload {
    public static final Type<WorldMorphSelectionPromptPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "world_morph_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldMorphSelectionPromptPayload>
        CODEC =
            StreamCodec.of(
                (buffer, payload) -> {
                  buffer.writeVarInt(payload.configs().size());
                  for (MorphConfigEntry entry : payload.configs()) {
                    buffer.writeUtf(entry.morphId());
                    buffer.writeUtf(entry.configJson());
                  }
                },
                buffer -> {
                  int size = buffer.readVarInt();
                  ArrayList<MorphConfigEntry> configs = new ArrayList<>(size);
                  for (int index = 0; index < size; index++) {
                    configs.add(new MorphConfigEntry(buffer.readUtf(), buffer.readUtf()));
                  }
                  return new WorldMorphSelectionPromptPayload(configs);
                });

    public WorldMorphSelectionPromptPayload {
      configs = List.copyOf(configs);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record MorphConfigEntry(String morphId, String configJson) {}

  /** Gameplay settings are sent only from the server and never accepted from clients. */
  public record ServerConfigPayload(
      boolean morphEnabled,
      boolean playerMorphEnabled,
      boolean hotbarLimitEnabled,
      boolean inventorySlotLimitEnabled,
      boolean offhandLimitEnabled,
      boolean miningSpeedChangeEnabled,
      boolean reachChangeEnabled)
      implements CustomPacketPayload {
    public static final Type<ServerConfigPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "server_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerConfigPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              buffer.writeBoolean(payload.morphEnabled());
              buffer.writeBoolean(payload.playerMorphEnabled());
              buffer.writeBoolean(payload.hotbarLimitEnabled());
              buffer.writeBoolean(payload.inventorySlotLimitEnabled());
              buffer.writeBoolean(payload.offhandLimitEnabled());
              buffer.writeBoolean(payload.miningSpeedChangeEnabled());
              buffer.writeBoolean(payload.reachChangeEnabled());
            },
            buffer ->
                new ServerConfigPayload(
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean()));

    public ServerMobLifeConfig.Settings settings() {
      return new ServerMobLifeConfig.Settings(
          morphEnabled,
          playerMorphEnabled,
          hotbarLimitEnabled,
          inventorySlotLimitEnabled,
          offhandLimitEnabled,
          miningSpeedChangeEnabled,
          reachChangeEnabled);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record MorphSelectionPayload(String morphId, CompoundTag nbt, String configJson)
      implements CustomPacketPayload {
    public static final Type<MorphSelectionPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "morph_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MorphSelectionPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              buffer.writeUtf(payload.morphId());
              buffer.writeNbt(payload.nbt());
              buffer.writeUtf(payload.configJson());
            },
            buffer -> {
              String morphId = buffer.readUtf();
              CompoundTag nbt = buffer.readNbt();
              return new MorphSelectionPayload(
                  morphId, nbt != null ? nbt : new CompoundTag(), buffer.readUtf());
            });

    public MorphSelectionPayload {
      nbt = nbt.copy();
    }

    @Override
    public CompoundTag nbt() {
      return nbt.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  /**
   * The initial world selection contains an ID plus a fixed cosmetic variant request. It never
   * serializes a client-provided {@link CompoundTag}.
   */
  public record WorldMorphSelectionSubmitPayload(String morphId, MorphVariantRequest variantRequest)
      implements CustomPacketPayload {
    private static final int MAX_MORPH_ID_LENGTH = 64;
    private static final int MAX_VARIANT_IDENTIFIER_LENGTH = 128;

    public static final Type<WorldMorphSelectionSubmitPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "world_morph_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldMorphSelectionSubmitPayload>
        CODEC =
            StreamCodec.of(
                (buffer, payload) -> {
                  buffer.writeUtf(payload.morphId(), MAX_MORPH_ID_LENGTH);
                  writeVariantRequest(buffer, payload.variantRequest());
                },
                buffer ->
                    new WorldMorphSelectionSubmitPayload(
                        buffer.readUtf(MAX_MORPH_ID_LENGTH), readVariantRequest(buffer)));

    public WorldMorphSelectionSubmitPayload {
      variantRequest = variantRequest != null ? variantRequest : MorphVariantRequest.empty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }

    private static void writeVariantRequest(
        RegistryFriendlyByteBuf buffer, MorphVariantRequest request) {
      writeOptionalIdentifier(buffer, request.variantId());
      writeOptionalIdentifier(buffer, request.soundVariantId());
      buffer.writeVarInt(request.rabbitType() + 1);
      buffer.writeVarInt(request.sheepColor() + 1);
      buffer.writeVarInt(request.horseVariant() + 1);
      buffer.writeBoolean(request.baby());
    }

    private static MorphVariantRequest readVariantRequest(RegistryFriendlyByteBuf buffer) {
      return new MorphVariantRequest(
          readOptionalIdentifier(buffer),
          readOptionalIdentifier(buffer),
          readOptionalValue(buffer, 255),
          readOptionalValue(buffer, 15),
          readOptionalValue(buffer, 0xFFFF),
          buffer.readBoolean());
    }

    private static void writeOptionalIdentifier(RegistryFriendlyByteBuf buffer, String value) {
      boolean present = value != null && !value.isEmpty();
      buffer.writeBoolean(present);
      if (present) {
        buffer.writeUtf(value, MAX_VARIANT_IDENTIFIER_LENGTH);
      }
    }

    private static String readOptionalIdentifier(RegistryFriendlyByteBuf buffer) {
      return buffer.readBoolean() ? buffer.readUtf(MAX_VARIANT_IDENTIFIER_LENGTH) : "";
    }

    private static int readOptionalValue(RegistryFriendlyByteBuf buffer, int maximum) {
      int encoded = buffer.readVarInt();
      return encoded >= 0 && encoded <= maximum + 1 ? encoded - 1 : MorphVariantRequest.UNSPECIFIED;
    }
  }

  public record ChargedJumpPayload(int chargeAmount) implements CustomPacketPayload {
    public static final Type<ChargedJumpPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "charged_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChargedJumpPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.chargeAmount()),
            buffer -> new ChargedJumpPayload(buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record AwkwardnessPayload(float value) implements CustomPacketPayload {
    public static final Type<AwkwardnessPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "awkwardness"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AwkwardnessPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> buffer.writeFloat(payload.value()),
            buffer -> new AwkwardnessPayload(buffer.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record GrassEatingStatePayload(int entityId, int remainingTicks)
      implements CustomPacketPayload {
    public static final Type<GrassEatingStatePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "grass_eating_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GrassEatingStatePayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              buffer.writeVarInt(payload.entityId());
              buffer.writeVarInt(payload.remainingTicks());
            },
            buffer -> new GrassEatingStatePayload(buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record SleepRequestPayload() implements CustomPacketPayload {
    public static final Type<SleepRequestPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "sleep_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SleepRequestPayload> CODEC =
        StreamCodec.unit(new SleepRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record AbilityRequestPayload() implements CustomPacketPayload {
    public static final Type<AbilityRequestPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "ability_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityRequestPayload> CODEC =
        StreamCodec.unit(new AbilityRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record InstinctEscapeInputPayload(int flags) implements CustomPacketPayload {
    public static final Type<InstinctEscapeInputPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "instinct_escape_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InstinctEscapeInputPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> buffer.writeByte(payload.flags()),
            buffer -> new InstinctEscapeInputPayload(buffer.readUnsignedByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record InstinctInterventionPayload(int flags, float viewYaw)
      implements CustomPacketPayload {
    public static final Type<InstinctInterventionPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "instinct_intervention"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InstinctInterventionPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              buffer.writeByte(payload.flags());
              buffer.writeFloat(payload.viewYaw());
            },
            buffer ->
                new InstinctInterventionPayload(buffer.readUnsignedByte(), buffer.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  /**
   * Mirrors client-only screens so server-owned idle timing can pause without blocking forced AI.
   */
  public record ClientGuiStatePayload(boolean open) implements CustomPacketPayload {
    public static final Type<ClientGuiStatePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "client_gui_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientGuiStatePayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> buffer.writeBoolean(payload.open()),
            buffer -> new ClientGuiStatePayload(buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record InstinctControlPayload(
      boolean enabled,
      int state,
      float targetYaw,
      float targetPitch,
      int eatTicks,
      float instinctLevel,
      boolean playerInterventionAllowed,
      float movementX,
      float movementY,
      float movementZ)
      implements CustomPacketPayload {
    public static final Type<InstinctControlPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "instinct_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InstinctControlPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              buffer.writeBoolean(payload.enabled());
              buffer.writeVarInt(payload.state());
              buffer.writeFloat(payload.targetYaw());
              buffer.writeFloat(payload.targetPitch());
              buffer.writeVarInt(payload.eatTicks());
              buffer.writeFloat(payload.instinctLevel());
              buffer.writeBoolean(payload.playerInterventionAllowed());
              buffer.writeFloat(payload.movementX());
              buffer.writeFloat(payload.movementY());
              buffer.writeFloat(payload.movementZ());
            },
            buffer ->
                new InstinctControlPayload(
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readVarInt(),
                    buffer.readFloat(),
                    buffer.readBoolean(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record OutlinePayload(List<Integer> predators, List<Integer> prey)
      implements CustomPacketPayload {
    public static final Type<OutlinePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "outlines"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OutlinePayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              writeIds(buffer, payload.predators());
              writeIds(buffer, payload.prey());
            },
            buffer -> new OutlinePayload(readIds(buffer), readIds(buffer)));

    public OutlinePayload {
      predators = List.copyOf(predators);
      prey = List.copyOf(prey);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }

    private static void writeIds(RegistryFriendlyByteBuf buffer, List<Integer> ids) {
      buffer.writeVarInt(ids.size());
      ids.forEach(buffer::writeVarInt);
    }

    private static List<Integer> readIds(RegistryFriendlyByteBuf buffer) {
      int size = Math.min(buffer.readVarInt(), 4096);
      ArrayList<Integer> ids = new ArrayList<>(size);
      for (int index = 0; index < size; index++) {
        ids.add(buffer.readVarInt());
      }
      return ids;
    }
  }
}
