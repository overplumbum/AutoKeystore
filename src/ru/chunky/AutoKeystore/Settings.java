package ru.chunky.AutoKeystore;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Settings extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.applyContext(this);
        addPreferencesFromResource(R.xml.preferences);

        findPreference("unlock_btn").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean unlocked;
                unlocked = SimpleKeystore.getInstance().state() == SimpleKeystore.State.UNLOCKED;

                boolean relocked = false;
                SudoedKeyStore suk = new SudoedKeyStore(preference.getContext());

                if (unlocked) {
                    try {
                        suk.lock();
                        relocked = true;
                    } catch (SudoedKeyStore.Error e) {
                        showNotification("Failed to lock: " + e.getMsg(preference.getContext()), preference.getContext());
                        return false;
                    }
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(preference.getContext());

                try {
                    String passwd = prefs.getString("passwd", null);
                    suk.unlock(passwd);
                } catch (SudoedKeyStore.Error e) {
                    showNotification("Failed to unlock: " + e.getMsg(preference.getContext()), preference.getContext());
                }

                String msg = preference.getContext().getString(relocked ? R.string.status_no_error_relocked : R.string.status_no_error);
                showNotification(msg, preference.getContext());
                return false;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            findPreference("unlockvpn").setEnabled(true);

            findPreference("unlockvpn").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent();
                    intent.setClass(Settings.this, UnlockVPN.class);
                    startActivity(intent);

                    return false;
                }
            });
        }
    }

    private void showNotification(CharSequence text, Context context) {
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    @Override
    protected void onStop() {
        Util.onClose(this);
        super.onStop();
    }
}
