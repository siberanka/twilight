package com.siberanka.twilight.compiler;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DisplayEntityResourcesTest {
    @Test void mirrorsExplicitAndFallbackLeftContextsWithoutMirroringScale() {
        JsonObject geometry = JsonParser.parseString("""
                {"minecraft:geometry":[{"description":{},"bones":[{"name":"mesh","cubes":[]}]}]}
                """).getAsJsonObject();
        JsonObject display = JsonParser.parseString("""
                {"thirdperson_righthand":{"translation":[2,3,4],"rotation":[10,20,30],"scale":[1,2,3]},
                 "firstperson_righthand":{"translation":[8,8,8]},
                 "firstperson_lefthand":{"translation":[5,6,7],"rotation":[40,50,60],"scale":[4,5,6]}}
                """).getAsJsonObject();
        Map<String, byte[]> files = new HashMap<>();
        new DisplayEntityResources().add(files, "twilight:test", "test", geometry, display);
        JsonObject bones = json(files, "animations/display.test.json").getAsJsonObject("animations")
                .getAsJsonObject("animation.twilight.display.test").getAsJsonObject("bones");
        assertArrayEquals(new double[]{-2,3,-4}, evaluate(bones, "item_t", "position", 1));
        assertArrayEquals(new double[]{2,3,-4}, evaluate(bones, "item_t", "position", 2));
        assertArrayEquals(new double[]{-5,6,-7}, evaluate(bones, "item_t", "position", 3));
        assertArrayEquals(new double[]{1,2,3}, evaluate(bones, "item_s", "scale", 1));
        assertArrayEquals(new double[]{4,5,6}, evaluate(bones, "item_s", "scale", 3));
        assertArrayEquals(new double[]{10,0,0}, evaluate(bones, "item_x", "rotation", 1));
        assertArrayEquals(new double[]{0,20,0}, evaluate(bones, "item_y", "rotation", 1));
        assertArrayEquals(new double[]{0,0,60}, evaluate(bones, "item_z", "rotation", 3));
    }

    private static double[] evaluate(JsonObject bones, String bone, String component, int context) {
        JsonArray values = bones.getAsJsonObject(bone).getAsJsonArray(component);
        double[] result = new double[3];
        MolangMath math = new MolangMath();
        for (int axis = 0; axis < 3; axis++) result[axis] = math.eval(values.get(axis).getAsString()
                .replace("q.property('twilight:appearance')", Integer.toString(context)));
        return result;
    }

    @Test void emitsIndependentCenteredRigWithoutChangingJavaOrAttachableGeometry() {
        JsonObject original = JsonParser.parseString("""
                {"format_version":"1.21.0","minecraft:geometry":[{"description":{"identifier":"geometry.test"},
                "bones":[{"name":"bone","binding":"hand","pivot":[0,8,0],"cubes":[
                {"origin":[-8,2,-8],"size":[16,12,16],"pivot":[0,8,0]}]}]}]}
                """).getAsJsonObject();
        String before = original.toString();
        Map<String, byte[]> files = new LinkedHashMap<>();
        DisplayEntityResources resources = new DisplayEntityResources();
        resources.add(files, "twilight:first", "first", original, new JsonObject());
        resources.add(files, "twilight:second", "second", original, new JsonObject());
        resources.finish(files);
        assertEquals(before, original.toString());
        JsonArray bones = json(files, "models/entity/display.first.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        Set<String> names = new HashSet<>();
        for (JsonElement value : bones) {
            JsonObject bone = value.getAsJsonObject();
            if (bone.has("parent")) assertTrue(names.contains(bone.get("parent").getAsString()));
            assertTrue(names.add(bone.get("name").getAsString()));
            assertFalse(bone.has("binding"));
        }
        JsonObject cube = bones.get(bones.size() - 1).getAsJsonObject().getAsJsonArray("cubes").get(0).getAsJsonObject();
        assertEquals(-6, cube.getAsJsonArray("origin").get(1).getAsInt());
        assertEquals(0, cube.getAsJsonArray("pivot").get(1).getAsInt());
        assertEquals(0, json(files, "twilight/display-index.json").get("twilight:first").getAsInt());
        assertEquals(1, json(files, "twilight/display-index.json").get("twilight:second").getAsInt());
        String entity = new String(files.get("entity/twilight_display.entity.json"), StandardCharsets.UTF_8);
        Set<String> properties = new HashSet<>();
        var matcher = java.util.regex.Pattern.compile("q\\.property\\('([^']+)'\\)").matcher(entity);
        while (matcher.find()) properties.add(matcher.group(1));
        // Two complete TRSR endpoints plus timing, revision, and packed item/context.
        assertEquals(32, properties.size());
        JsonObject pose = json(files, "animations/twilight_display.json").getAsJsonObject("animations")
                .getAsJsonObject("animation.twilight.display_pose").getAsJsonObject("bones");
        assertEquals(180, pose.getAsJsonObject("item_flip").getAsJsonArray("rotation").get(1).getAsInt());
        assertEquals(0, pose.getAsJsonObject("yaw").getAsJsonArray("rotation").get(1).getAsInt(),
                "The actor already applies body yaw; the mesh must not apply it a second time");
        JsonObject flip = java.util.stream.StreamSupport.stream(bones.spliterator(), false)
                .map(JsonElement::getAsJsonObject).filter(b -> b.get("name").getAsString().equals("item_flip")).findFirst().orElseThrow();
        assertEquals("rx", flip.get("parent").getAsString(), "Java adds the item frame after both display rotations");
        assertTrue(entity.contains("v.rev=-1"), "pre_animation must not read an uninitialized revision variable");
    }

    private static JsonObject json(Map<String, byte[]> files, String path) {
        return JsonParser.parseString(new String(files.get(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
