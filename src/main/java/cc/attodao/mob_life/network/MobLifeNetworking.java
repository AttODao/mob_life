package cc.attodao.mob_life.network;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.gameplay.ability.MorphAbility;
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
    PayloadTypeRegistry.serverboundPlay()
        .register(ChargedJumpPayload.TYPE, ChargedJumpPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(SleepRequestPayload.TYPE, SleepRequestPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(AbilityRequestPayload.TYPE, AbilityRequestPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(WorldMorphSelectionSubmitPayload.TYPE, WorldMorphSelectionSubmitPayload.CODEC);
    ServerPlayNetworking.registerGlobalReceiver(
        ChargedJumpPayload.TYPE,
        (payload, context) -> {
          MinecraftServer server = context.server();
          ServerPlayer player = context.player();
          server.execute(
              () -> ServerMorphManager.performChargedJump(player, payload.chargeAmount()));
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
                      server, payload.morphId(), payload.nbt()));
        });
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
}
