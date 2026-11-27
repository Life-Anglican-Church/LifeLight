package org.lifeac.lighting.companion;

import android.content.Context;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import java.util.function.IntUnaryOperator;

/** The preview is purely visual. The fixture pins are separate views above it. */
final class FootprintPreview extends View {
    enum Shape { Ellipse, Rectangle, Triangle }
    enum Falloff { Radial, Linear }

    static final class Footprint {
        Shape shape;
        Falloff falloff;
        float x, y, width, height, angle, hotspotX = .5f, hotspotY = .5f, softness = .75f;

        Footprint(Shape shape, Falloff falloff, float x, float y, float width, float height, float angle) {
            this.shape = shape;
            this.falloff = falloff;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.angle = angle;
        }
    }

    static Footprint[] demo() {
        return new Footprint[] {
            new Footprint(Shape.Rectangle, Falloff.Linear, .17f, .72f, .23f, .25f, 0),
            new Footprint(Shape.Rectangle, Falloff.Linear, .84f, .73f, .23f, .25f, 0),
            null, null,
            new Footprint(Shape.Ellipse, Falloff.Radial, .44f, .80f, .37f, .25f, 0),
            new Footprint(Shape.Ellipse, Falloff.Radial, .60f, .80f, .37f, .25f, 0),
            new Footprint(Shape.Triangle, Falloff.Linear, .21f, .90f, .41f, .21f, -10),
            new Footprint(Shape.Triangle, Falloff.Linear, .80f, .90f, .41f, .21f, 10)
        };
    }

    private final Footprint[] footprints;
    private final IntUnaryOperator intensity, colour;
    private final float aspect;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path triangle = new Path();
    private final RectF bounds = new RectF(0, 0, 1, 1);

    FootprintPreview(Context context, Footprint[] footprints, IntUnaryOperator intensity,
                     IntUnaryOperator colour, float aspect) {
        super(context);
        this.footprints = footprints;
        this.intensity = intensity;
        this.colour = colour;
        this.aspect = aspect;
        paint.setBlendMode(BlendMode.SCREEN);
        triangle.moveTo(.5f, 0);
        triangle.lineTo(1, 1);
        triangle.lineTo(0, 1);
        triangle.close();
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = Math.max(getWidth(), getHeight() * aspect);
        float h = w / aspect;
        float left = (getWidth() - w) / 2;
        float top = (getHeight() - h) / 2;
        for (int i = 0; i < footprints.length; i++) {
            Footprint f = footprints[i];
            int dimmer = intensity.applyAsInt(i);
            if (f == null || dimmer <= 0) continue;
            int rgb = colour.applyAsInt(i) & 0x00ffffff;
            int peak = rgb | (Math.round(dimmer * 2.2f) << 24);
            int shoulder = rgb | (Math.round(dimmer * 1.2f) << 24);
            float spread = .08f + (1 - f.softness) * .72f;
            int[] colors = {peak, shoulder, rgb};
            float[] stops = {0, spread, 1};
            if (f.falloff == Falloff.Radial) {
                paint.setShader(new RadialGradient(f.hotspotX, f.hotspotY, .72f, colors, stops, Shader.TileMode.CLAMP));
            } else {
                // Local Y is the falloff direction. Rotating the footprint rotates the wash.
                paint.setShader(new LinearGradient(0, f.hotspotY, 0, f.hotspotY + Math.max(f.hotspotY, 1 - f.hotspotY),
                    colors, stops, Shader.TileMode.MIRROR));
            }
            int restore = canvas.save();
            canvas.translate(left + f.x * w, top + f.y * h);
            canvas.rotate(f.angle);
            canvas.scale(f.width * w, f.height * h);
            canvas.translate(-.5f, -.5f);
            switch (f.shape) {
                case Ellipse: canvas.drawOval(bounds, paint); break;
                case Rectangle: canvas.drawRect(bounds, paint); break;
                case Triangle: canvas.drawPath(triangle, paint); break;
            }
            canvas.restoreToCount(restore);
        }
    }
}
