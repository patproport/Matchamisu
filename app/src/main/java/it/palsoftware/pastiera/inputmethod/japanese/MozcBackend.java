package it.palsoftware.pastiera.inputmethod.japanese;

import android.content.Context;

import com.google.android.apps.inputmethod.libs.mozc.session.MozcJni;

/** Loads the official source-built Mozc binding; session commands stay in MozcBridge. */
public final class MozcBackend {
    private MozcBackend() {}

    public static void load(Context context) {
        MozcJni.load(context);
    }

    public static byte[] evalCommand(byte[] command) {
        return MozcJni.evalCommand(command);
    }
}
