package org.lifeac.lighting.companion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final String[] THEMES = {"Dark", "Light", "System"};
    private static final int[] DEMO_ACCENTS = {
        0xff777de3, 0xffe4ad57, 0xffb970ca, 0xff709ac6, 0xffce849e, 0xff62a693, 0xff6a9cbc
    };
    // The desktop scene library derives these accents from the scene ID.
    private static final int[] DESK_ACCENTS = {
        0xffedbb61, 0xffaa79e0, 0xff22c5e9, 0xff5ac493, 0xffdf76ba, 0xfff08e6c
    };
    private int backdrop, surface, divider, accent, muted, warning, ink;
    private boolean light;
    private int themeChoice;
    private static final int[] PALETTE = {
        0xffffdfb1, 0xfffba173, 0xffff7389, 0xffc17aff, 0xff738fff, 0xff69d4fc, 0xff6be4b1, 0xfff4f7ff
    };
    private static final String[] COLOR_NAMES = {
        "Warm white", "Amber", "Rose", "Violet", "Blue", "Cyan", "Mint", "Cool white"
    };
    private static final int FIXTURE_PIN_DP = 20;
    private static final float[][] POSITIONS = {
        {.20f,.28f}, {.79f,.28f}, {.36f,.37f}, {.64f,.37f},
        {.36f,.57f}, {.64f,.57f}, {.22f,.75f}, {.78f,.75f}
    };

    private final DemoShow show = new DemoShow();
    private final FootprintPreview.Footprint[] demoFootprints = FootprintPreview.demo();
    private FootprintPreview stageFootprints;
    private boolean footprintsVisible = true;
    private TextView[] markers;
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private LiveHost host;
    private LiveShow live;
    private boolean liveMode, busy, connected, resumed, touching, touchCancelled, redraw;
    private long updatedAt;
    private int foregroundEpoch, renderRevision;
    private JSONObject pendingCommand;
    private boolean commandInFlight;
    private String connectionMessage = "";
    private ScrollView portraitScroll;
    private final Runnable refreshHost = this::pollHost;
    private final Set<Integer> selectedFixtures = new LinkedHashSet<>();
    private int sceneFolder;
    private int group = -1;
    private boolean started, pairingInProgress, connectionFailed, revealConsole;
    private boolean fixturesPage;
    private TextView status, statusScene, statusDetails, selectionStatus;
    private LinearLayout statusCard;
    private Button release, releaseAll;
    private Drawable photo;
    private PilotUpdate pendingUpdate;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        themeChoice = Math.max(0, Math.min(2, getPreferences(MODE_PRIVATE).getInt("appearance", 0)));
        footprintsVisible = getPreferences(MODE_PRIVATE).getBoolean("footprints-visible", true);
        try (InputStream stream = getAssets().open("stage.jpg")) {
            photo = Drawable.createFromStream(stream, null);
        } catch (IOException ignored) {
            // Fixture controls remain available if the stage asset is missing.
        }
        try { host = LiveHost.load(this); } catch (Exception error) { LiveHost.forget(this); }
        if (host != null) enterLive();
        else render();
        checkForPilotUpdate();
    }

    private void checkForPilotUpdate() {
        Thread check = new Thread(() -> {
            try {
                long version = getPackageManager().getPackageInfo(getPackageName(), 0).getLongVersionCode();
                PilotUpdate update = PilotUpdate.check(version);
                if (update != null) ui.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    pendingUpdate = update;
                    offerPilotUpdate();
                });
            } catch (Exception ignored) {
                // Startup and local control are available without internet access.
            }
        }, "LifeLIGHT update check");
        check.setDaemon(true);
        check.start();
    }

    private void offerPilotUpdate() {
        if (pendingUpdate == null || !resumed || !hasWindowFocus() || isFinishing()) return;
        PilotUpdate update = pendingUpdate;
        pendingUpdate = null;
        dialog().setTitle("LifeLIGHT " + update.version + " is available")
            .setMessage("Download the APK from GitHub and open it to install the update.")
            .setNegativeButton("Later", null)
            .setPositiveButton("Download APK", (window, which) -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(update.downloadUrl)));
                } catch (android.content.ActivityNotFoundException error) {
                    Toast.makeText(this, "Open GitHub in a browser to download the APK.", Toast.LENGTH_LONG).show();
                }
            }).show();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) offerPilotUpdate();
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        if (liveMode) {
            connected = false;
            render();
            schedulePoll(0);
        }
        offerPilotUpdate();
    }

    @Override protected void onPause() {
        resumed = false;
        foregroundEpoch++;
        pendingCommand = null;
        connected = false;
        touching = false;
        ui.removeCallbacks(refreshHost);
        super.onPause();
    }

    @Override protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        network.shutdownNow();
        super.onDestroy();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            touching = true;
            touchCancelled = false;
        }
        if (action == MotionEvent.ACTION_CANCEL) touchCancelled = true;
        boolean result = super.dispatchTouchEvent(e);
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            touching = false;
            if (redraw) ui.post(() -> { if (redraw && resumed) render(); });
        }
        return result;
    }

    private void scanPairing() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 10);
        } else startActivityForResult(new Intent(this, PairScanner.class), 11);
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == 10 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) scanPairing();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 11 || result != RESULT_OK || data == null) return;
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setText(Build.MODEL);
        name.setSelectAllOnFocus(true);
        dialog().setTitle("Name this device").setView(name)
            .setPositiveButton("Pair", (dialog, which) -> pair(data.getStringExtra("pairing"), name.getText().toString().trim()))
            .setNegativeButton("Cancel", null).show();
    }

    private void pair(String payload, String name) {
        if (busy) return;
        busy = true;
        pairingInProgress = true;
        render();
        network.execute(() -> {
            try {
                LiveHost pair = LiveHost.pair(getApplicationContext(), new JSONObject(payload), name);
                ui.post(() -> {
                    if (isDestroyed()) return;
                    busy = false;
                    pairingInProgress = false;
                    host = pair;
                    enterLive();
                });
            } catch (Exception error) {
                ui.post(() -> {
                    busy = false;
                    pairingInProgress = false;
                    if (!isDestroyed()) {
                        render();
                        Toast.makeText(this, "Pairing failed. Check Wi-Fi and scan a fresh code at the desk.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void enterLive() {
        started = true;
        liveMode = true;
        live = null;
        connected = false;
        connectionMessage = "Connecting to desk…";
        connectionFailed = false;
        revealConsole = true;
        sceneFolder = 0;
        clearFixtureSelection();
        render();
        schedulePoll(0);
    }

    private void leaveLive() {
        liveMode = false;
        foregroundEpoch++;
        pendingCommand = null;
        started = false;
        revealConsole = false;
        connected = false;
        live = null;
        ui.removeCallbacks(refreshHost);
        render();
    }

    private boolean canControl() {
        return !liveMode || connected && live != null && live.ready() && resumed
            && SystemClock.elapsedRealtime() - updatedAt < 4500;
    }

    private void schedulePoll(long delay) {
        ui.removeCallbacks(refreshHost);
        if (resumed && liveMode) ui.postDelayed(refreshHost, delay);
    }

    private void pollHost() {
        if (!resumed || !liveMode || host == null) return;
        if (busy) { schedulePoll(300); return; }
        if (connected && SystemClock.elapsedRealtime() - updatedAt >= 4500) {
            connected = false;
            redrawWhenReady();
        }
        exchange("state", null);
    }

    private void exchange(String path, JSONObject body) {
        final LiveHost target = host;
        final int epoch = foregroundEpoch;
        commandInFlight = body != null;
        busy = true;
        network.execute(() -> {
            try {
                LiveShow snapshot = new LiveShow(target.request(path, body));
                ui.post(() -> {
                    busy = false;
                    commandInFlight = false;
                    if (isDestroyed() || !liveMode || host != target) return;
                    if (epoch != foregroundEpoch) { schedulePoll(0); return; }
                    boolean changed = !connected || live == null || !live.state.toString().equals(snapshot.state.toString());
                    boolean firstSnapshot = live == null;
                    boolean newPatch = firstSnapshot || live.state.optInt("revision") != snapshot.state.optInt("revision");
                    live = snapshot;
                    connected = resumed;
                    connectionFailed = false;
                    updatedAt = SystemClock.elapsedRealtime();
                    connectionMessage = connected ? "" : "Connecting…";
                    if (newPatch) {
                        clearFixtureSelection();
                        if (firstSnapshot && fixtureCount() > 0) selectedFixtures.add(0);
                        sceneFolder = Math.min(sceneFolder, Math.max(0, live.folders.length - 1));
                    }
                    if (changed || body != null) redrawWhenReady();
                    JSONObject pending = pendingCommand;
                    pendingCommand = null;
                    if (pending != null && canControl()
                        && pending.optInt("revision") == snapshot.state.optInt("revision")) {
                        exchange("command", pending);
                    } else schedulePoll(850);
                });
            } catch (Exception error) {
                ui.post(() -> {
                    busy = false;
                    commandInFlight = false;
                    if (isDestroyed() || !liveMode || host != target) return;
                    if (epoch != foregroundEpoch) { schedulePoll(0); return; }
                    connected = false;
                    if (error instanceof LiveHost.ResponseError && ((LiveHost.ResponseError) error).status == 401) {
                        LiveHost.forget(this);
                        host = null;
                        leaveLive();
                        Toast.makeText(this, "Device approval ended. Pair again at the desk.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    pendingCommand = null;
                    connectionFailed = true;
                    connectionMessage = error instanceof javax.net.ssl.SSLException
                        ? "Could not verify desk · pair again"
                        : "Disconnected · checking host";
                    if (body != null) Toast.makeText(this, error instanceof LiveHost.ResponseError
                        ? error.getMessage() : "Command not confirmed. Check the live scene before retrying.", Toast.LENGTH_LONG).show();
                    redrawWhenReady();
                    schedulePoll(1200);
                });
            }
        });
    }

    private void redrawWhenReady() {
        redraw = true;
        if (!touching) render();
    }

    private void send(String op, Integer channel, Integer value, Integer scene) {
        send(op, channel, value, scene, false);
    }

    private void send(String op, Integer channel, Integer value, Integer scene, boolean allFixtures) {
        if (!canControl() || renderRevision != live.state.optInt("revision")) return;
        if (commandInFlight || pendingCommand != null) {
            Toast.makeText(this, "Wait for the desk to confirm the previous action.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject body = new JSONObject().put("op", op).put("revision", live.state.getInt("revision"));
            if (scene != null) body.put("sceneId", live.scene(scene).getString("id"));
            else {
                JSONArray ids = new JSONArray();
                for (int i = 0; i < fixtureCount(); i++) if (allFixtures || selected(i)) ids.put(live.fixtureId(i));
                body.put("ids", ids);
            }
            if (channel != null && channel == 1) {
                body.put("rgb", new JSONArray().put(Color.red(value)).put(Color.green(value)).put(Color.blue(value)));
            } else if (channel != null) {
                body.put("key", LiveShow.key(channel)).put("value", value);
            }
            if (busy) pendingCommand = body;
            else exchange("command", body);
        } catch (org.json.JSONException error) {
            Toast.makeText(this, "Control unavailable. Try refreshing.", Toast.LENGTH_SHORT).show();
        }
    }

    private int fixtureCount() { return liveMode ? live == null ? 0 : live.fixtures.length() : DemoShow.FIXTURES.length; }
    private String fixtureName(int i) { return liveMode ? live.fixture(i).optString("name") : DemoShow.FIXTURES[i]; }
    private String[] groups() { return liveMode ? live.groups : DemoShow.GROUPS; }
    private String[] folders() { return liveMode ? live.folders : DemoShow.FOLDERS; }
    private int currentScene() { return liveMode ? live.current() : show.scene; }
    private String sceneName(int i) { return liveMode ? live.scene(i).optString("name") : DemoShow.SCENES[i]; }
    private boolean isHeld(int i) { return liveMode ? live.fixture(i).optBoolean("held") : show.isHeld(i); }
    private int value(int i, int channel) { return liveMode ? live.value(i, channel) : show.value(i, channel); }
    private boolean supports(int channel) {
        boolean found = false;
        for (int i = 0; i < fixtureCount(); i++) if (selected(i)) {
            if (!(liveMode ? live.supports(i, channel) : DemoShow.supports(i, channel))) return false;
            found = true;
        }
        return found;
    }

    @Override public void onBackPressed() {
        if (!started) {
            super.onBackPressed();
        } else if (fixturesPage && !wideConsole()) {
            fixturesPage = false;
            portraitScroll = null;
            render();
        } else {
            leaveLive();
        }
    }

    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        render();
    }

    private int dp(float n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable background(int fill, int border, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (border != Color.TRANSPARENT) shape.setStroke(Math.max(1, dp(.65f)), border);
        return shape;
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private LinearLayout row() {
        LinearLayout view = new LinearLayout(this);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView text(String label, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private Button button(String label, boolean selected, Runnable action) {
        return styleButton(new Button(this), label, selected, action);
    }

    private Button styleButton(Button view, String label, boolean selected, Runnable action) {
        view.setText(label);
        view.setAllCaps(false);
        view.setTextColor(selected ? accent : ink);
        view.setTextSize(15);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setLetterSpacing(0f);
        view.setMinHeight(dp(48));
        view.setPadding(dp(7), dp(5), dp(7), dp(5));
        setButtonSurface(view, selected ? blend(surface, accent, .14f) : surface, 9);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private void updateTheme() {
        light = themeChoice == 1 || themeChoice == 2
            && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                != Configuration.UI_MODE_NIGHT_YES;
        backdrop = light ? 0xfff5f2eb : 0xff101013;
        surface = light ? 0xfffdfaf4 : 0xff1b1a20;
        divider = light ? 0xffcbc4b8 : 0xff48434e;
        ink = light ? 0xff26232b : 0xfff7f3fa;
        muted = light ? 0xff595361 : 0xffc3bacb;
        accent = light ? 0xff613779 : 0xffe2b9ff;
        warning = light ? 0xff783e13 : 0xffffcb92;
        getWindow().setStatusBarColor(backdrop);
        getWindow().setNavigationBarColor(backdrop);
        getWindow().getDecorView().setSystemUiVisibility(light
            ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
    }

    private AlertDialog.Builder dialog() {
        return new AlertDialog.Builder(this, light
            ? android.R.style.Theme_Material_Light_Dialog_Alert
            : android.R.style.Theme_Material_Dialog_Alert);
    }

    private void chooseTheme() {
        dialog().setTitle("Appearance").setSingleChoiceItems(THEMES, themeChoice, (dialog, selected) -> {
            themeChoice = selected;
            getPreferences(MODE_PRIVATE).edit().putInt("appearance", selected).apply();
            dialog.dismiss();
            render();
        }).setNegativeButton("Cancel", null).show();
    }

    private Button withIcon(Button button, ConsoleIcon.Shape shape) {
        button.setCompoundDrawablePadding(dp(7));
        button.setCompoundDrawablesRelative(new ConsoleIcon(shape,
            button.getCurrentTextColor(), dp(17)), null, null, null);
        return button;
    }

    private int blend(int base, int tint, float amount) {
        return Color.rgb(Math.round(Color.red(base) * (1 - amount) + Color.red(tint) * amount),
            Math.round(Color.green(base) * (1 - amount) + Color.green(tint) * amount),
            Math.round(Color.blue(base) * (1 - amount) + Color.blue(tint) * amount));
    }

    private int sceneTint(int index) {
        if (!liveMode) return DEMO_ACCENTS[index % DEMO_ACCENTS.length];
        String id = live.scene(index).optString("id");
        int hash = 0;
        for (int i = 0; i < id.length();) {
            int point = id.codePointAt(i);
            hash = hash * 31 + id.charAt(i);
            i += Character.charCount(point);
        }
        return DESK_ACCENTS[Integer.remainderUnsigned(hash, DESK_ACCENTS.length)];
    }

    private void setButtonSurface(Button view, int fill, int radius) {
        GradientDrawable shape = background(fill, Color.TRANSPARENT, radius);
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(light ? 0x22000000 : 0x2affffff), shape, null));
    }

    private GradientDrawable tintedCard(int start, int end) {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[] {start, end});
        shape.setCornerRadius(dp(10));
        return shape;
    }

    private void inset(View view, int horizontal, int vertical) {
        view.setPadding(dp(horizontal), dp(vertical), dp(horizontal), dp(vertical));
    }

    private void addLiveCard(LinearLayout root, boolean compact) {
        statusCard = column();
        inset(statusCard, 15, compact ? 8 : 12);
        status = text("", 12, accent, true);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        statusCard.addView(status);
        statusScene = text("", compact ? 21 : 26, ink, true);
        statusScene.setAccessibilityHeading(true);
        statusCard.addView(statusScene);
        statusDetails = text("", 13, muted, false);
        statusCard.addView(statusDetails);
        block(root, statusCard);
    }

    private boolean wideConsole() {
        Configuration config = getResources().getConfiguration();
        return config.screenWidthDp >= 720 && !portrait();
    }

    private boolean portrait() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private void block(LinearLayout parent, View child) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(8);
        parent.addView(child, params);
    }

    private void share(LinearLayout parent, View child, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, weight);
        params.rightMargin = dp(5);
        parent.addView(child, params);
    }

    private ScrollView scroll(LinearLayout content) {
        ScrollView view = new ScrollView(this);
        view.setFillViewport(true);
        view.addView(content);
        return view;
    }

    private ImageView wordmark() {
        ImageView view = new ImageView(this);
        view.setImageResource(getResources().getIdentifier("lifelight_wordmark", "drawable", getPackageName()));
        view.setContentDescription("LifeLIGHT");
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return view;
    }

    private void addHeaderBrand(LinearLayout header) {
        FrameLayout brandSlot = new FrameLayout(this);
        header.addView(brandSlot, new LinearLayout.LayoutParams(0, dp(48), 1));
        ImageView mark = wordmark();
        inset(mark, 8, 3);
        if (light) mark.setBackground(background(0xff16131b, Color.TRANSPARENT, 7));
        int available = getResources().getConfiguration().screenWidthDp - (liveMode ? 148 : 166);
        int width = Math.max(92, Math.min(152, available));
        brandSlot.addView(mark, new FrameLayout.LayoutParams(dp(width), -1));
    }

    private void renderEntry(LinearLayout root, LinearLayout header) {
        getWindow().setStatusBarColor(0xff080d1b);
        getWindow().setNavigationBarColor(0xff080d1b);
        getWindow().getDecorView().setSystemUiVisibility(0);

        FrameLayout screen = new FrameLayout(this);
        screen.addView(new BrandWash(this), new FrameLayout.LayoutParams(-1, -1));
        LinearLayout content = column();
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams headerSize = new LinearLayout.LayoutParams(-1, -2);
        headerSize.topMargin = dp(8);
        content.addView(header, headerSize);
        content.addView(new Space(this), new LinearLayout.LayoutParams(1, dp(14), 3));
        ImageView mark = wordmark();
        int screenWidth = getResources().getConfiguration().screenWidthDp;
        int width = Math.min(420, screenWidth - 40);
        content.addView(mark, new LinearLayout.LayoutParams(dp(width), dp(portrait() ? 142 : 108)));
        content.addView(new Space(this), new LinearLayout.LayoutParams(1, dp(18), 2));

        LinearLayout actions = column();
        if (pairingInProgress || liveMode) {
            TextView message = text(pairingInProgress ? "Pairing with desk…" : connectionMessage, 18, Color.WHITE, true);
            message.setGravity(Gravity.CENTER);
            message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            block(actions, message);
            if (liveMode && connectionFailed) {
                block(actions, entryButton("Retry", ConsoleIcon.Shape.LINK, true, () -> {
                    connectionFailed = false;
                    connectionMessage = "Connecting to desk…";
                    render();
                    schedulePoll(0);
                }));
            }
        } else {
            block(actions, entryButton(host == null ? "Pair at the desk" : "Connect to desk",
                host == null ? ConsoleIcon.Shape.QR : ConsoleIcon.Shape.LINK, true,
                () -> { if (host == null) scanPairing(); else enterLive(); }));
            block(actions, entryButton("Try demo", ConsoleIcon.Shape.SCENES, false, () -> {
                sceneFolder = 0;
                clearFixtureSelection();
                selectedFixtures.add(4);
                revealConsole = true;
                started = true;
                render();
            }));
            if (host != null) {
                Button repair = button("Pair a different desk", false, this::scanPairing);
                repair.setTextColor(0xffe5d9f6);
                setButtonSurface(repair, Color.TRANSPARENT, 9);
                block(actions, repair);
            }
        }
        int actionWidth = Math.min(260, Math.round(screenWidth * .72f));
        content.addView(actions, new LinearLayout.LayoutParams(dp(actionWidth), -2));
        content.addView(new Space(this), new LinearLayout.LayoutParams(1, dp(16), 1));
        screen.addView(scroll(content), new FrameLayout.LayoutParams(-1, -1));
        root.addView(screen, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private Button entryButton(String label, ConsoleIcon.Shape icon, boolean primary, Runnable action) {
        Button button = button(label, false, action);
        button.setTextColor(primary ? 0xfff9f2ff : 0xffe5eafa);
        button.setTextSize(15);
        button.setMinHeight(dp(58));
        inset(button, 12, 8);
        setButtonSurface(button, primary ? 0xff35204d : 0xff192337, 12);
        return withIcon(button, icon);
    }

    private void render() {
        updateTheme();
        redraw = false;
        int scrollY = portraitScroll == null ? 0 : portraitScroll.getScrollY();
        portraitScroll = null;
        status = null;
        selectionStatus = null;
        release = null;
        releaseAll = null;
        stageFootprints = null;
        markers = new TextView[fixtureCount()];
        renderRevision = live == null ? -1 : live.state.optInt("revision");
        LinearLayout root = column();
        root.setBackgroundColor(backdrop);
        boolean entry = !started || liveMode && live == null;
        if (!entry) root.setPadding(dp(16), dp(8), dp(16), dp(8));
        setContentView(root);
        LinearLayout header = row();
        if (entry) {
            inset(header, 16, 0);
            header.addView(new Space(this), new LinearLayout.LayoutParams(0, 0, 1));
        } else {
            addHeaderBrand(header);
            if (!light) header.setBackground(tintedCard(backdrop, blend(backdrop, accent, .09f)));
        }
        if (liveMode) {
            Button connections = withIcon(button("", false, this::leaveLive), ConsoleIcon.Shape.LINK);
            connections.setContentDescription("Connections");
            if (entry) {
                connections.setTextColor(Color.WHITE);
                setButtonSurface(connections, 0xff192337, 10);
                withIcon(connections, ConsoleIcon.Shape.LINK);
            }
            inset(connections, 14, 5);
            header.addView(connections, new LinearLayout.LayoutParams(dp(48), -2));
        }
        else if (started) {
            TextView demo = text("Demo", 13, accent, true);
            inset(demo, 11, 5);
            demo.setBackground(background(blend(surface, accent, .14f), Color.TRANSPARENT, 18));
            header.addView(demo);
        }
        Button appearance = withIcon(button("", false, this::chooseTheme), ConsoleIcon.Shape.THEME);
        appearance.setContentDescription("Appearance, " + THEMES[themeChoice]);
        if (entry) {
            appearance.setTextColor(Color.WHITE);
            setButtonSurface(appearance, 0xff192337, 10);
            withIcon(appearance, ConsoleIcon.Shape.THEME);
        }
        inset(appearance, 14, 5);
        LinearLayout.LayoutParams appearanceSize = new LinearLayout.LayoutParams(dp(48), -2);
        appearanceSize.leftMargin = dp(6);
        header.addView(appearance, appearanceSize);
        if (entry) {
            renderEntry(root, header);
            return;
        }
        block(root, header);
        if (revealConsole) {
            revealConsole = false;
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
                root.setAlpha(.28f);
                root.animate().alpha(1).setDuration(160).start();
            }
        }
        addLiveCard(root, wideConsole());
        if (portrait()) addReleaseAll(root);
        if (wideConsole()) {
            LinearLayout desk = row();
            desk.setGravity(Gravity.TOP);
            pane(desk, cuePanel(false), .77f);
            pane(desk, stagePanel(), 1.42f);
            pane(desk, inspector(), 1.05f);
            root.addView(desk, new LinearLayout.LayoutParams(-1, 0, 1));
        } else {
            LinearLayout page = column();
            if (fixturesPage) {
                block(page, stagePanel());
                block(page, inspector());
            } else {
                block(page, cuePanel(portrait()));
            }
            portraitScroll = scroll(page);
            ScrollView currentScroll = portraitScroll;
            root.addView(currentScroll, new LinearLayout.LayoutParams(-1, 0, 1));
            currentScroll.post(() -> currentScroll.scrollTo(0, scrollY));
            LinearLayout nav = row();
            inset(nav, 4, 4);
            for (boolean fixtures : new boolean[]{false, true}) {
                Button tab = navigationTab(fixtures);
                LinearLayout.LayoutParams tabSize = new LinearLayout.LayoutParams(0, -2, 1);
                if (!fixtures) tabSize.rightMargin = dp(4);
                nav.addView(tab, tabSize);
            }
            root.addView(nav);
        }
        refresh();
    }

    private Button navigationTab(boolean fixtures) {
        boolean active = fixturesPage == fixtures;
        Button tab = button(fixtures ? "Fixtures" : "Scenes", active, () -> {
            fixturesPage = fixtures;
            portraitScroll = null;
            render();
        });
        tab.setSelected(active);
        tab.setGravity(Gravity.CENTER);
        tab.setMinHeight(dp(61));
        tab.setTextSize(13);
        inset(tab, 8, 6);
        setButtonSurface(tab, active ? blend(surface, accent, .17f) : backdrop, 10);
        tab.setCompoundDrawablePadding(dp(3));
        ConsoleIcon.Shape icon = fixtures ? ConsoleIcon.Shape.FIXTURE : ConsoleIcon.Shape.SCENES;
        tab.setCompoundDrawablesRelative(null, new ConsoleIcon(icon, tab.getCurrentTextColor(),
            dp(26), 2.1f), null, null);
        return tab;
    }

    private void pane(LinearLayout desk, LinearLayout content, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, weight);
        params.rightMargin = dp(7);
        desk.addView(scroll(content), params);
    }

    private LinearLayout cuePanel(boolean grid) {
        LinearLayout list = column();
        if (folders().length == 0) {
            block(list, text("No scene folders", 16, muted, false));
            return list;
        }
        Button folderPicker = button(folders()[sceneFolder] + "  ▾", false, this::chooseFolder);
        folderPicker.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        inset(folderPicker, 13, 6);
        folderPicker.setContentDescription("Scene folder, " + folders()[sceneFolder]);
        block(list, withIcon(folderPicker, ConsoleIcon.Shape.FOLDER));
        int[] cues = liveMode ? live.cues(sceneFolder) : DemoShow.FOLDER_SCENES[sceneFolder];
        if (cues.length == 0) block(list, text("No scenes in this folder", 16, muted, false));
        if (grid) {
            for (int i = 0; i < cues.length; i += 2) {
                LinearLayout tiles = row();
                share(tiles, cueButton(cues[i], true), 1);
                View secondTile = i + 1 < cues.length ? cueButton(cues[i + 1], true) : new Space(this);
                share(tiles, secondTile, 1);
                block(list, tiles);
            }
        } else {
            for (int index : cues) block(list, cueButton(index, false));
        }
        if (!liveMode) {
            Button simulate = button("Simulate ProPresenter cue", false,
                () -> cue(show.nextCue(), "Simulated ProPresenter"));
            setButtonSurface(simulate, backdrop, 9);
            block(list, simulate);
        }
        return list;
    }

    private void chooseFolder() {
        dialog().setTitle("Scene folder")
            .setSingleChoiceItems(folders(), sceneFolder, (dialog, folder) -> {
                sceneFolder = folder;
                portraitScroll = null;
                dialog.dismiss();
                render();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private Button cueButton(int index, boolean grid) {
        boolean current = currentScene() == index;
        String label = sceneName(index) + (current ? (grid ? "\nLive" : "  ·  Live") : "");
        Runnable action = () -> cue(index, "Console cue");
        Button tile = !grid ? button(label, current, action) : styleButton(new Button(this) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                super.onMeasure(widthSpec, heightSpec);
                if (MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY) {
                    int height = Math.max(getMeasuredHeight(), Math.round(getMeasuredWidth() * 2f / 3f));
                    setMeasuredDimension(getMeasuredWidth(), height);
                }
            }
        }, label, current, action);
        tile.setGravity(Gravity.BOTTOM | Gravity.START);
        inset(tile, grid ? 13 : 10, grid ? 11 : 6);
        if (grid) tile.setTextSize(17);
        int tint = sceneTint(index);
        // The bright corner carries the scene colour; the lower edge supports the caption.
        int bright = blend(surface, tint, current ? .94f : .78f);
        int shaded = light ? blend(surface, tint, .33f) : blend(surface, tint, grid ? .33f : .42f);
        GradientDrawable cueSurface = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{bright, shaded});
        cueSurface.setCornerRadius(dp(6));
        if (current) cueSurface.setStroke(dp(2),
            blend(tint, light ? Color.BLACK : Color.WHITE, light ? .38f : .28f));
        tile.setTextColor(light ? 0xff15111a : Color.WHITE);
        if (!light) tile.setShadowLayer(dp(3), 0, dp(1), 0xee100b17);
        tile.setBackground(new RippleDrawable(ColorStateList.valueOf(light ? 0x22000000 : 0x2affffff), cueSurface, null));
        if (liveMode) tile.setEnabled(canControl() && live.scene(index).optBoolean("playable"));
        tile.setAlpha(tile.isEnabled() ? 1f : .55f);
        return tile;
    }

    private void cue(int index, String source) {
        if (liveMode) { send("cue", null, null, index); return; }
        show.cue(index, source);
        render();
    }

    private LinearLayout stagePanel() {
        LinearLayout panel = column();
        TextView stageTitle = text("Stage", 18, ink, true);
        stageTitle.setAccessibilityHeading(true);
        block(panel, stageTitle);
        if (hasFixtureSelection()) {
            String selection = selectionTitle();
            TextView selectedLabel = text(selectedFixtures.size() > 1 ? selection : "Selected · " + selection,
                14, ink, true);
            inset(selectedLabel, 10, 6);
            selectedLabel.setBackground(background(blend(surface, accent, .13f), Color.TRANSPARENT, 8));
            block(panel, selectedLabel);
        }
        FrameLayout stage = new FrameLayout(this);
        stage.setBackground(background(surface, Color.TRANSPARENT, 10));
        stage.setClipToOutline(true);
        if (hasFixtureSelection()) {
            stage.setContentDescription("Stage photo. Clear fixture selection");
            stage.setOnClickListener(v -> {
                clearFixtureSelection();
                render();
            });
        }
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageDrawable(photo);
        stage.addView(image, new FrameLayout.LayoutParams(-1, -1));
        if (!liveMode || live.hasFootprints()) {
            stageFootprints = addFootprintLayer(stage);
            stageFootprints.setVisibility(footprintsVisible ? View.VISIBLE : View.GONE);
        }
        int pinSize = dp(FIXTURE_PIN_DP);
        for (int i = 0; i < markers.length; i++) {
            final int index = i;
            if (liveMode && live.fixture(i).optJSONObject("position") == null) continue;
            TextView marker = text(Integer.toString(i + 1), 10, Color.WHITE, true);
            marker.setGravity(Gravity.CENTER);
            marker.setSingleLine(true);
            marker.setIncludeFontPadding(false);
            marker.setPadding(dp(2), 0, dp(2), 0);
            marker.setAutoSizeTextTypeUniformWithConfiguration(7, 10, 1,
                android.util.TypedValue.COMPLEX_UNIT_SP);
            marker.setContentDescription(fixtureName(i));
            marker.setOnClickListener(v -> selectFixture(index));
            marker.setOnLongClickListener(v -> {
                toggleFixture(index);
                return true;
            });
            markers[i] = marker;
            stage.addView(marker, new FrameLayout.LayoutParams(pinSize, pinSize));
        }
        stage.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            FixturePinLayout layout = new FixturePinLayout(r - l, b - t, pinSize, dp(2));
            for (int i = 0; i < markers.length; i++) {
                View marker = markers[i];
                if (marker == null || marker.getParent() != stage) continue;
                JSONObject position = liveMode ? live.fixture(i).optJSONObject("position") : null;
                double x = liveMode ? position.optDouble("x", .5) : POSITIONS[i][0];
                double y = liveMode ? position.optDouble("y", .5) : POSITIONS[i][1];
                FixturePinLayout.Point placement = layout.place((float) ((r - l) * x) - pinSize / 2f,
                    (float) ((b - t) * y) - pinSize / 2f);
                marker.setTranslationX(placement.x);
                marker.setTranslationY(placement.y);
            }
        });
        panel.addView(stage, new LinearLayout.LayoutParams(-1, dp(235)));
        if (liveMode) {
            if (live.groups.length > 0)
                block(panel, button(group < 0 ? "Select fixture group" : live.groups[group] + "  ▾", false,
                    () -> {
                        int revision = renderRevision;
                        dialog().setTitle("Fixture groups").setItems(live.groups, (dialog, which) -> {
                            if (live != null && revision == live.state.optInt("revision")) selectGroup(which);
                        }).show();
                    }));
            String[] names = new String[fixtureCount()];
            for (int i = 0; i < names.length; i++) names[i] = fixtureName(i) + (isHeld(i) ? " · Held" : "");
            if (names.length > 0) block(panel, button("Select individual fixture", false,
                () -> {
                    int revision = renderRevision;
                    dialog().setTitle("Fixtures").setItems(names, (dialog, which) -> {
                        if (live != null && revision == live.state.optInt("revision")) selectFixture(which);
                    }).show();
                }));
            if (live.hasFootprints()) block(panel, footprintToggle());
        } else {
            LinearLayout groups = row();
            for (int i = 0; i < DemoShow.GROUPS.length; i++) {
                final int selected = i;
                share(groups, button(DemoShow.GROUPS[i], group == i, () -> selectGroup(selected)), 1);
            }
            block(panel, groups);
            LinearLayout previewControls = row();
            share(previewControls, footprintToggle(), 1);
            if (group < 0 && selectedFixtures.size() == 1) share(previewControls, button("Edit footprint", false, this::editFootprint), 1);
            block(panel, previewControls);
        }
        return panel;
    }

    private Button footprintToggle() {
        return button(footprintsVisible ? "Hide light footprints" : "Show light footprints",
            footprintsVisible, () -> {
                footprintsVisible = !footprintsVisible;
                getPreferences(MODE_PRIVATE).edit().putBoolean("footprints-visible", footprintsVisible).apply();
                render();
            });
    }

    private FootprintPreview addFootprintLayer(FrameLayout frame) {
        float photoAspect = photo == null ? 4f / 3f : (float) photo.getIntrinsicWidth() / photo.getIntrinsicHeight();
        LiveShow snapshot = liveMode ? live : null;
        FootprintPreview preview = snapshot == null
            ? new FootprintPreview(this, demoFootprints, i -> show.value(i, 0), i -> show.value(i, 1), photoAspect)
            : new FootprintPreview(this, snapshot.footprints(), snapshot::footprintIntensity, snapshot::footprintColour, photoAspect);
        frame.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        return preview;
    }

    private void editFootprint() {
        int index = firstSelected();
        FootprintPreview.Footprint shape = demoFootprints[index];
        if (shape == null) {
            shape = new FootprintPreview.Footprint(FootprintPreview.Shape.Ellipse,
                FootprintPreview.Falloff.Radial, .5f, .77f, .32f, .23f, 0);
            demoFootprints[index] = shape;
        }
        final FootprintPreview.Footprint selected = shape;
        footprintsVisible = true;
        if (stageFootprints != null) stageFootprints.setVisibility(View.VISIBLE);
        LinearLayout editor = column();
        inset(editor, 15, 5);
        FrameLayout thumb = new FrameLayout(this);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageDrawable(photo);
        thumb.addView(image, new FrameLayout.LayoutParams(-1, -1));
        FootprintPreview preview = addFootprintLayer(thumb);
        editor.addView(thumb, new LinearLayout.LayoutParams(-1, dp(168)));
        Runnable update = () -> {
            preview.invalidate();
            if (stageFootprints != null) stageFootprints.invalidate();
        };
        update.run();
        LinearLayout controls = column();
        block(controls, footprintChoice("Shape", FootprintPreview.Shape.values(), selected.shape.ordinal(), ordinal -> {
            selected.shape = FootprintPreview.Shape.values()[ordinal];
            update.run();
        }));
        block(controls, footprintChoice("Falloff", FootprintPreview.Falloff.values(), selected.falloff.ordinal(), ordinal -> {
            selected.falloff = FootprintPreview.Falloff.values()[ordinal];
            update.run();
        }));
        footprintSlider(controls, "Horizontal position", 0, 100, Math.round(selected.x*100), v -> selected.x = v/100f, update);
        footprintSlider(controls, "Vertical position", 0, 100, Math.round(selected.y*100), v -> selected.y = v/100f, update);
        footprintSlider(controls, "Width", 5, 100, Math.round(selected.width*100), v -> selected.width = v/100f, update);
        footprintSlider(controls, "Height", 5, 100, Math.round(selected.height*100), v -> selected.height = v/100f, update);
        footprintSlider(controls, "Rotation", -180, 180, Math.round(selected.angle), v -> selected.angle = v, update);
        footprintSlider(controls, "Hotspot left / right (radial)", 0, 100, Math.round(selected.hotspotX*100), v -> selected.hotspotX = v/100f, update);
        footprintSlider(controls, "Hotspot up / down", 0, 100, Math.round(selected.hotspotY*100), v -> selected.hotspotY = v/100f, update);
        footprintSlider(controls, "Softness", 0, 100, Math.round(selected.softness*100), v -> selected.softness = v/100f, update);
        editor.addView(scroll(controls), new LinearLayout.LayoutParams(-1, 0, 1));
        AlertDialog window = dialog().setTitle("Demo footprint · " + fixtureName(index)).setView(editor)
            .setNeutralButton("Remove", (dialog, action) -> demoFootprints[index] = null)
            .setPositiveButton("Done", null).create();
        window.setOnDismissListener(dialog -> render());
        window.show();
        if (window.getWindow() != null)
            window.getWindow().setLayout(-1, Math.round(getResources().getDisplayMetrics().heightPixels * .88f));
    }

    private Button footprintChoice(String label, Enum<?>[] values, int selected,
                                   java.util.function.IntConsumer change) {
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) names[i] = values[i].name();
        Button option = button(label + " · " + names[selected], false, () -> {});
        option.setOnClickListener(v -> dialog().setTitle(label).setItems(names, (dialog, index) -> {
            option.setText(label + " · " + names[index]);
            change.accept(index);
        }).show());
        return option;
    }

    private void footprintSlider(LinearLayout parent, String name, int min, int max, int initial,
                                 java.util.function.IntConsumer change, Runnable redrawPreview) {
        String unit = min < 0 ? "°" : "%";
        TextView label = text(name + " · " + initial + unit, 14, ink, true);
        block(parent, label);
        SeekBar bar = new SeekBar(this);
        bar.setMax(max-min);
        bar.setProgress(initial-min);
        bar.setContentDescription(name);
        bar.setMinimumHeight(dp(44));
        bar.setProgressTintList(ColorStateList.valueOf(accent));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar view) {}
            @Override public void onStopTrackingTouch(SeekBar view) {}
            @Override public void onProgressChanged(SeekBar view, int progress, boolean fromUser) {
                if (!fromUser) return;
                change.accept(progress + min);
                label.setText(name + " · " + (progress + min) + unit);
                redrawPreview.run();
            }
        });
        block(parent, bar);
    }

    private void clearFixtureSelection() {
        selectedFixtures.clear();
        group = -1;
    }

    private boolean canSelectFixture(int index) {
        return index >= 0 && index < fixtureCount() &&
            (!liveMode || renderRevision == live.state.optInt("revision"));
    }

    private void selectFixture(int index) {
        if (!canSelectFixture(index)) return;
        clearFixtureSelection();
        selectedFixtures.add(index);
        render();
    }

    private void selectGroup(int index) {
        selectedFixtures.clear();
        group = index;
        render();
    }

    private void toggleFixture(int index) {
        if (!canSelectFixture(index)) return;
        if (group >= 0) {
            for (int i = 0; i < fixtureCount(); i++) {
                if (selected(i)) selectedFixtures.add(i);
            }
            group = -1;
        }
        if (!selectedFixtures.remove(index)) selectedFixtures.add(index);
        render();
    }

    private boolean selected(int index) {
        return group < 0 ? selectedFixtures.contains(index)
            : (liveMode ? live.group(index) : DemoShow.group(index)) == group;
    }

    private boolean hasFixtureSelection() {
        return fixtureCount() > 0 && (group >= 0 || !selectedFixtures.isEmpty());
    }

    private String selectionTitle() {
        if (group >= 0) return groups()[group];
        if (selectedFixtures.size() == 1) return fixtureName(firstSelected());
        return selectedFixtures.size() + " fixtures selected";
    }

    private LinearLayout inspector() {
        LinearLayout panel = column();
        if (fixtureCount() == 0) {
            block(panel, text("No configured fixtures", 16, muted, false));
            return panel;
        }
        inset(panel, 13, 11);
        panel.setBackground(background(surface, Color.TRANSPARENT, 10));
        if (!hasFixtureSelection()) {
            block(panel, text("Select a fixture or group", 16, muted, false));
            if (!portrait()) addReleaseAll(panel);
            return panel;
        }
        String selection = selectionTitle();
        TextView title = text(selection, 22, ink, true);
        title.setAccessibilityHeading(true);
        block(panel, title);
        selectionStatus = text("Following scene", 13, muted, false);
        block(panel, selectionStatus);
        if (!portrait()) addReleaseAll(panel);
        View rule = new View(this);
        rule.setBackgroundColor(blend(surface, divider, .55f));
        LinearLayout.LayoutParams ruleSize = new LinearLayout.LayoutParams(-1, 1);
        ruleSize.bottomMargin = dp(10);
        panel.addView(rule, ruleSize);
        if (supports(0)) slider(panel, "Intensity", 0);
        if (supports(1)) colourPanel(panel);
        if (supports(2)) slider(panel, "Pan", 2);
        if (supports(3)) slider(panel, "Tilt", 3);
        release = button("Release adjustment", false, () -> {
            if (liveMode) { send("release", null, null, null); return; }
            for (int i = 0; i < markers.length; i++) if (selected(i)) show.release(i);
            render();
        });
        setButtonSurface(release, blend(surface, warning, .08f), 9);
        release.setTextColor(warning);
        block(panel, withIcon(release, ConsoleIcon.Shape.RELEASE));
        return panel;
    }

    private void addReleaseAll(LinearLayout parent) {
        releaseAll = button("Release all adjustments", false, this::confirmReleaseAll);
        setButtonSurface(releaseAll, blend(surface, warning, .08f), 9);
        releaseAll.setTextColor(warning);
        releaseAll.setVisibility(View.GONE);
        block(parent, withIcon(releaseAll, ConsoleIcon.Shape.RELEASE));
    }

    private void confirmReleaseAll() {
        if (!liveMode) {
            for (int i = 0; i < fixtureCount(); i++) show.release(i);
            render();
            return;
        }
        dialog().setTitle("Release all adjustments?")
            .setMessage("All held fixture adjustments will return to the running scene.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Release all", (dialog, which) -> send("release", null, null, null, true))
            .show();
    }

    private void updateReleaseButton(Button action, boolean hasHeldFixtures) {
        if (action == null) return;
        boolean enabled = hasHeldFixtures && canControl() && (!liveMode || !live.state.optBoolean("dark"));
        action.setEnabled(enabled);
        action.setAlpha(enabled ? 1f : .55f);
    }

    private Drawable fixtureMarker(int color, boolean selected) {
        int radius = 2;
        if (!selected) return background(color, 0xffd2cbd5, radius);
        // A black separator keeps the white outline readable against any fixture colour.
        LayerDrawable layers = new LayerDrawable(new Drawable[]{
            background(Color.BLACK, Color.BLACK, radius),
            background(Color.WHITE, Color.WHITE, radius),
            background(color, Color.BLACK, radius)
        });
        int outer = dp(1);
        int inner = dp(2);
        layers.setLayerInset(1, outer, outer, outer, outer);
        layers.setLayerInset(2, inner, inner, inner, inner);
        return layers;
    }

    private void colourPanel(LinearLayout panel) {
        block(panel, text("Colour", 16, muted, true));
        ColourWheel wheel = new ColourWheel(firstSelected());
        LinearLayout.LayoutParams wheelSize = new LinearLayout.LayoutParams(-1, -2);
        wheelSize.bottomMargin = dp(8);
        panel.addView(wheel, wheelSize);
        for (int i = 0; i < PALETTE.length; i += 4) {
            LinearLayout swatches = row();
            for (int j = i; j < i + 4; j++) {
                final int color = PALETTE[j];
                Button swatch = button("", false, () -> {
                    adjust(1, color);
                    wheel.invalidate();
                });
                swatch.setEnabled(canControl());
                swatch.setContentDescription(COLOR_NAMES[j]);
                setButtonSurface(swatch, color, 8);
                swatch.setAlpha(swatch.isEnabled() ? 1f : .55f);
                share(swatches, swatch, 1);
            }
            block(panel, swatches);
        }
    }

    private int firstSelected() {
        for (int i = 0; i < markers.length; i++) if (selected(i)) return i;
        throw new IllegalStateException("Selection has no fixtures");
    }

    private final class ColourWheel extends View {
        private final int fixtureIndex;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] hsv = new float[3];
        private float cx, cy, radius;
        private Shader hue, saturation;

        ColourWheel(int fixtureIndex) {
            super(MainActivity.this);
            this.fixtureIndex = fixtureIndex;
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            int max = dp(wideConsole() ? 264 : 344);
            setMeasuredDimension(width, resolveSize(Math.min(width, max), heightSpec));
        }

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            if (w == 0 || h == 0) return;
            cx = w / 2f;
            cy = h / 2f;
            radius = Math.min(cx, cy) - dp(9);
            if (radius <= 0) return;
            hue = new SweepGradient(cx, cy, new int[] {Color.RED, Color.YELLOW, Color.GREEN,
                Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED}, null);
            saturation = new RadialGradient(cx, cy, radius, Color.WHITE, 0x00ffffff, Shader.TileMode.CLAMP);
        }

        @Override protected void onDraw(Canvas canvas) {
            if (radius <= 0) return;
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(hue);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(saturation);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(light ? 0x66000000 : 0x88ffffff);
            paint.setStrokeWidth(Math.max(1, dp(.7f)));
            canvas.drawCircle(cx, cy, radius, paint);
            Color.colorToHSV(value(fixtureIndex, 1), hsv);
            double angle = Math.toRadians(hsv[0]);
            float x = cx + (float) Math.cos(angle) * hsv[1] * radius;
            float y = cy + (float) Math.sin(angle) * hsv[1] * radius;
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xff151219);
            paint.setStrokeWidth(dp(4));
            canvas.drawCircle(x, y, dp(7), paint);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(dp(2));
            canvas.drawCircle(x, y, dp(7), paint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (radius <= 0 || !canControl()) return false;
            float x = event.getX() - cx;
            float y = event.getY() - cy;
            float distance = (float) Math.hypot(x, y);
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                if (distance > radius) return false;
                getParent().requestDisallowInterceptTouchEvent(true);
            } else if (action == MotionEvent.ACTION_CANCEL) {
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            } else if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP) {
                return true;
            }
            hsv[0] = ((float) Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
            hsv[1] = Math.min(1, distance / radius);
            hsv[2] = 1;
            if (!liveMode || action == MotionEvent.ACTION_UP) adjust(1, Color.HSVToColor(hsv));
            invalidate();
            if (action == MotionEvent.ACTION_UP) {
                getParent().requestDisallowInterceptTouchEvent(false);
                performClick();
            }
            return true;
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }
    }

    private void slider(LinearLayout panel, String name, int channel) {
        int value = -1;
        boolean mixed = false;
        for (int i = 0; i < markers.length; i++) {
            if (!selected(i)) continue;
            int next = value(i, channel);
            if (value < 0) value = next;
            else if (value != next) mixed = true;
        }
        String unit = "%";
        LinearLayout sliderHeader = row();
        sliderHeader.addView(text(name, 15, ink, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView label = text(mixed ? "Mixed" : value + unit, 14, accent, true);
        label.setFontFeatureSettings("tnum");
        sliderHeader.addView(label);
        block(panel, sliderHeader);
        SeekBar seek = new SeekBar(this);
        seek.setMax(100);
        seek.setProgressTintList(ColorStateList.valueOf(accent));
        seek.setThumbTintList(ColorStateList.valueOf(ink));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(divider));
        seek.setEnabled(canControl());
        seek.setProgress(value);
        seek.setMinimumHeight(dp(42));
        seek.setContentDescription(name);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean dragging, changed;
            @Override public void onStartTrackingTouch(SeekBar bar) { dragging = true; changed = false; }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                dragging = false;
                if (liveMode && changed && !touchCancelled) adjust(channel, bar.getProgress());
            }
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                changed = true;
                label.setText(progress + unit);
                if (!liveMode || !dragging) adjust(channel, progress);
            }
        });
        block(panel, seek);
    }

    private void adjust(int channel, int value) {
        if (liveMode) { send(channel == 1 ? "colour" : "adjust", channel, value, null); return; }
        for (int i = 0; i < markers.length; i++) if (selected(i)) show.adjust(i, channel, value);
        refresh();
    }

    private void refresh() {
        if (stageFootprints != null) stageFootprints.invalidate();
        int held = 0;
        boolean selectionHeld = false;
        for (int i = 0; i < markers.length; i++) {
            boolean overridden = isHeld(i);
            if (overridden) {
                held++;
                selectionHeld |= selected(i);
            }
            if (markers[i] == null) continue;
            int color = blend(Color.BLACK, value(i, 1), Math.max(0, Math.min(100, value(i, 0))) / 100f);
            boolean chosen = selected(i);
            markers[i].setBackground(fixtureMarker(color, chosen));
            markers[i].setTextColor(Color.luminance(color) > .179f ? Color.BLACK : Color.WHITE);
            markers[i].setTranslationZ(chosen ? dp(3) : 0);
            markers[i].setSelected(chosen);
            markers[i].setText((i + 1) + (overridden ? "*" : ""));
            markers[i].setContentDescription(fixtureName(i) + (chosen ? ", selected" : "")
                + (overridden ? ", held adjustment" : ""));
        }
        updateReleaseButton(release, selectionHeld);
        if (releaseAll != null) {
            releaseAll.setVisibility(held > 0 ? View.VISIBLE : View.GONE);
            updateReleaseButton(releaseAll, held > 0);
        }
        if (selectionStatus != null) {
            selectionStatus.setText(selectionHeld ? "Adjustment held" : "Following scene");
            selectionStatus.setTextColor(selectionHeld ? warning : muted);
        }
        String scene = currentScene() < 0 ? "No scene" : sceneName(currentScene());
        boolean available = !liveMode || (connected && live.ready());
        String label = !liveMode ? "Current cue"
            : !connected ? connectionMessage
            : !live.ready() ? "Controls unavailable at desk"
            : "simulation".equals(live.state.optString("output")) ? "Host simulation" : "Live cue";
        status.setText(label);
        status.setTextColor(available ? accent : warning);
        statusScene.setText(!liveMode || connected ? scene : "Last known · " + scene);
        String details = held > 0 ? held + (held == 1 ? " fixture held" : " fixtures held")
            : !liveMode ? show.source : "";
        statusDetails.setText(details);
        statusDetails.setTextColor(held > 0 ? warning : muted);
        statusDetails.setVisibility(details.isEmpty() ? View.GONE : View.VISIBLE);
        int stateTint = available ? (currentScene() >= 0 ? sceneTint(currentScene()) : accent) : warning;
        if (available) status.setTextColor(blend(ink, stateTint, light ? .25f : .5f));
        statusCard.setBackground(tintedCard(blend(surface, stateTint, .30f), surface));
    }
}
