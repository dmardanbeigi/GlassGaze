package com.glassgaze;

import android.util.Log;

import java.security.MessageDigest;
import java.util.Arrays;

public class Utils {

    private final static String TAG = "android-btxfr/Utils";

    public static byte[] intToByteArray(int a) {
        byte[] ret = new byte[4];
        ret[3] = (byte) (a & 0xFF);
        ret[2] = (byte) ((a >> 8) & 0xFF);
        ret[1] = (byte) ((a >> 16) & 0xFF);
        ret[0] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }

    public static int GetIndicator(byte[] a)
    {
        byte[] ret = new byte[4];
        java.lang.System.arraycopy(a, 0, ret, 0, 4);
        return Utils.byteArrayToInt_reverse(ret);
    }
    public static int GetX(byte[] a)
    {
        byte[] ret = new byte[4];
        java.lang.System.arraycopy(a, 4, ret, 0, 4);
        return Utils.byteArrayToInt_reverse(ret);
    }

    public static int GetY(byte[] a)
    {
        byte[] ret = new byte[4];
        java.lang.System.arraycopy(a, 8, ret, 0, 4);
        return Utils.byteArrayToInt_reverse(ret);
    }

    public static int byteArrayToInt(byte[] b)
    {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }
    public static int byteArrayToInt_reverse(byte[] b)
    {
        return   b[0] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16 |
                (b[3] & 0xFF) << 24;
    }

    public static boolean digestMatch(byte[] digestData1, byte[] digestData2) {
        return Arrays.equals(digestData1, digestData2);
    }

    public static byte[] getDigest(byte[] imageData) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            return messageDigest.digest(imageData);
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
            throw new UnsupportedOperationException("MD5 algorithm not available on this device.");
        }
    }


}
