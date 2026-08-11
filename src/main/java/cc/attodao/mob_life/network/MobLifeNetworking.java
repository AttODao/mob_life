package cc.attodao.mob_life.network;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.gameplay.ability.MorphAbility;
import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import cc.attodao.mob_life.gameplay.sleep.MorphSleep;
import cc.attodao.mob_life.server.ServerMorphManager;
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
        .register(InstinctEnterPayload.TYPE, InstinctEnterPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(InstinctExitPayload.TYPE, InstinctExitPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(InstinctRestHoldPayload.TYPE, InstinctRestHoldPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(InstinctInterventionPayload.TYPE, InstinctInterventionPayload.CODEC);
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
                      server, payload.morphId(), payload.nbt()));
        });
    ServerPlayNetworking.registerGlobalReceiver(
        InstinctEnterPayload.TYPE,
        (payload, context) ->
            context.server().execute(() -> InstinctManager.enable(context.player())));
    ServerPlayNetworking.registerGlobalReceiver(
        InstinctExitPayload.TYPE,
        (payload, context) ->
            context.server().execute(() -> InstinctManager.requestExit(context.player())));
    ServerPlayNetworking.registerGlobalReceiver(
        InstinctRestHoldPayload.TYPE,
        (payload, context) ->
            context.server().execute(() -> InstinctManager.holdRestForExit(context.player())));
    ServerPlayNetworking.registerGlobalReceiver(
        InstinctInterventionPayload.TYPE,
        (payload, context) ->
            context
                .server()
                .execute(
                    () ->
                        InstinctManager.intervene(
                            context.player(), payload.flags(), payload.viewYaw())));
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

  public record WorldMorphSelectionSubmitPayload(String morphId, CompoundTag nbt)
      implements CustomPacketPayload {
    public static final Type<WorldMorphSelectionSubmitPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "world_morph_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldMorphSelectionSubmitPayload>
        CODEC =
            StreamCodec.of(
                (buffer, payload) -> {
                  buffer.writeUtf(payload.morphId());
                  buffer.writeNbt(payload.nbt());
                },
                buffer -> {
                  String morphId = buffer.readUtf();
                  CompoundTag nbt = buffer.readNbt();
                  return new WorldMorphSelectionSubmitPayload(
                      morphId, nbt != null ? nbt : new CompoundTag());
                });

    public WorldMorphSelectionSubmitPayload {
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

  public record InstinctEnterPayload() implements CustomPacketPayload {
    public static final Type<InstinctEnterPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "instinct_enter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InstinctEnterPayload> CODEC =
        StreamCodec.unit(new InstinctEnterPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record InstinctExitPayload() implements CustomPacketPayload {
    public static final Type<InstinctExitPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "instinct_exit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InstinctExitPayload> CODEC =
        StreamCodec.unit(new InstinctExitPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record InstinctRestHoldPayload() implements CustomPacketPayload {
    public static final Type<InstinctRestHoldPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "instinct_rest_hold"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InstinctRestHoldPayload> CODEC =
        StreamCodec.unit(new InstinctRestHoldPayload());

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

  public record InstinctControlPayload(
      boolean enabled,
      int state,
      float targetYaw,
      float targetPitch,
      int eatTicks,
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
