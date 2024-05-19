package com.tempoplatform.bl;

import android.util.Log;

public class Utils {

    public static void Shout(String msg) {
        Log.e(Constants.TEST_LOG, msg);
    }
    public static void Say(String msg) {
        Log.d(Constants.TEST_LOG, msg);
    }
    public static void Warn(String msg) {
        Log.d(Constants.TEST_LOG, msg);
    }
}
