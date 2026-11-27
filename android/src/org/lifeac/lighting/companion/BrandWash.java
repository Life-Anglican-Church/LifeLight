package org.lifeac.lighting.companion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Cyan and violet spill, with a dark centre for the LifeLIGHT wordmark. */
final class BrandWash extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Shader cyan = new RadialGradient(.06f, .98f, .54f,
        0xaf008fa9, 0x00008fa9, Shader.TileMode.CLAMP);
    private final Shader violet = new RadialGradient(.93f, .06f, .56f,
        0xc77721a6, 0x007721a6, Shader.TileMode.CLAMP);

    BrandWash(Context context) {
        super(context);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xff070c19);
        int save = canvas.save();
        canvas.scale(getWidth(), getHeight());
        paint.setShader(cyan);
        canvas.drawRect(0, 0, 1, 1, paint);
        paint.setShader(violet);
        canvas.drawRect(0, 0, 1, 1, paint);
        canvas.restoreToCount(save);
    }
}
