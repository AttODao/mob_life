package cc.attodao.mob_life.gameplay.vision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cc.attodao.mob_life.config.MorphConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MobVisionPolicyTest {
  private static final float TOLERANCE = 0.0001F;

  @Test
  void derivesACompleteFrameFromVisionAndEnvironment() {
    MorphConfig.Vision vision =
        new MorphConfig.Vision(
            "test",
            1.5F,
            new MorphConfig.ColorResponse(0.1F, 0.2F, 0.3F),
            new MorphConfig.ColorResponse(0.4F, 0.5F, 0.6F),
            new MorphConfig.ColorResponse(0.7F, 0.8F, 0.9F),
            2.0F,
            10.0F,
            18.0F,
            28.0F,
            8.0F,
            0.25F,
            0.8F,
            0.5F,
            0.98F,
            1.1F,
            1.8F,
            0.58F,
            1.4F);

    MobVisionPolicy.Frame frame =
        MobVisionPolicy.derive(
            vision, new MobVisionPolicy.Environment(160.0F, 3.0F, 100.0F, true, 50.0F));

    assertVec(frame.distance().parameters(), 8.0F, 16.0F, 24.0F, 34.0F);
    assertVec(frame.distance().effects(), 24.0F, 0.8F, 1.0F, 0.8F);
    assertVec(frame.distance().depthRange(), 0.05F, 160.0F, 0.0F, 2.0F);
    assertVec(frame.distance().configOverrides(), 0.5F, 0.98F, 1.1F, 14.4F);
    assertVec(frame.distance().redResponse(), 0.1F, 0.2F, 0.3F, 0.0F);
    assertVec(frame.distance().greenResponse(), 0.4F, 0.5F, 0.6F, 0.0F);
    assertVec(frame.distance().blueResponse(), 0.7F, 0.8F, 0.9F, 0.0F);
    assertVec(frame.distance().visionBehavior(), 0.58F, 1.4F, 0.0F, 0.0F);
    assertEquals(
        List.of(
            frame.distance().parameters(),
            frame.distance().effects(),
            frame.distance().depthRange(),
            frame.distance().configOverrides(),
            frame.distance().redResponse(),
            frame.distance().greenResponse(),
            frame.distance().blueResponse(),
            frame.distance().visionBehavior()),
        frame.distance().values());
    assertVec(frame.instinctVignette(), 63.0F / 255.0F, 40.0F / 255.0F, 24.0F / 255.0F, 0.275F);
  }

  @Test
  void uniformLayoutMatchesBothPostChainsAndTheShader() throws IOException {
    List<Path> distanceDeclarations =
        List.of(
            Path.of("src/main/resources/assets/mob_life/post_effect/vision_base.json"),
            Path.of("src/main/resources/assets/mob_life/post_effect/vision_distance.json"),
            Path.of("src/main/resources/assets/mob_life/shaders/post/mob_vision.fsh"));
    for (Path declaration : distanceDeclarations) {
      String text = Files.readString(declaration);
      assertTrue(text.contains(MobVisionPolicy.DISTANCE_UNIFORM), declaration.toString());
      assertTrue(text.contains(MobVisionPolicy.INSTINCT_UNIFORM), declaration.toString());
      if (declaration.toString().endsWith(".json")) {
        assertOrdered(text, jsonNames(MobVisionPolicy.DISTANCE_MEMBERS), declaration);
        assertOrdered(text, jsonNames(MobVisionPolicy.INSTINCT_MEMBERS), declaration);
      } else {
        assertOrdered(text, shaderMembers(MobVisionPolicy.DISTANCE_MEMBERS), declaration);
        assertOrdered(text, shaderMembers(MobVisionPolicy.INSTINCT_MEMBERS), declaration);
      }
    }

    assertEquals(128, MobVisionPolicy.DISTANCE_UNIFORM_BYTES);
    assertEquals(16, MobVisionPolicy.INSTINCT_UNIFORM_BYTES);
  }

  private static void assertOrdered(String text, List<String> names, Path declaration) {
    int previous = -1;
    for (String name : names) {
      int current = text.indexOf(name, previous + 1);
      assertTrue(current > previous, () -> name + " is missing or out of order in " + declaration);
      previous = current;
    }
  }

  private static List<String> jsonNames(List<String> names) {
    return names.stream().map(name -> "\"name\": \"" + name + "\"").toList();
  }

  private static List<String> shaderMembers(List<String> names) {
    return names.stream().map(name -> "vec4 " + name + ";").toList();
  }

  private static void assertVec(MobVisionPolicy.Vec4 value, float x, float y, float z, float w) {
    assertEquals(x, value.x(), TOLERANCE);
    assertEquals(y, value.y(), TOLERANCE);
    assertEquals(z, value.z(), TOLERANCE);
    assertEquals(w, value.w(), TOLERANCE);
  }
}
