package org.lifeac.lighting.companion;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

final class ConsoleIcon extends Drawable {
    enum Shape { SCENES, FIXTURE, FOLDER, QR, THEME, RELEASE, LINK }

    private final Shape shape;
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);

    ConsoleIcon(Shape shape, int color, int size) {
        this(shape, color, size, 1.5f);
    }

    ConsoleIcon(Shape shape, int color, int size, float strokeWidth) {
        this.shape = shape;
        ink.setColor(color);
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeWidth(strokeWidth);
        ink.setStrokeCap(Paint.Cap.ROUND);
        ink.setStrokeJoin(Paint.Join.ROUND);
        setBounds(0, 0, size, size);
    }

    @Override public void draw(Canvas canvas) {
        int saved = canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
        switch (shape) {
            case SCENES:
                for (int x : new int[]{3, 13}) for (int y : new int[]{3, 13})
                    canvas.drawRoundRect(x, y, x + 7, y + 7, 1.2f, 1.2f, ink);
                break;
            case FIXTURE:
                canvas.save();
                canvas.rotate(-28, 12, 9);
                canvas.drawRoundRect(7, 4, 17, 12, 1.5f, 1.5f, ink);
                canvas.drawLine(9, 15, 6, 19, ink);
                canvas.drawLine(12, 15, 12, 21, ink);
                canvas.drawLine(15, 15, 18, 19, ink);
                canvas.restore();
                break;
            case FOLDER:
                line(canvas, 3, 18, 3, 6, 10, 6, 12, 8, 21, 8, 21, 18, 3, 18);
                break;
            case QR:
                canvas.drawRect(3, 3, 9, 9, ink);
                canvas.drawRect(15, 3, 21, 9, ink);
                canvas.drawRect(3, 15, 9, 21, ink);
                line(canvas, 14, 14, 18, 14, 18, 18, 21, 18, 21, 21);
                canvas.drawLine(14, 18, 14, 21, ink);
                break;
            case THEME:
                canvas.drawCircle(12, 12, 4, ink);
                for (int i = 0; i < 8; i++) {
                    canvas.drawLine(12, 3, 12, 5, ink);
                    canvas.rotate(45, 12, 12);
                }
                break;
            case RELEASE:
                canvas.drawArc(5, 5, 20, 20, -135, 292, false, ink);
                line(canvas, 5, 4, 5, 9, 10, 9);
                break;
            case LINK:
                canvas.drawArc(3, 5, 21, 20, 222, 96, false, ink);
                canvas.drawArc(6, 9, 18, 20, 222, 96, false, ink);
                canvas.drawCircle(12, 16, 1, ink);
                break;
        }
        canvas.restoreToCount(saved);
    }

    private void line(Canvas canvas, float... points) {
        Path path = new Path();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        canvas.drawPath(path, ink);
    }

    @Override public void setAlpha(int alpha) { ink.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { ink.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
