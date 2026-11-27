package org.lifeac.lighting.companion;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

final class PilotUpdate {
    private static final String DOWNLOAD_ROOT =
        "https://raw.githubusercontent.com/Life-Anglican-Church/LifeLight/pilot-downloads/pilot-downloads/";
    static final String APK_MIME = "application/vnd.android.package-archive";
    static final String APK_NAME_PATTERN = "pilot-[0-9]{1,10}\\.apk";
    private static final long MAX_APK_BYTES = 32L * 1024 * 1024;
    final String version;
    final long versionCode;
    private final String sha256;

    private PilotUpdate(String version, long versionCode, String sha256) {
        this.version = version;
        this.versionCode = versionCode;
        this.sha256 = sha256;
    }

    static File stagingDirectory(Context context) {
        return new File(context.getCacheDir(), "verified-pilot-updates");
    }

    static void cleanInstalledUpdates(Context context, long installedCode) {
        File[] files = stagingDirectory(context).listFiles();
        if (files == null) return;
        for (File file : files) {
            String name = file.getName();
            if (name.matches(APK_NAME_PATTERN)
                && Long.parseLong(name.substring(6, name.length() - 4)) <= installedCode) file.delete();
        }
    }

    private static HttpsURLConnection connect(String filename, int readTimeout) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection)
            URI.create(DOWNLOAD_ROOT + filename).toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(readTimeout);
        connection.setRequestProperty("Cache-Control", "no-cache");
        return connection;
    }

    static PilotUpdate check(long installedCode) throws Exception {
        HttpsURLConnection connection = connect("latest-android.json", 3000);
        try {
            if (connection.getResponseCode() != 200) return null;
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[512];
                for (int count; (count = input.read(buffer)) != -1;) {
                    if (output.size() + count > 4096) throw new IOException("Update manifest too large");
                    output.write(buffer, 0, count);
                }
                return available(new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8)), installedCode);
            }
        } finally {
            connection.disconnect();
        }
    }

    static PilotUpdate available(JSONObject manifest, long installedCode) throws org.json.JSONException {
        long code = manifest.getLong("versionCode");
        String version = manifest.getString("version");
        String checksum = manifest.optString("sha256", "");
        if (code <= installedCode || code > Integer.MAX_VALUE
            || !version.matches("[0-9]{1,4}\\.[0-9]{1,4}\\.[0-9]{1,4}")
            || !checksum.matches("[a-f0-9]{64}")) return null;
        return new PilotUpdate(version, code, checksum);
    }

    File downloadAndVerify(Context context) throws Exception {
        File directory = stagingDirectory(context);
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Update storage unavailable");
        File candidate = File.createTempFile("download-", ".apk", directory);
        try {
            download(candidate);
            verifyPackage(context, candidate);
            checkCancelled();
            File verified = new File(directory, "pilot-" + versionCode + ".apk");
            Files.move(candidate.toPath(), verified.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return verified;
        } finally {
            candidate.delete();
        }
    }

    private void download(File candidate) throws Exception {
        HttpsURLConnection connection = connect("LifeLIGHT-Pilot-" + version + ".apk", 12000);
        try {
            if (connection.getResponseCode() != 200) throw new IOException("APK download unavailable");
            long length = connection.getContentLengthLong();
            if (length > MAX_APK_BYTES) throw new IOException("APK exceeds the update size limit");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(candidate)) {
                byte[] buffer = new byte[16384];
                long bytes = 0;
                for (int count; (count = input.read(buffer)) != -1;) {
                    checkCancelled();
                    bytes += count;
                    if (bytes > MAX_APK_BYTES) throw new IOException("APK exceeds the update size limit");
                    digest.update(buffer, 0, count);
                    output.write(buffer, 0, count);
                }
            }
            byte[] expected = new byte[32];
            for (int i = 0; i < expected.length; i++) {
                expected[i] = (byte) Integer.parseInt(sha256.substring(i * 2, i * 2 + 2), 16);
            }
            if (!MessageDigest.isEqual(expected, digest.digest())) throw new IOException("APK checksum does not match");
        } finally {
            connection.disconnect();
        }
    }

    private void verifyPackage(Context context, File candidate) throws Exception {
        int flags = PackageManager.GET_SIGNING_CERTIFICATES;
        PackageManager manager = context.getPackageManager();
        PackageInfo installed = manager.getPackageInfo(context.getPackageName(), flags);
        PackageInfo archive = manager.getPackageArchiveInfo(candidate.getAbsolutePath(), flags);
        if (archive == null || !installed.packageName.equals(archive.packageName)
            || archive.getLongVersionCode() != versionCode || versionCode <= installed.getLongVersionCode()
            || installed.signingInfo == null || archive.signingInfo == null) {
            throw new IOException("APK identity or version does not match");
        }
        Signature[] current = installed.signingInfo.getApkContentsSigners();
        Signature[] incoming = archive.signingInfo.getApkContentsSigners();
        if (current == null || current.length == 0 || incoming == null) throw new IOException("APK has no signing certificate");
        Comparator<Signature> order = Comparator.comparing(Signature::toCharsString);
        Arrays.sort(current, order);
        Arrays.sort(incoming, order);
        if (!Arrays.equals(current, incoming)) throw new IOException("APK signing certificate does not match");
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Update cancelled");
    }
}
