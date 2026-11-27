package org.lifeac.lighting.companion;

import java.util.Arrays;

final class DemoShow {
    static final String[] FOLDERS = {"Sunday Services", "General"};
    static final String[] SCENES = {
        "Pre-Service", "Welcome", "Worship", "Message", "Rehearsal", "Stage Check", "Walk Out"
    };
    static final int[][] FOLDER_SCENES = {{0, 1, 2, 3}, {4, 5, 6}};
    static final String[] GROUPS = {"Stage Wash", "Moving Heads", "Floor Wash"};
    static final String[] FIXTURES = {
        "Fixture 1", "Fixture 2", "Fixture 3", "Fixture 4", "Fixture 5", "Fixture 6",
        "Fixture 7", "Fixture 8"
    };
    private static final int[][] LEVELS = {
        {55, 55, 45, 45, 80, 80, 30, 30},
        {75, 75, 60, 60, 60, 60, 40, 40},
        {50, 50, 75, 75, 70, 70, 60, 60},
        {65, 65, 20, 20, 45, 45, 25, 25},
        {85, 85, 30, 30, 70, 70, 40, 40},
        {100, 100, 65, 65, 100, 100, 65, 65},
        {35, 35, 0, 0, 35, 35, 55, 55}
    };
    private static final int[] COLORS = {0xff6b78ff, 0xffffc18b, 0xffa875ff, 0xffffd4ac, 0xffeff4ff, 0xfffff5e3, 0xff68c9df};
    private final Integer[][] held = new Integer[FIXTURES.length][4];
    int scene;
    String source = "Demo startup";

    static int group(int fixture) {
        if (fixture == 2 || fixture == 3) return 1;
        return fixture >= 6 ? 2 : 0;
    }

    static boolean supports(int fixture, int channel) {
        return channel < 2 || group(fixture) == 1;
    }

    void cue(int index, String from) {
        scene = index;
        source = from;
    }

    int nextCue() {
        for (int[] folder : FOLDER_SCENES) {
            for (int i = 0; i < folder.length; i++) {
                if (folder[i] == scene) return folder[(i + 1) % folder.length];
            }
        }
        throw new IllegalStateException("Live scene has no folder");
    }

    int value(int fixture, int channel) {
        if (held[fixture][channel] != null) return held[fixture][channel];
        switch (channel) {
            case 0: return LEVELS[scene][fixture];
            case 1: return COLORS[scene];
            case 2: return scene == 2 ? 65 : 50;
            default: return scene == 2 ? 35 : 50;
        }
    }

    void adjust(int fixture, int channel, int value) {
        if (supports(fixture, channel)) held[fixture][channel] = value;
    }

    boolean isHeld(int fixture) {
        for (Integer value : held[fixture]) if (value != null) return true;
        return false;
    }

    void release(int fixture) {
        Arrays.fill(held[fixture], null);
    }
}
