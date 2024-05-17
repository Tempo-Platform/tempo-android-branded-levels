package com.tempoplatform.bl;

import android.content.Intent;
import android.webkit.JavascriptInterface;

import com.stripe.android.googlepaylauncher.GooglePayLauncher;

public class RewardedWebAppInterface extends AdWebAppInterface {

    private final RewardedActivity activity;

    public RewardedWebAppInterface(GooglePayLauncher googlePayLauncher, RewardedActivity activity) {
        super(googlePayLauncher);
        this.activity = activity;
    }

    @Override
    @JavascriptInterface
    public void closeAd() {
        RewardedView.instanceWithLiveActivity.close();
    }

    @Override
    protected void startActivityWithShareIntent(Intent shareIntent) {
        activity.startActivity(shareIntent);
    }

    @Override
    @JavascriptInterface
    public void logEvent(String eventType) {
        if(RewardedView.instanceWithLiveActivity != null)
        {
            RewardedView.instanceWithLiveActivity.createMetric(eventType);
        } else {
            TempoUtils.Shout(eventType + " not logged! RewardedView.instanceWithLiveActivity is null");
        }
    }
}
