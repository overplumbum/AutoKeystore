package ru.chunky.AutoKeystore;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class UnlockVPN extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.applyContext(this);
        addPreferencesFromResource(R.xml.unlockvpn);

        findPreference("unlock").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return perform_unlock(preference.getContext());
            }
        });

        findPreference("settings_lock").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // "com.android.settings/.ChooseLockPassword"
                // https://android.googlesource.com/platform/packages/apps/Settings/+/master/AndroidManifest.xml
                Intent intent = new Intent("android.app.action.SET_NEW_PASSWORD");
                startActivity(intent);
                return false;
            }
        });

        findPreference("settings_vpn").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent("android.net.vpn.SETTINGS");
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Intent intent = new Intent("android.settings.SETTINGS");
                    startActivity(intent);
                }
                return false;
            }
        });

        findPreference("autokeystore").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent();
                intent.setClass(preference.getContext(), Settings.class);
                preference.getContext().startActivity(intent);
                return false;
            }
        });

        findPreference("credits").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://forum.xda-developers.com/showthread.php?p=32987763"));
                startActivity(browserIntent);
                return false;
            }
        });
    }

    private boolean perform_unlock(Context ctx) {
        File cmd = new File(ctx.getApplicationInfo().dataDir, "lib/libunlockcmd-executable.so");

        boolean ok = true;
        try {
            if (!Util.canExecute(cmd)) {
                try {
                    execute(new String[]{
                        "su", "-c",
                        String.format("chmod 0555 %s", cmd.getAbsolutePath())
                    });
                } catch (IOException e) {
                    Util.log(e.toString());
                    // try anyway
                }
            } else {
                Util.log("already executable");
            }

            execute(new String[]{
                "su", "-c",
                cmd.getAbsolutePath()
            });
        } catch (InterruptedException e) {
            Util.error(e);
            ok = false;
        } catch (IOException e) {
            Util.error(e);
            ok = false;
        } catch (CmdException e) {
            Util.error(e);
            ok = false;
        }
        showNotification(ok? "done" : "failed", ctx);
        return false;
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

    private void execute(String[] cmd) throws IOException, InterruptedException, CmdException {
        for (int i = 0 ; i < cmd.length; i ++) {
            Util.log("cmd[%d] = '%s'", i, cmd[i]);
        }
        Runtime run = Runtime.getRuntime();
        Process pr = run.exec(cmd);

        String line;
        BufferedReader stderr = new BufferedReader(new InputStreamReader(pr.getErrorStream()), 512);
        while ((line = stderr.readLine()) != null) {
            Util.error(String.format("%s reports errors: %s", cmd[0], line));
            Util.flushErrors(this);
        }

        BufferedReader stdout = new BufferedReader(new InputStreamReader(pr.getInputStream()), 16);
        while ((line = stdout.readLine()) != null) {
            Util.error(String.format("%s reports to stdout: %s", cmd[0], line));
        }

        stderr.close();
        stdout.close();

        int result = pr.waitFor();

        if (result != 0) {
            throw new CmdException(result);
        }
    }

    private class CmdException extends Exception {
        public CmdException(int exitCode) {
            super(String.format("Command exited with %d code", exitCode));
        }
    }
}
