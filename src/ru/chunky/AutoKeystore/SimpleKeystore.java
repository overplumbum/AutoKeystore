package ru.chunky.AutoKeystore;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class SimpleKeystore {

    // ResponseCodes
    public static final int NO_ERROR = 1;
    public static final int LOCKED = 2;
    public static final int UNINITIALIZED = 3;
    public static final int SYSTEM_ERROR = 4;
    public static final int PROTOCOL_ERROR = 5;
    public static final int PERMISSION_DENIED = 6;
    public static final int KEY_NOT_FOUND = 7;
    public static final int VALUE_CORRUPTED = 8;
    public static final int UNDEFINED_ACTION = 9;
    public static final int WRONG_PASSWORD = 10;

    // States
    public enum State { UNLOCKED, LOCKED, UNINITIALIZED }

    private static final LocalSocketAddress sAddress = new LocalSocketAddress(
            "keystore", LocalSocketAddress.Namespace.RESERVED);

    private int mError = NO_ERROR;

    private SimpleKeystore() {}

    public static SimpleKeystore getInstance() {
        return new SimpleKeystore();
    }

    public State state() {
        execute('t');
        switch (mError) {
            case NO_ERROR: return State.UNLOCKED;
            case LOCKED: return State.LOCKED;
            case UNINITIALIZED: return State.UNINITIALIZED;
            default: throw new AssertionError(mError);
        }
    }

    private ArrayList<byte[]> execute(int code, byte[]... parameters) {
        mError = PROTOCOL_ERROR;

        for (byte[] parameter : parameters) {
            if (parameter == null || parameter.length > 65535) {
                return null;
            }
        }

        LocalSocket socket = new LocalSocket();
        try {
            socket.connect(sAddress);

            OutputStream out = socket.getOutputStream();
            out.write(code);
            for (byte[] parameter : parameters) {
                out.write(parameter.length >> 8);
                out.write(parameter.length);
                out.write(parameter);
            }
            out.flush();
            socket.shutdownOutput();

            InputStream in = socket.getInputStream();
            if ((code = in.read()) != NO_ERROR) {
                if (code != -1) {
                    mError = code;
                }
                return null;
            }

            ArrayList<byte[]> values = new ArrayList<byte[]>();
            while (true) {
                int i, j;
                if ((i = in.read()) == -1) {
                    break;
                }
                if ((j = in.read()) == -1) {
                    return null;
                }
                byte[] value = new byte[i << 8 | j];
                for (i = 0; i < value.length; i += j) {
                    if ((j = in.read(value, i, value.length - i)) == -1) {
                        return null;
                    }
                }
                values.add(value);
            }
            mError = NO_ERROR;
            return values;
        } catch (IOException e) {
            // ignore
        } finally {
            try {
                socket.close();
            } catch (IOException e) {}
        }
        return null;
    }
}
