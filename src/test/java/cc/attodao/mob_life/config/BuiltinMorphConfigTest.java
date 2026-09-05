package cc.attodao.mob_life.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BuiltinMorphConfigTest {
  @BeforeAll
  static void bootstrapMinecraftRegistries() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  @Test
  void allBuiltinV2ConfigsLoadWithExpectedMovementStates() {
    for (MorphType morph : MorphType.values()) {
      if (!morph.isPlayer()) {
        assertEquals("mob_life:" + morph.id(), MorphConfig.defaults(morph).instinct().profile());
      }
    }

    MorphConfig.Movement cat = MorphConfig.defaults(MorphType.CAT).movement();
    assertEquals(0.6, cat.value(MorphConfig.MovementState.SNEAK).goalSpeedModifier());
    assertEquals(1.33, cat.value(MorphConfig.MovementState.SPRINT).goalSpeedModifier());
    assertEquals(
        1.3, cat.value(MorphConfig.MovementState.SPRINT).movementSpeedAttributeMultiplier());
    assertEquals(cat, MorphConfig.defaults(MorphType.OCELOT).movement());

    MorphConfig.Movement cow = MorphConfig.defaults(MorphType.COW).movement();
    assertFalse(cow.states().containsKey(MorphConfig.MovementState.SNEAK));
    assertEquals(2.0, cow.value(MorphConfig.MovementState.SPRINT).goalSpeedModifier());
    assertMovement(MorphType.SHEEP, 1.0, 1.25);
    assertMovement(MorphType.CHICKEN, 1.0, 1.4);
    assertMovement(MorphType.PIG, 1.0, 1.25);
    assertMovement(MorphType.WOLF, 1.0, 1.5);
    assertMovement(MorphType.RABBIT, 0.6, 2.2);
    assertMovement(MorphType.HORSE, 0.7, 1.0);
    assertMovement(MorphType.DONKEY, 0.7, 1.0);
    assertMovement(MorphType.MULE, 0.7, 1.0);
  }

  @Test
  void oldMovementKeysAndPlayerConfigAreErrors() {
    var old =
        JsonParser.parseString(
                """
                {"schema_version":2,"movement":{"reference_mob_speed":0.2}}
                """)
            .getAsJsonObject();
    assertThrows(
        IllegalArgumentException.class, () -> MorphConfigCodec.validateLayer(MorphType.COW, old));
    assertThrows(IllegalArgumentException.class, () -> MorphConfig.fromJson(MorphType.PLAYER, old));
  }

  @Test
  void wrongSchemaVersionIsAnError() {
    var wrong = JsonParser.parseString("{\"schema_version\":1}").getAsJsonObject();
    var fractional = JsonParser.parseString("{\"schema_version\":2.5}").getAsJsonObject();
    assertThrows(
        IllegalArgumentException.class, () -> MorphConfigCodec.validateLayer(MorphType.COW, wrong));
    assertThrows(
        IllegalArgumentException.class,
        () -> MorphConfigCodec.validateLayer(MorphType.COW, fractional));
  }

  @Test
  void partialLayersAreAllowedButInvalidValuesAreRejected() {
    var partial =
        JsonParser.parseString(
                """
                {"schema_version":2,"movement":{"sprint":{"goal_speed_modifier":1.5}}}
                """)
            .getAsJsonObject();
    MorphConfigCodec.validateLayer(MorphType.COW, partial);

    var unknown =
        JsonParser.parseString("{\"schema_version\":2,\"unknown\":true}").getAsJsonObject();
    var fractionalInteger =
        JsonParser.parseString("{\"schema_version\":2,\"inventory\":{\"hotbar_slots\":2.5}}")
            .getAsJsonObject();
    var outOfRange =
        JsonParser.parseString(
                "{\"schema_version\":2,\"movement\":{\"walk\":{\"goal_speed_modifier\":4.1}}}")
            .getAsJsonObject();
    JsonObject nonFinite = JsonParser.parseString("{\"schema_version\":2}").getAsJsonObject();
    JsonObject attributes = new JsonObject();
    attributes.addProperty("mining_speed", Double.NaN);
    nonFinite.add("attributes", attributes);

    assertThrows(
        IllegalArgumentException.class,
        () -> MorphConfigCodec.validateLayer(MorphType.COW, unknown));
    assertThrows(
        IllegalArgumentException.class,
        () -> MorphConfigCodec.validateLayer(MorphType.COW, fractionalInteger));
    assertThrows(
        IllegalArgumentException.class,
        () -> MorphConfigCodec.validateLayer(MorphType.COW, outOfRange));
    assertThrows(
        IllegalArgumentException.class,
        () -> MorphConfigCodec.validateLayer(MorphType.COW, nonFinite));
  }

  @Test
  void rejectedSynchronizedBatchDoesNotPartiallyInstall() {
    var before = MorphConfigManager.snapshot();
    long generation = MorphConfigManager.generation();
    ArrayList<MobLifeNetworking.MorphConfigEntry> entries = new ArrayList<>();
    for (MorphType morph : MorphType.values()) {
      if (!morph.isPlayer()) {
        entries.add(
            new MobLifeNetworking.MorphConfigEntry(morph.id(), MorphConfigManager.encode(morph)));
      }
    }
    entries.removeLast();

    assertThrows(
        IllegalArgumentException.class,
        () -> MorphConfigManager.installSyncedBatch(generation + 1, entries));
    assertSame(before, MorphConfigManager.snapshot());
    assertEquals(generation, MorphConfigManager.generation());
  }

  private static void assertMovement(MorphType morph, double walk, double sprint) {
    MorphConfig.Movement movement = MorphConfig.defaults(morph).movement();
    assertEquals(walk, movement.value(MorphConfig.MovementState.WALK).goalSpeedModifier());
    assertEquals(sprint, movement.value(MorphConfig.MovementState.SPRINT).goalSpeedModifier());
  }
}
