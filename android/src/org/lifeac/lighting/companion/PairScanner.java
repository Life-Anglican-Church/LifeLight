package org.lifeac.lighting.companion;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

@SuppressWarnings("deprecation")
public final class PairScanner extends Activity implements SurfaceHolder.Callback {
    private Camera camera;
    private SurfaceHolder surface;
    private long lastFrame;
    private final QRCodeReader reader = new QRCodeReader();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        SurfaceView view = new SurfaceView(this);
        view.getHolder().addCallback(this);
        setContentView(view);
        Toast.makeText(this, "Point the camera at the pairing code on the desk", Toast.LENGTH_LONG).show();
    }

    @Override public void surfaceCreated(SurfaceHolder holder) { surface = holder; openCamera(); }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) { surface = null; closeCamera(); }
    @Override protected void onResume() { super.onResume(); openCamera(); }
    @Override protected void onPause() { closeCamera(); super.onPause(); }

    private void openCamera() {
        if (camera != null || surface == null) return;
        try {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
            Camera.Parameters parameters = camera.getParameters();
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(parameters);
            Camera.Size size = parameters.getPreviewSize();
            camera.setPreviewDisplay(surface);
            camera.setPreviewCallback((data, unused) -> scan(data, size));
            camera.startPreview();
        } catch (Exception error) {
            Toast.makeText(this, "Camera unavailable", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void scan(byte[] data, Camera.Size size) {
        if (data == null || android.os.SystemClock.elapsedRealtime() - lastFrame < 180) return;
        lastFrame = android.os.SystemClock.elapsedRealtime();
        PlanarYUVLuminanceSource luminance = new PlanarYUVLuminanceSource(data, size.width, size.height,
            0, 0, size.width, size.height, false);
        try {
            String payload = reader.decode(new BinaryBitmap(new HybridBinarizer(luminance))).getText();
            closeCamera();
            setResult(RESULT_OK, new Intent().putExtra("pairing", payload));
            finish();
        } catch (ReaderException ignored) {
            reader.reset();
        }
    }

    private void closeCamera() {
        if (camera == null) return;
        camera.setPreviewCallback(null);
        camera.release();
        camera = null;
    }
}
