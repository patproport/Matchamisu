package com.google.android.apps.inputmethod.libs.mozc.session;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Binding for the source-built Mozc Android session JNI at the pinned revision. */
public final class MozcJni {
    private static final String DATA_SHA256 = "720d0cb42651fe35692578ea5f3c752940b8357fc24efc46230c904d2f5d5d70";
    private static boolean loaded;

    private MozcJni() {}

    public static synchronized void load(Context context) {
        if (loaded) return;
        System.loadLibrary("mozc");
        if (!initialize()) throw new IllegalStateException("Mozc JNI registration failed");

        File profile = new File(context.getFilesDir(), "source_mozc_profile");
        if (!profile.isDirectory() && !profile.mkdirs()) {
            throw new IllegalStateException("Cannot create Mozc profile directory");
        }
        File data = ensureDataFile(context);
        if (!onPostLoad(profile.getAbsolutePath(), data.getAbsolutePath())) {
            throw new IllegalStateException("Mozc onPostLoad failed");
        }
        String dataVersion = getDataVersion();
        // The pinned engine returns 0.0.0 when data loading fell back to its minimal converter.
        if (dataVersion == null || dataVersion.isEmpty() || "0.0.0".equals(dataVersion)) {
            throw new IllegalStateException("Mozc dictionary was not loaded");
        }
        loaded = true;
    }

    private static File ensureDataFile(Context context) {
        File directory = new File(context.getFilesDir(), "source_mozc");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("Cannot create Mozc data directory");
        }
        File data = new File(directory, "mozc.data");
        try {
            if (data.isFile() && DATA_SHA256.equals(sha256(data))) return data;
            File temporary = File.createTempFile("mozc-", ".data", directory);
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream input = context.getAssets().open("mozc.data");
                     FileOutputStream output = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[64 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                        digest.update(buffer, 0, count);
                    }
                    output.getFD().sync();
                }
                if (!DATA_SHA256.equals(hex(digest.digest()))) {
                    throw new IllegalStateException("Packaged Mozc data hash mismatch");
                }
                try {
                    Files.move(temporary.toPath(), data.toPath(),
                            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary.toPath(), data.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                if (temporary.exists()) temporary.delete();
            }
            return data;
        } catch (IOException | NoSuchAlgorithmException error) {
            throw new IllegalStateException("Cannot prepare Mozc data file", error);
        }
    }

    private static String sha256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return hex(digest.digest());
    }

    private static String hex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            result[index * 2] = Character.forDigit((bytes[index] >>> 4) & 0xf, 16);
            result[index * 2 + 1] = Character.forDigit(bytes[index] & 0xf, 16);
        }
        return new String(result);
    }

    public static native boolean initialize();
    public static native byte[] evalCommand(byte[] command);
    public static native boolean onPostLoad(String profileDirectory, String dataFilePath);
    public static native String getDataVersion();
}
