package org.lifeac.lighting.companion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/** Reads the public pilot manifest without sharing any desk or pairing details. */
final class PilotUpdate {
    private static final String DOWNLOAD_ROOT =
        "https://raw.githubusercontent.com/Life-Anglican-Church/LifeLight/pilot-downloads/pilot-downloads/";
    final String version;
    final String downloadUrl;

    private PilotUpdate(String version) {
        this.version = version;
        downloadUrl = DOWNLOAD_ROOT + "LifeLIGHT-Pilot-" + version + ".apk";
    }

    static PilotUpdate check(long installedCode) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection)
            URI.create(DOWNLOAD_ROOT + "latest-android.json").toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setRequestProperty("Cache-Control", "no-cache");
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
        if (code <= installedCode || !version.matches("[0-9]{1,4}\\.[0-9]{1,4}\\.[0-9]{1,4}")) return null;
        return new PilotUpdate(version);
    }
}
