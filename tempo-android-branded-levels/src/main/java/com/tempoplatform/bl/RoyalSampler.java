package com.tempoplatform.bl;

import android.util.Log;
import com.tempoplatform.bl.Constants;
import androidx.annotation.Keep;

@Keep
public class RoyalSampler {

    public static String HelloNurse = "Hellooooooooo, nurse!!";

    public static void SaySomething() {
        Log.d("TEST", "THis is my message: " + HelloNurse + " | " + Constants.ADS_API_APP_ID );
    }

    public static int AddMe(int addend1, int addend2) {
        int sum = addend1 + addend2;
        return sum;
    }
}
