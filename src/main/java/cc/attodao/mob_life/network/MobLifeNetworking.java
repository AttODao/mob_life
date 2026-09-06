package cc.attodao.mob_life.network;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.config.ServerMobLifeConfig;
import cc.attodao.mob_life.gameplay.ability.MorphAbility;
import cc.attodao.mob_life.gameplay.instinct.InstinctActivity;
import cc.attodao.mob_life.gameplay.instinct.InstinctInput;
import cc.attodao.mob_life.gameplay.instinct.InstinctSyncState;
import cc.attodao.mob_life.gameplay.instinct.MorphInstinct;
import cc.attodao.mob_life.gameplay.jump.GaitType;
import cc.attodao.mob_life.gameplay.movement.MorphBodyYawSync;
import cc.attodao.mob_life.gameplay.sleep.MorphSleep;
import cc.attodao.mob_life.gameplay.view.MorphViewControl;
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
import net.minecraft.world.phys.Vec3;

public final class MobLifeNetworking {

  private MobLifeNetworking() {}

  public static void registerPayloads() {
    PayloadTypeRegistry.clientboundPlay()
        .register(WorldMorphSelectionPromptPayload.TYPE, WorldMorphSelectionPromptPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(MorphProfilesPayload.TYPE, MorphProfilesPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(MorphBodyYawPayload.TYPE, MorphBodyYawPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(ServerConfigPayload.TYPE, ServerConfigPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(MorphSelectionPayload.TYPE, MorphSelectionPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(AwkwardnessPayload.TYPE, AwkwardnessPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(GrassEatingStatePayload.TYPE, GrassEatingStatePayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(PredatorOutlinePayload.TYPE, PredatorOutlinePayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(InstinctStatePayload.TYPE, InstinctStatePayload.CODEC);
    PayloadTypeRegistry.serverboundPlay().register(GaitEventPayload.TYPE, GaitEventPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(MorphBodyYawUpdatePayload.TYPE, MorphBodyYawUpdatePayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(SleepRequestPayload.TYPE, SleepRequestPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(AbilityRequestPayload.TYPE, AbilityRequestPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(WorldMorphSelectionSubmitPayload.TYPE, WorldMorphSelectionSubmitPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(InstinctInputPayload.TYPE, InstinctInputPayload.CODEC);
    ServerPlayNetworking.registerGlobalReceiver(
        MorphBodyYawUpdatePayload.TYPE,
        (payload, context) ->
            context
                .server()
                .execute(() -> MorphBodyYawSync.receive(context.player(), payload.bodyYaw())));
    ServerPlayNetworking.registerGlobalReceiver(
        InstinctInputPayload.TYPE,
        (payload, context) -> {
          MinecraftServer server = context.server();
          ServerPlayer player = context.player();
          server.execute(
              () ->
                  MorphInstinct.receiveInput(
                      player,
                      new InstinctInput(
                          payload.sideways(),
                          payload.forward(),
                          payload.cameraYaw(),
                          payload.cameraPitch(),
                          payload.cameraDelta(),
                          payload.buttons(),
                          payload.screenMode())));
        });
    ServerPlayNetworking.registerGlobalReceiver(
        GaitEventPayload.TYPE,
        (payload, context) -> {
          MinecraftServer server = context.server();
          ServerPlayer player = context.player();
          server.execute(() -> ServerMorphManager.handleGaitEvent(player, payload.gaitType()));
        });
    ServerPlayNetworking.registerGlobalReceiver(
        SleepRequestPayload.TYPE,
        (payload, context) -> {
          MinecraftServer server = context.server();
          ServerPlayer player = context.player();
          server.execute(() -> MorphSleep.requestSleep(player));
        });
    ServerPlayNetworking.registerGlobalReceiver(
        AbilityRequestPayload.TYPE,
        (payload, context) -> {
          MinecraftServer server = context.server();
          ServerPlayer player = context.player();
          server.execute(() -> MorphAbility.request(player));
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
  }

  public record WorldMorphSelectionPromptPayload(List<String> morphIds)
      implements CustomPacketPayload {
    public static final Type<WorldMorphSelectionPromptPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "world_morph_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldMorphSelectionPromptPayload>
        CODEC =
            StreamCodec.of(
                (buffer, payload) -> {
                  buffer.writeVarInt(payload.morphIds().size());
                  for (String morphId : payload.morphIds()) {
                    buffer.writeUtf(morphId);
                  }
                },
                buffer -> {
                  int size = buffer.readVarInt();
                  ArrayList<String> morphIds = new ArrayList<>(size);
                  for (int index = 0; index < size; index++) {
                    morphIds.add(buffer.readUtf());
                  }
                  return new WorldMorphSelectionPromptPayload(morphIds);
                });

    public WorldMorphSelectionPromptPayload {
      morphIds = List.copyOf(morphIds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record MorphConfigEntry(String morphId, String configJson) {}

  public record MorphBodyYawUpdatePayload(float bodyYaw) implements CustomPacketPayload {
    public static final Type<MorphBodyYawUpdatePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "body_yaw_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MorphBodyYawUpdatePayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> buffer.writeFloat(payload.bodyYaw()),
            buffer -> new MorphBodyYawUpdatePayload(buffer.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record MorphBodyYawPayload(int entityId, float bodyYaw) implements CustomPacketPayload {
    public static final Type<MorphBodyYawPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "body_yaw"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MorphBodyYawPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              buffer.writeVarInt(payload.entityId());
              buffer.writeFloat(payload.bodyYaw());
            },
            buffer -> new MorphBodyYawPayload(buffer.readVarInt(), buffer.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record MorphProfilesPayload(long generation, List<MorphConfigEntry> configs)
      implements CustomPacketPayload {
    public static final Type<MorphProfilesPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "morph_profiles"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MorphProfilesPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              buffer.writeVarLong(payload.generation());
              buffer.writeVarInt(payload.configs().size());
              for (MorphConfigEntry entry : payload.configs()) {
                buffer.writeUtf(entry.morphId());
                buffer.writeUtf(entry.configJson());
              }
            },
            buffer -> {
              long generation = buffer.readVarLong();
              int size = buffer.readVarInt();
              if (size < 0 || size > 128) {
                throw new IllegalArgumentException("Invalid morph profile count " + size);
              }
              ArrayList<MorphConfigEntry> configs = new ArrayList<>(size);
              for (int index = 0; index < size; index++) {
                configs.add(new MorphConfigEntry(buffer.readUtf(), buffer.readUtf()));
              }
              return new MorphProfilesPayload(generation, configs);
            });

    public MorphProfilesPayload {
      configs = List.copyOf(configs);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

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

  public record MorphSelectionPayload(String morphId, CompoundTag nbt)
      implements CustomPacketPayload {
    public static final Type<MorphSelectionPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "morph_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MorphSelectionPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              buffer.writeUtf(payload.morphId());
              buffer.writeNbt(payload.nbt());
            },
            buffer -> {
              String morphId = buffer.readUtf();
              CompoundTag nbt = buffer.readNbt();
              return new MorphSelectionPayload(morphId, nbt != null ? nbt : new CompoundTag());
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

  public record GaitEventPayload(GaitType gaitType) implements CustomPacketPayload {
    public static final Type<GaitEventPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "gait_event"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GaitEventPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.gaitType().ordinal()),
            buffer -> {
              int ordinal = buffer.readVarInt();
              GaitType[] values = GaitType.values();
              if (ordinal < 0 || ordinal >= values.length) {
                throw new IllegalArgumentException("Unknown gait type " + ordinal);
              }
              return new GaitEventPayload(values[ordinal]);
            });

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

  public record PredatorOutlinePayload(List<Integer> predators, List<Integer> prey)
      implements CustomPacketPayload {
    public static final Type<PredatorOutlinePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "predator_outlines"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PredatorOutlinePayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              writeIds(buffer, payload.predators());
              writeIds(buffer, payload.prey());
            },
            buffer -> new PredatorOutlinePayload(readIds(buffer), readIds(buffer)));

    public PredatorOutlinePayload {
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

  public record InstinctInputPayload(
      float sideways,
      float forward,
      float cameraYaw,
      float cameraPitch,
      float cameraDelta,
      int buttons,
      int screenMode)
      implements CustomPacketPayload {
    public static final Type<InstinctInputPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "instinct_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InstinctInputPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              buffer.writeFloat(payload.sideways());
              buffer.writeFloat(payload.forward());
              buffer.writeFloat(payload.cameraYaw());
              buffer.writeFloat(payload.cameraPitch());
              buffer.writeFloat(payload.cameraDelta());
              buffer.writeVarInt(payload.buttons());
              buffer.writeVarInt(payload.screenMode());
            },
            buffer ->
                new InstinctInputPayload(
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readVarInt(),
                    buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record InstinctStatePayload(
      boolean active,
      float level,
      double x,
      double y,
      double z,
      boolean onGround,
      float bodyYaw,
      float headYaw,
      float headPitch,
      boolean lookingAtTarget,
      int activity,
      float horizontalDisplacement,
      float horizontalSpeed)
      implements CustomPacketPayload {
    private static final InstinctActivity[] ACTIVITIES = InstinctActivity.values();
    public static final Type<InstinctStatePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "instinct_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InstinctStatePayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              buffer.writeBoolean(payload.active());
              buffer.writeFloat(payload.level());
              buffer.writeDouble(payload.x());
              buffer.writeDouble(payload.y());
              buffer.writeDouble(payload.z());
              buffer.writeBoolean(payload.onGround());
              buffer.writeFloat(payload.bodyYaw());
              buffer.writeFloat(payload.headYaw());
              buffer.writeFloat(payload.headPitch());
              buffer.writeBoolean(payload.lookingAtTarget());
              buffer.writeVarInt(payload.activity());
              buffer.writeFloat(payload.horizontalDisplacement());
              buffer.writeFloat(payload.horizontalSpeed());
            },
            buffer ->
                new InstinctStatePayload(
                    buffer.readBoolean(),
                    buffer.readFloat(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readBoolean(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readFloat(),
                    buffer.readFloat()));

    public InstinctStatePayload {
      if (!Float.isFinite(horizontalDisplacement) || horizontalDisplacement < 0.0F) {
        horizontalDisplacement = 0.0F;
      }
      if (!Float.isFinite(horizontalSpeed) || horizontalSpeed < 0.0F) {
        horizontalSpeed = 0.0F;
      }
    }

    public static InstinctStatePayload fromState(InstinctSyncState state) {
      return new InstinctStatePayload(
          state.active(),
          state.level(),
          state.position().x,
          state.position().y,
          state.position().z,
          state.onGround(),
          state.pose().bodyYaw(),
          state.pose().headYaw(),
          state.pose().headPitch(),
          state.lookingAtTarget(),
          state.activity().ordinal(),
          state.motion().horizontalDisplacement(),
          state.motion().horizontalSpeed());
    }

    public InstinctSyncState toState() {
      InstinctActivity decodedActivity =
          activity >= 0 && activity < ACTIVITIES.length
              ? ACTIVITIES[activity]
              : InstinctActivity.REST;
      return new InstinctSyncState(
          active,
          level,
          new Vec3(x, y, z),
          onGround,
          new MorphViewControl.Pose(bodyYaw, headYaw, headPitch),
          lookingAtTarget,
          decodedActivity,
          new InstinctSyncState.Motion(horizontalDisplacement, horizontalSpeed));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }
}
