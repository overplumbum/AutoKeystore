package ru.chunky.AutoKeystore;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class UnlockReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean("boot_unlock_enabled", false)
                || prefs.getString("passwd", "") == null
                || prefs.getString("passwd", "").length() == 0) {
            return;
        }

        SudoedKeyStore suk = new SudoedKeyStore(context);
        SudoedKeyStore.Error error = null;
        try {
            suk.unlock(prefs.getString("passwd", null));
        } catch (SudoedKeyStore.Error e) {
            error = e;
        }
        if (error != null && error.is_wrong_password) {
            prefs.edit().putBoolean("boot_unlock_enabled", false).commit();
            showNotification(context, context.getResources().getString(R.string.wrong_password_disabling));
        } else if (error != null) {
            showNotification(context, error.getMsg(context));
        }
    }

    private void showNotification(Context context, String message) {
        Resources res = context.getResources();

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        CharSequence tickerText = res.getString(R.string.notification_ticker);
        long when = System.currentTimeMillis();

        Notification notification = new Notification(R.drawable.stat_unlock, tickerText, when);
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        CharSequence contentTitle = tickerText;
        CharSequence contentText = message;

        Intent notificationIntent = new Intent(context, Settings.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        mNotificationManager.notify(1, notification);
    }
}
