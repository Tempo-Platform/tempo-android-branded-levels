package com.tempoplatform.bl;

import android.annotation.SuppressLint;
import androidx.annotation.Nullable;
import android.os.Bundle;

public class RewardedActivity extends AdActivity {

    public static RewardedActivity instance;

    @SuppressLint(Constants.SET_JS_ENABLED)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        TempoUtils.Say("RewardedActivity.onCreate()");
        isInterstitial = false;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void addJSInterface() {
        webView.addJavascriptInterface(new RewardedWebAppInterface(createGooglePayLauncher(), this), Constants.ANDROID);
    }

    @Override
    protected void updateInstance() {
        instance = this;
    }

    @Override
    protected void closeThisActivityOnShowFail(String reason) {
        RewardedView.instanceWithLiveActivity.listener.onTempoAdShowFailed(reason);
        RewardedView.instanceWithLiveActivity.createMetric(Constants.METRIC_AD_SHOW_FAIL);
        RewardedView.instanceWithLiveActivity.activateInstance(false);
    }
}
