package org.lifeac.lighting.companion;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.json.JSONObject;

final class LiveHost {
    private static final String KEY = "lifeac-lighting-pairing";
    final String origin;
    private final String fingerprint;
    private final SSLContext tls;
    private String token;

    static final class ResponseError extends IOException {
        final int status;
        ResponseError(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    LiveHost(JSONObject data) throws Exception {
        URL url = new URL(data.getString("url"));
        if (!"https".equals(url.getProtocol()) || url.getUserInfo() != null
                || url.getHost().isEmpty() || url.getPort() < 1 || url.getPort() > 65535
                || !"/".equals(url.getFile()) || url.getRef() != null)
            throw new IOException("Invalid pairing address");
        origin = "https://" + url.getAuthority();
        fingerprint = data.getString("fingerprint").replace(":", "").toLowerCase(java.util.Locale.ROOT);
        if (!fingerprint.matches("[0-9a-f]{64}")) throw new IOException("Invalid host fingerprint");
        token = data.optString("token", "");
        if (!token.isEmpty() && !token.matches("[0-9a-f]{64}")) throw new IOException("Invalid device approval");
        tls = SSLContext.getInstance("TLS");
        tls.init(null, new TrustManager[] {new X509TrustManager() {
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            @Override public void checkClientTrusted(X509Certificate[] chain, String type) throws CertificateException {
                throw new CertificateException("Client certificates are not used");
            }
            @Override public void checkServerTrusted(X509Certificate[] chain, String type) throws CertificateException {
                if (chain.length == 0) throw new CertificateException("Host certificate missing");
                chain[0].checkValidity();
                verifyPin(chain[0]);
            }
        }}, null);
    }

    private void verifyPin(X509Certificate cert) throws CertificateException {
        try {
            byte[] actual = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
            byte[] expected = new byte[32];
            for (int i = 0; i < 32; i++) expected[i] = (byte) Integer.parseInt(fingerprint.substring(i * 2, i * 2 + 2), 16);
            if (!MessageDigest.isEqual(expected, actual)) throw new CertificateException("Host certificate changed. Pair again at the desk.");
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new CertificateException(error);
        }
    }

    JSONObject request(String path, JSONObject body) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(origin + "/api/mobile/" + path).openConnection();
        connection.setSSLSocketFactory(tls.getSocketFactory());
        connection.setHostnameVerifier((hostname, session) -> {
            try {
                verifyPin((X509Certificate) session.getPeerCertificates()[0]);
                return true;
            } catch (Exception error) {
                return false;
            }
        });
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(2500);
        connection.setReadTimeout(3500);
        if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
        try {
            if (body != null) {
                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Origin", origin);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setFixedLengthStreamingMode(data.length);
                connection.setDoOutput(true);
                try (OutputStream stream = connection.getOutputStream()) { stream.write(data); }
            }
            int status = connection.getResponseCode();
            try (InputStream input = status == 200 ? connection.getInputStream() : connection.getErrorStream()) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                if (input != null) for (int count; (count = input.read(buffer)) != -1;) {
                    if (bytes.size() + count > 2_000_000) throw new IOException("Host response too large");
                    bytes.write(buffer, 0, count);
                }
                JSONObject result;
                try {
                    result = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                } catch (org.json.JSONException error) {
                    if (status == 200) throw error;
                    result = new JSONObject();
                }
                if (status != 200) throw new ResponseError(status, result.optString("error", "Host request failed"));
                return result;
            }
        } finally {
            connection.disconnect();
        }
    }

    static LiveHost pair(Context context, JSONObject qr, String name) throws Exception {
        if (!"lifeac-pair-v1".equals(qr.optString("type")) || !qr.optString("code").matches("[0-9a-f]{64}"))
            throw new IOException("Scan a LifeAC Lighting pairing code");
        LiveHost host = new LiveHost(new JSONObject().put("url", qr.getString("url")).put("fingerprint", qr.getString("fingerprint")));
        host.token = host.request("pair", new JSONObject().put("code", qr.getString("code")).put("name", name)).getString("token");
        host.save(context);
        return host;
    }

    private static SecretKey key(boolean create) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY)) return (SecretKey) store.getKey(KEY, null);
        if (!create) throw new IOException("Device approval key missing");
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }

    private void save(Context context) throws Exception {
        JSONObject secret = new JSONObject().put("url", origin + "/").put("fingerprint", fingerprint).put("token", token);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(true));
        byte[] iv = cipher.getIV();
        byte[] data = cipher.doFinal(secret.toString().getBytes(StandardCharsets.UTF_8));
        byte[] combined = Arrays.copyOf(iv, iv.length + data.length);
        System.arraycopy(data, 0, combined, iv.length, data.length);
        if (!context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit()
                .putString("pairing", Base64.encodeToString(combined, Base64.NO_WRAP)).commit())
            throw new IOException("Could not save device approval");
    }

    static LiveHost load(Context context) throws Exception {
        String saved = context.getSharedPreferences(KEY, Context.MODE_PRIVATE).getString("pairing", null);
        if (saved == null) return null;
        byte[] data = Base64.decode(saved, Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(false), new GCMParameterSpec(128, Arrays.copyOf(data, 12)));
        return new LiveHost(new JSONObject(new String(cipher.doFinal(data, 12, data.length - 12), StandardCharsets.UTF_8)));
    }

    static void forget(Context context) {
        context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
