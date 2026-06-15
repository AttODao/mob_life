package cc.attodao.mob_life.network;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.gameplay.ability.MorphAbility;
import cc.attodao.mob_life.gameplay.sleep.MorphSleep;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class MobLifeNetworking {

  private MobLifeNetworking() {}

  public static void registerPayloads() {
    PayloadTypeRegistry.clientboundPlay()
        .register(MorphSelectionPayload.TYPE, MorphSelectionPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(AwkwardnessPayload.TYPE, AwkwardnessPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(FastSprintStatePayload.TYPE, FastSprintStatePayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(GrassEatingStatePayload.TYPE, GrassEatingStatePayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(ChargedJumpPayload.TYPE, ChargedJumpPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(SleepRequestPayload.TYPE, SleepRequestPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(AbilityRequestPayload.TYPE, AbilityRequestPayload.CODEC);
    ServerPlayNetworking.registerGlobalReceiver(
        ChargedJumpPayload.TYPE,
        (payload, context) ->
            ServerMorphManager.performChargedJump(context.player(), payload.chargeAmount()));
    ServerPlayNetworking.registerGlobalReceiver(
        SleepRequestPayload.TYPE, (payload, context) -> MorphSleep.requestSleep(context.player()));
    ServerPlayNetworking.registerGlobalReceiver(
        AbilityRequestPayload.TYPE, (payload, context) -> MorphAbility.request(context.player()));
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

  public record FastSprintStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<FastSprintStatePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "fast_sprint_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FastSprintStatePayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> buffer.writeBoolean(payload.active()),
            buffer -> new FastSprintStatePayload(buffer.readBoolean()));

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
