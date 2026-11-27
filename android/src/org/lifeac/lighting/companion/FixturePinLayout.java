package org.lifeac.lighting.companion;

import java.util.ArrayList;
import java.util.List;

final class FixturePinLayout {
    static final class Point {
        final float x, y;
        Point(float x, float y) { this.x = x; this.y = y; }
    }

    private final float maxX, maxY, pitch;
    private final List<Point> placed = new ArrayList<>();

    FixturePinLayout(int width, int height, int pinSize, int gap) {
        maxX = Math.max(0, width - pinSize);
        maxY = Math.max(0, height - pinSize);
        pitch = pinSize + gap;
    }

    Point place(float x, float y) {
        Point anchor = new Point(Math.max(0, Math.min(x, maxX)), Math.max(0, Math.min(y, maxY)));
        Point best = anchor;
        if (!available(anchor.x, anchor.y)) {
            float bestDistance = Float.POSITIVE_INFINITY;
            for (float cy = 0; cy <= maxY; cy += pitch) {
                for (float cx = 0; cx <= maxX; cx += pitch) {
                    float dx = cx - anchor.x, dy = cy - anchor.y;
                    float distance = dx * dx + dy * dy;
                    if (distance < bestDistance && available(cx, cy)) {
                        best = new Point(cx, cy);
                        bestDistance = distance;
                    }
                }
            }
        }
        placed.add(best);
        return best;
    }

    private boolean available(float x, float y) {
        for (Point pin : placed) {
            if (Math.abs(x - pin.x) < pitch && Math.abs(y - pin.y) < pitch) return false;
        }
        return true;
    }
}
