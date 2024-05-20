package com.tempoplatform.bl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.IOException;

public class GetGAIDTask extends AsyncTask<String, Integer, String> {

    private final AdIdListener adIdListener;

    @SuppressLint("StaticFieldLeak")
    private final Context context;

    public GetGAIDTask(Context context, AdIdListener adIdListener) {
        this.context = context;
        this.adIdListener = adIdListener;
    }

    @Override
    protected String doInBackground(String... strings) {

        AdvertisingIdClient.Info adInfo;
        try {
            adInfo = AdvertisingIdClient.getAdvertisingIdInfo(this.context.getApplicationContext());
            // check if user has opted out of tracking
            if (adInfo.isLimitAdTrackingEnabled())  {
                return BridgeRef.GAID_NONE;
            }
            return adInfo.getId();
        } catch (IOException | GooglePlayServicesNotAvailableException |
                 GooglePlayServicesRepairableException e) {
            return BridgeRef.GAID_NONE;
        }
    }

    @Override
    protected void onPostExecute(String s) {
        adIdListener.sendAdId(s);
    }
}