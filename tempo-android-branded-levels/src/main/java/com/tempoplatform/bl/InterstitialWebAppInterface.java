package com.tempoplatform.bl;

import android.content.Intent;
import android.webkit.JavascriptInterface;
import com.stripe.android.googlepaylauncher.GooglePayLauncher;

public class InterstitialWebAppInterface extends AdWebAppInterface {

    private final InterstitialActivity activity;

    public InterstitialWebAppInterface(GooglePayLauncher googlePayLauncher, InterstitialActivity activity) {
        super(googlePayLauncher);
        this.activity = activity;
    }

    @Override
    @JavascriptInterface
    public void closeAd() {
        InterstitialView.instanceWithLiveActivity.close();
    }

    @Override
    protected void startActivityWithShareIntent(Intent shareIntent) {
        activity.startActivity(shareIntent);
    }

    @Override
    @JavascriptInterface
    public void logEvent(String eventType) {
        if(InterstitialView.instanceWithLiveActivity != null)
        {
            InterstitialView.instanceWithLiveActivity.createMetric(eventType);
        } else {
            TempoUtils.Shout(eventType + " not logged! InterstitialView.instanceWithLiveActivity is null");
        }
    }
}
