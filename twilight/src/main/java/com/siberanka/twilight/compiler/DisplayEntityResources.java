/* Copyright (c) 2026 siberanka. Licensed under the GNU LGPL v3.0 or later. */
package com.siberanka.twilight.compiler;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Resource half of the item-display protocol. Java remains the animation authority. */
final class DisplayEntityResources {
    private static final Gson JSON = new GsonBuilder().disableHtmlEscaping().create();
    private final JsonObject index = new JsonObject();
    private final JsonObject geometries = new JsonObject();
    private final JsonObject textures = new JsonObject();
    private final JsonObject animations = new JsonObject();
    private final JsonArray animate = new JsonArray();

    void add(Map<String, byte[]> files, String identifier, String safe, JsonObject itemGeometry,
             JsonObject display) {
        add(files, identifier, safe, itemGeometry, List.of(display));
    }

    void add(Map<String, byte[]> files, String identifier, String safe, JsonObject itemGeometry,
             List<JsonObject> displays) {
        int variant = index.size();
        index.addProperty(identifier, variant);
        String key = "v" + variant;
        geometries.addProperty(key, "geometry.twilight.display." + safe);
        textures.addProperty(key, "textures/twilight/" + safe);
        JsonObject geometry = itemGeometry.deepCopy();
        JsonObject body = geometry.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        body.getAsJsonObject("description").addProperty("identifier", "geometry.twilight.display." + safe);
        // Animated translations can leave the static mesh bounds.
        body.getAsJsonObject("description").addProperty("visible_bounds_width", 128);
        body.getAsJsonObject("description").addProperty("visible_bounds_height", 128);
        JsonArray bones = new JsonArray();
        String parent = null;
        for (String name : List.of("yaw", "pitch", "translation", "lz", "ly", "lx", "scale",
                "rz", "ry", "rx", "item_flip")) {
            JsonObject bone = new JsonObject();
            bone.addProperty("name", name);
            if (parent != null) bone.addProperty("parent", parent);
            bone.add("pivot", array(0, 0, 0));
            bones.add(bone);
            parent = name;
        }
        JsonArray meshes = body.getAsJsonArray("bones");
        if (meshes.size() != displays.size()) throw new IllegalArgumentException("Composite mesh/pose count mismatch");
        JsonObject staticBones = new JsonObject();
        for (int part = 0; part < meshes.size(); part++) {
            String prefix = meshes.size() == 1 ? "item_" : "item_" + part + "_";
            parent = "item_flip";
            for (String suffix : List.of("t", "x", "y", "z", "s")) {
                JsonObject bone = new JsonObject();
                bone.addProperty("name", prefix + suffix);
                bone.addProperty("parent", parent);
                bone.add("pivot", array(0, 0, 0));
                bones.add(bone);
                parent = prefix + suffix;
            }
            JsonObject mesh = meshes.get(part).getAsJsonObject();
            mesh.remove("binding");
            mesh.addProperty("parent", parent);
            mesh.add("pivot", array(0, 0, 0));
            // Item rendering uses a centered mesh; attachables use a hand pivot at y=8.
            for (JsonElement value : mesh.getAsJsonArray("cubes")) {
                JsonObject cube = value.getAsJsonObject();
                for (String vector : List.of("origin", "pivot")) if (cube.has(vector)) {
                    JsonArray coordinates = cube.getAsJsonArray(vector);
                    coordinates.set(1, new JsonPrimitive(coordinates.get(1).getAsDouble() - 8));
                }
            }
            bones.add(mesh);
            addContextTransforms(staticBones, prefix, displays.get(part));
        }
        body.add("bones", bones);
        put(files, "models/entity/display." + safe + ".geo.json", geometry);

        String animationId = "animation.twilight.display." + safe;
        animations.addProperty(key, animationId);
        JsonObject condition = new JsonObject();
        condition.addProperty(key, "math.floor(q.property('twilight:appearance')/9)==" + variant);
        animate.add(condition);
        JsonObject animation = new JsonObject(); animation.addProperty("loop", true); animation.add("bones", staticBones);
        put(files, "animations/display." + safe + ".json", property("animations", property(animationId, animation), "1.10.0"));
    }

    private static void addContextTransforms(JsonObject staticBones, String prefix, JsonObject display) {
        // Java ItemDisplayContext network ordinals, including NONE.
        String[] contexts = {"none", "thirdperson_lefthand", "thirdperson_righthand", "firstperson_lefthand",
                "firstperson_righthand", "head", "gui", "ground", "fixed"};
        for (String component : List.of("translation", "rotation", "scale")) {
            JsonArray[] values = new JsonArray[contexts.length];
            for (int c = 0; c < contexts.length; c++) {
                JsonObject transform = display == null ? null : display.getAsJsonObject(contexts[c]);
                if (transform == null && display != null && (c == 1 || c == 3)) {
                    transform = display.getAsJsonObject(contexts[c + 1]);
                }
                values[c] = transform != null && transform.has(component) ? transform.getAsJsonArray(component)
                        : array(component.equals("scale") ? 1 : 0, component.equals("scale") ? 1 : 0,
                        component.equals("scale") ? 1 : 0);
            }
            JsonArray expressions = new JsonArray();
            for (int axis = 0; axis < 3; axis++) {
                String expression = "0";
                for (int c = contexts.length - 1; c >= 0; c--) {
                    double number = values[c].get(axis).getAsDouble();
                    // ItemTransform.apply mirrors explicit left poses as well as fallback poses.
                    if ((c == 1 || c == 3) && (component.equals("translation") && axis == 0
                            || component.equals("rotation") && axis != 0)) number = -number;
                    if (component.equals("translation") && axis == 2 || component.equals("rotation") && axis != 0) number = -number;
                    expression = "math.mod(q.property('twilight:appearance'),9)==" + c + "?" + number + ":(" + expression + ")";
                }
                expressions.add(expression);
            }
            if (component.equals("rotation")) {
                for (int axis = 0; axis < 3; axis++) {
                    JsonArray rotation = array(0, 0, 0);
                    rotation.set(axis, expressions.get(axis));
                    staticBones.add(prefix + "xyz".charAt(axis), property("rotation", rotation));
                }
            } else staticBones.add(prefix + (component.equals("scale") ? "s" : "t"),
                    property(component.equals("translation") ? "position" : "scale", expressions));
        }
    }

    void finish(Map<String, byte[]> files) {
        if (index.isEmpty()) return;
        put(files, "twilight/display-index.json", index);
        JsonObject description = new JsonObject();
        description.addProperty("identifier", "twilight:item_display");
        description.add("materials", stringProperty("default", "entity_alphatest"));
        description.add("textures", textures); description.add("geometry", geometries);
        animations.addProperty("pose", "animation.twilight.display_pose");
        description.add("animations", animations); animate.add("pose");
        JsonObject scripts = new JsonObject(); scripts.add("animate", animate);
        scripts.add("initialize", strings("v.rev=-1;v.start=0;v.p=1;v.t0=0;v.t1=0;v.t2=0;v.t3=1;v.t4=1;v.t5=1;"
                + "v.lex=0;v.ley=0;v.lez=0;v.rex=0;v.rey=0;v.rez=0;"));
        JsonArray pre = new JsonArray();
        pre.add("v.rev!=q.property('twilight:revision')?{v.rev=q.property('twilight:revision');v.start=q.life_time;};"
                + "v.p=q.property('twilight:seconds')<=0?1:math.clamp((q.life_time-v.start-q.property('twilight:delay'))/q.property('twilight:seconds'),0,1);");
        for (int i = 0; i < 6; i++) pre.add("v.t" + i + "=math.lerp(q.property('twilight:a" + i
                + "'),q.property('twilight:b" + i + "'),v.p);");
        pre.add(quaternionScript("l", 6)); pre.add(quaternionScript("r", 10));
        scripts.add("pre_animation", pre); description.add("scripts", scripts);
        description.add("render_controllers", strings("controller.render.twilight.display"));
        put(files, "entity/twilight_display.entity.json", property("minecraft:client_entity", property("description", description), "1.10.0"));
        JsonObject arrays = new JsonObject();
        JsonArray geo = new JsonArray(), tex = new JsonArray();
        for (String key : geometries.keySet()) { geo.add("Geometry." + key); tex.add("Texture." + key); }
        arrays.add("geometries", property("Array.models", geo)); arrays.add("textures", property("Array.textures", tex));
        JsonObject controller = new JsonObject(); controller.add("arrays", arrays);
        controller.addProperty("geometry", "Array.models[math.floor(q.property('twilight:appearance')/9)]");
        controller.add("textures", strings("Array.textures[math.floor(q.property('twilight:appearance')/9)]"));
        JsonArray materials = new JsonArray(); materials.add(stringProperty("*", "Material.default")); controller.add("materials", materials);
        JsonArray visibility = new JsonArray(); visibility.add(stringProperty("*", "math.floor(q.property('twilight:appearance')/9)>=0"));
        controller.add("part_visibility", visibility);
        put(files, "render_controllers/twilight_display.json", property("render_controllers", property("controller.render.twilight.display", controller), "1.10.0"));
        JsonObject bones = new JsonObject();
        // Bedrock already rotates the actor by its body yaw. Applying it again
        // here rotates the entire display twice (hidden by zero-yaw fixtures).
        bones.add("yaw", property("rotation", array(0, 0, 0)));
        bones.add("pitch", property("rotation", strings("q.target_x_rotation", "0", "0")));
        bones.add("translation", property("position", strings("v.t0*16", "v.t1*16", "-v.t2*16")));
        // Java's ItemDisplayRenderer adds Y=180 AFTER the display TRSR and BEFORE ItemTransform.
        bones.add("item_flip", property("rotation", array(0, 180, 0)));
        bones.add("scale", property("scale", strings("v.t3", "v.t4", "v.t5")));
        for (String q : List.of("l", "r")) for (int axis = 0; axis < 3; axis++) {
            JsonArray rotation = array(0, 0, 0);
            rotation.set(axis, new JsonPrimitive((axis == 0 ? "" : "-") + "v." + q + "e" + "xyz".charAt(axis)));
            bones.add(q + "xyz".charAt(axis), property("rotation", rotation));
        }
        JsonObject pose = new JsonObject(); pose.addProperty("loop", true); pose.add("bones", bones);
        put(files, "animations/twilight_display.json", property("animations", property("animation.twilight.display_pose", pose), "1.10.0"));
    }

    static String quaternionScript(String q, int offset) {
        StringBuilder script = new StringBuilder();
        for (int i = 0; i < 4; i++) for (String end : List.of("a", "b")) script.append("v.").append(q).append(end).append(i)
                .append("=q.property('twilight:").append(end).append(offset + i).append("');");
        script.append("v.dot=");
        for (int i = 0; i < 4; i++) { if (i > 0) script.append('+'); script.append("v.").append(q).append('a').append(i).append("*v.").append(q).append('b').append(i); }
        script.append(";v.sign=v.dot<0?-1:1;v.dot=math.clamp(math.abs(v.dot),0,1);v.angle=math.acos(v.dot);v.den=math.max(math.sin(v.angle),0.000001);")
                .append("v.wa=v.dot>0.9995?1-v.p:math.sin((1-v.p)*v.angle)/v.den;v.wb=(v.dot>0.9995?v.p:math.sin(v.p*v.angle)/v.den)*v.sign;");
        for (int i = 0; i < 4; i++) script.append("v.").append(q).append("xyzw".charAt(i)).append("=v.wa*v.").append(q).append('a').append(i).append("+v.wb*v.").append(q).append('b').append(i).append(';');
        String x="v."+q+"x", y="v."+q+"y", z="v."+q+"z", w="v."+q+"w";
        script.append("v.n=math.max(math.sqrt(").append(x).append('*').append(x).append('+').append(y).append('*').append(y).append('+').append(z).append('*').append(z).append('+').append(w).append('*').append(w).append("),0.000001);");
        for (String v : List.of(x,y,z,w)) script.append(v).append('=').append(v).append("/v.n;");
        // At +/-90 degrees, X and Z are not individually determined. Keep X=0
        // and recover the combined Z angle from the remaining matrix entries.
        script.append("v.sy=math.clamp(2*(").append(w).append('*').append(y).append('-').append(z).append('*').append(x).append("),-1,1);")
                .append("v.singular=math.abs(v.sy)>0.9999999;")
                .append("v.").append(q).append("ex=v.singular?0:math.atan2(2*(").append(w).append('*').append(x).append('+').append(y).append('*').append(z).append("),1-2*(").append(x).append('*').append(x).append('+').append(y).append('*').append(y).append("));")
                .append("v.").append(q).append("ey=math.asin(v.sy);")
                .append("v.").append(q).append("ez=v.singular?math.atan2(2*(").append(w).append('*').append(z).append('-').append(x).append('*').append(y).append("),1-2*(").append(x).append('*').append(x).append('+').append(z).append('*').append(z).append(")):")
                .append("math.atan2(2*(").append(w).append('*').append(z).append('+').append(x).append('*').append(y).append("),1-2*(").append(y).append('*').append(y).append('+').append(z).append('*').append(z).append("));");
        return script.toString();
    }
    private static JsonArray array(double... numbers) { JsonArray a=new JsonArray(); for(double n:numbers)a.add(n); return a; }
    private static JsonArray strings(String... values) { JsonArray a=new JsonArray(); for(String v:values)a.add(v); return a; }
    private static JsonObject stringProperty(String key,String value) { JsonObject o=new JsonObject();o.addProperty(key,value);return o; }
    private static JsonObject property(String key,JsonElement value) { JsonObject o=new JsonObject();o.add(key,value);return o; }
    private static JsonObject property(String key,JsonElement value,String version) { JsonObject o=property(key,value);o.addProperty("format_version",version);return o; }
    private static void put(Map<String,byte[]> files,String path,JsonObject value) { files.put(path,JSON.toJson(value).getBytes(StandardCharsets.UTF_8)); }
}
