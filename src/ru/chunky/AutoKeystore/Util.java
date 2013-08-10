package ru.chunky.AutoKeystore;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import com.bugsense.trace.BugSenseHandler;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {
    private static final String TAG = "AutoKeystore";
    public static final String BUG_SENSE_KEY = null;

    public static String android_id = "unknown";
    private static boolean dev = false;
    private static boolean bugSenseActive = false;

    private static void wtf(Throwable tr) {
        try {
            Log.wtf(TAG, tr);
        } catch (NoSuchMethodError en) {
            // happens on 2.0
        }
    }

    private static void wtf(String msg, Throwable tr) {
        try {
            Log.wtf(TAG, msg, tr);
        } catch (NoSuchMethodError en) {
            // happens on 2.0
        }
    }

    public static void error(String msg, Exception e) {
        wtf(msg, e);
        try {
            if (bugSenseActive) {
                BugSenseHandler.sendExceptionMessage("exception", msg, e);
            }
        } catch (Exception et) {
            wtf(et);
        }
    }

    public static void error(Exception e) {
        wtf(e);
        try {
            if (bugSenseActive) {
                BugSenseHandler.sendExceptionMessage("exception", "error", e);
            }
        } catch (Exception et) {
            wtf(et);
        }
    }

    public static void error(String msg) {
        Log.e(TAG, msg);
        try {
            if (bugSenseActive) {
                BugSenseHandler.sendEvent(msg);
            }
        } catch (Exception et) {
            wtf(et);
        }
    }

    public static void log(String msg) {
        if (dev) {
            Log.v(TAG, msg);
        }
    }

    public static void log(String format, Object... args) {
        log(String.format(format, args));
    }

    public static void applyContext(Context context) {
        dev = false;
        if (!dev && BUG_SENSE_KEY != null) {
            BugSenseHandler.initAndStartSession(context, BUG_SENSE_KEY);
            bugSenseActive = true;
        }
    }

    public static void onClose(Context ctx) {
        if (bugSenseActive) {
            BugSenseHandler.closeSession(ctx);
            bugSenseActive = false;
        }
    }

    public static boolean canExecute(File f) {
        try {
            return f.canExecute();
        } catch (NoSuchMethodError e) {
            return false;
        }
    }

    public static StringBuffer join(String[] items) {
        StringBuffer result = new StringBuffer("");
        for (String item : items) {
            result.append(item);
        }
        return result;
    }

    public static void flushErrors(Context ctx) {
        if (bugSenseActive) {
            BugSenseHandler.flush(ctx);
        }
    }
}
