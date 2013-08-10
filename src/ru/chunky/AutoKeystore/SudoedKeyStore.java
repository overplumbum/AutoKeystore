package ru.chunky.AutoKeystore;

import android.content.Context;
import android.preference.PreferenceManager;

import java.io.*;

public class SudoedKeyStore {
    private final Context ctx;

    public SudoedKeyStore(Context ctx) {
        this.ctx = ctx;
    }

    public void unlock(String passwd) throws Error {
        checkSudo();
        checkCode(executeKeyStoreSudoed('u', new String[]{passwd}));
    }

    public void lock() throws Error {
        checkSudo();
        checkCode(executeKeyStoreSudoed('l', new String[]{}));
    }

    private static String escapeShellArg(String s) {
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private int executeKeyStoreSudoed(char command, String[] args) throws Error {
        byte[] commandData = renderCommand(command, args);

        File cmd = new File(ctx.getApplicationInfo().dataDir, "lib/libkeystorecmd-executable.so");
        if (!Util.canExecute(cmd)) {
            execute(new String[]{
                    "su", "-c",
                    String.format("chmod 0555 %s", escapeShellArg(cmd.getAbsolutePath()))
            }, null, true);
        } else {
            Util.log("already executable");
        }

        return executeIntStatus(new String[]{
                "su", "-c",
                cmd.getAbsolutePath()
        }, commandData);
    }

    private byte[] renderCommand(char command, String[] args) throws Error {
        ByteArrayOutputStream dump = new ByteArrayOutputStream();
        try {
            dump.write(command);
            for (String arg : args) {
                byte[] rawArg = arg.getBytes();
                dump.write(rawArg.length >> 8);
                dump.write(rawArg.length);
                dump.write(rawArg);
            }
            dump.close();
        } catch (IOException e) {
            Util.error(e);
            throw new Error(R.string.suk_error_prepare);
        }
        return dump.toByteArray();
    }

    private void checkSudo() throws Error {
        String out = null;
        try {
            out = execute(new String[]{"sh", "-c", "command -v su"}, "".getBytes());
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            Util.error(e);
        }
        if (out == null || !out.endsWith("/su")) {
            throw new Error(R.string.suk_error_su_not_found);
        }
    }

    private static String execute(String[] cmd, byte[] stdin) throws Error {
        return execute(cmd, stdin, false);
    }

    private static String execute(String[] cmd, byte[] stdin, boolean ignore_IO_error) throws Error {
        Runtime run = Runtime.getRuntime();
        for (int i = 0 ; i < cmd.length; i ++) {
            Util.log("cmd[%d] = '%s'", i, cmd[i]);
        }
        try {
            Process pr = run.exec(cmd);

            if (stdin != null && stdin.length > 0) {
                OutputStream writer = pr.getOutputStream();
                writer.write(stdin);
                writer.close();
            }

            String line;
            BufferedReader stderr = new BufferedReader(new InputStreamReader(pr.getErrorStream()), 512);
            while ((line = stderr.readLine()) != null) {
                Util.error(String.format("%s reports errors: %s", cmd[0], line));
            }

            BufferedReader stdout = new BufferedReader(new InputStreamReader(pr.getInputStream()), 16);
            line = stdout.readLine();

            stderr.close();
            stdout.close();

            int exitCode = pr.waitFor();
            if (exitCode == 255) {
                throw new Error(R.string.suk_su_declined);
            } else if (exitCode == 51) {
                Util.error("suk_unable_to_connect");
                throw new Error(R.string.suk_unable_to_connect);
            } else if (exitCode == 52) {
                Util.error("suk_keystore_service_result_unavailable");
                throw new Error(R.string.suk_keystore_service_result_unavailable);
            } else if (exitCode == 53) {
                Util.error("suk_too_big_command");
                throw new Error(R.string.suk_too_big_command);
            } else if (exitCode == 54) {
                Util.error("suk_failed_to_change_userid");
                throw new Error(R.string.suk_failed_to_change_userid);
            } else if (exitCode != 0) {
                Util.error(String.format("suk_unknown_error#%d", exitCode));
                throw new Error(R.string.suk_unknown_error, exitCode);
            }

            return line;
        } catch (IOException e) {
            if (!ignore_IO_error) {
                Util.error(e);
                throw new Error(R.string.suk_io_error, cmd[0]);
            }
            return  null;
        } catch (InterruptedException e) {
            Util.error(e);
            throw new Error(R.string.suk_interupted, cmd[0]);
        }
    }

    private int executeIntStatus(String[] cmd, byte[] stdin) throws Error {
        String line = execute(cmd, stdin);
        if (line == null) {
            throw new Error(R.string.suk_emptry_result, cmd[0]);
        }

        return Integer.parseInt(line);
    }

    private static boolean isStatusWrongPassword(int code) {
        return code >= 10 && code < 20;
    }

    private void checkCode(int code) throws Error {
        if (isStatusWrongPassword(code)) {
            Error exc = new Error(R.string.status_10, code - 9);
            exc.is_wrong_password = true;
            throw exc;
        }
        switch (code) {
            case SimpleKeystore.NO_ERROR:
                return;
            case SimpleKeystore.UNINITIALIZED:
                throw new Error(R.string.status_uninitialized);
            case SimpleKeystore.PERMISSION_DENIED:
                throw new Error(R.string.status_sudo_old);
            default:
                Util.error(String.format("Unknown error #%d", code));
                throw new Error(R.string.suk_unknown_code, code);
        }
    }

    static class Error extends Exception {
        private final int msg_id;
        private final Object[] msg_args;

        public boolean is_wrong_password = false;

        public Error(int msg_id, Object... msg_args) {
            super();
            this.msg_id = msg_id;
            this.msg_args = msg_args;
        }

        public String getMsg(Context ctx) {
            switch (msg_args.length) {
                case 0: return ctx.getString(msg_id);
                case 1: return ctx.getString(msg_id, msg_args[0]);
                case 2: return ctx.getString(msg_id, msg_args[0], msg_args[1]);
                case 3: return ctx.getString(msg_id, msg_args[0], msg_args[1], msg_args[2]);
                case 4: return ctx.getString(msg_id, msg_args[0], msg_args[1], msg_args[2], msg_args[3]);
                case 5: return ctx.getString(msg_id, msg_args[0], msg_args[1], msg_args[2], msg_args[3], msg_args[4]);
                default:
                    Util.error("getMsg misused");
                    return "/* getMsg misused */";
            }
        }
    }
}
