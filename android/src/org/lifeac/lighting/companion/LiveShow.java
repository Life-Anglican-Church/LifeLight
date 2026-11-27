package org.lifeac.lighting.companion;

import android.graphics.Color;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

final class LiveShow {
    final JSONObject state;
    final JSONArray scenes, fixtures;
    final String[] folders, groups;

    LiveShow(JSONObject state) throws org.json.JSONException {
        this.state = state;
        scenes = state.getJSONArray("scenes");
        fixtures = state.getJSONArray("fixtures");
        JSONArray folderList = state.getJSONArray("folders");
        folders = new String[folderList.length()];
        for (int i = 0; i < folders.length; i++) folders[i] = folderList.optString(i);
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (int i = 0; i < fixtures.length(); i++) {
            String name = fixture(i).optString("group", "");
            if (!name.isEmpty()) names.add(name);
        }
        groups = names.toArray(new String[0]);
    }

    JSONObject fixture(int index) { return fixtures.optJSONObject(index); }
    JSONObject scene(int index) { return scenes.optJSONObject(index); }
    String fixtureId(int index) { return fixture(index).optString("id"); }
    int group(int index) { return Arrays.asList(groups).indexOf(fixture(index).optString("group", "")); }

    int[] cues(int folder) {
        if (folder < 0 || folder >= folders.length) return new int[0];
        ArrayList<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < scenes.length(); i++) if (folders[folder].equals(scene(i).optString("folder"))) indexes.add(i);
        return indexes.stream().mapToInt(i -> i).toArray();
    }

    int current() {
        String id = state.optString("sceneId", "");
        for (int i = 0; i < scenes.length(); i++) if (id.equals(scene(i).optString("id"))) return i;
        return -1;
    }

    boolean supports(int fixture, int channel) {
        JSONObject item = fixture(fixture);
        return channel == 1 ? item.optJSONArray("colour") != null : item.optJSONObject("levels").has(key(channel));
    }

    int value(int fixture, int channel) {
        JSONObject item = fixture(fixture);
        if (channel != 1) return item.optJSONObject("levels").optInt(key(channel));
        JSONArray rgb = item.optJSONArray("colour");
        return rgb == null ? Color.WHITE : Color.rgb(rgb.optInt(0), rgb.optInt(1), rgb.optInt(2));
    }

    static String key(int channel) {
        switch (channel) {
            case 0: return "dimmer:1";
            case 2: return "pan:1";
            case 3: return "tilt:1";
            default: throw new IllegalArgumentException("Unknown slider");
        }
    }

    boolean hasFootprints() {
        for (int i = 0; i < fixtures.length(); i++) if (fixture(i).optJSONObject("footprint") != null) return true;
        return false;
    }

    FootprintPreview.Footprint[] footprints() {
        FootprintPreview.Footprint[] result = new FootprintPreview.Footprint[fixtures.length()];
        for (int i = 0; i < result.length; i++) {
            JSONObject saved = fixture(i).optJSONObject("footprint");
            if (saved == null) continue;
            try {
                FootprintPreview.Footprint f = new FootprintPreview.Footprint(
                    FootprintPreview.Shape.valueOf(saved.getString("shape")),
                    FootprintPreview.Falloff.valueOf(saved.getString("falloff")),
                    (float) saved.getDouble("x"), (float) saved.getDouble("y"),
                    (float) saved.getDouble("width"), (float) saved.getDouble("height"),
                    (float) saved.getDouble("angle"));
                f.hotspotX = (float) saved.getDouble("hotspotX");
                f.hotspotY = (float) saved.getDouble("hotspotY");
                f.softness = (float) saved.getDouble("softness");
                result[i] = f;
            } catch (IllegalArgumentException | org.json.JSONException ignored) {
                // Ignore geometry from an unsupported host version.
            }
        }
        return result;
    }

    int footprintIntensity(int i) {
        JSONArray output = fixture(i).optJSONArray("footprintOutput");
        if (output == null) return 0;
        int peak = Math.max(output.optInt(0), Math.max(output.optInt(1), output.optInt(2)));
        return Math.round(peak * 100f / 255);
    }

    int footprintColour(int i) {
        JSONArray output = fixture(i).optJSONArray("footprintOutput");
        if (output == null) return Color.BLACK;
        int red = output.optInt(0), green = output.optInt(1), blue = output.optInt(2);
        int peak = Math.max(red, Math.max(green, blue));
        return peak == 0 ? Color.BLACK
            : Color.rgb(Math.round(red * 255f / peak), Math.round(green * 255f / peak), Math.round(blue * 255f / peak));
    }

    boolean ready() {
        String output = state.optString("output");
        return ("active".equals(output) || "simulation".equals(output)) && !state.optBoolean("preview");
    }
}
