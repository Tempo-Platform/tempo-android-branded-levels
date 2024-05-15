package com.tempoplatform.tempo_branded_levels;

import android.util.Log;

public class RoyalSampler {

    public static String HelloNurse = "Hellooooooooo, nurse!";

    public static void SaySomething() {
        Log.d("TEST", "THis is my message: " + HelloNurse);
    }

    public static int AddMe(int addend1, int addend2) {
        int sum = addend1 + addend2;
        return sum;
    }
}
