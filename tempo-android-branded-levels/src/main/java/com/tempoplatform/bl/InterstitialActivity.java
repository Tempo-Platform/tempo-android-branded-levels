package com.tempoplatform.bl;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.Nullable;

public class InterstitialActivity extends AdActivity {

    public static InterstitialActivity instance;

    @SuppressLint(Constants.SET_JS_ENABLED)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        TempoUtils.Say("InterstitialActivity.onCreate()");
        isInterstitial = true;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void addJSInterface() {
        webView.addJavascriptInterface(new InterstitialWebAppInterface(createGooglePayLauncher(), this), Constants.ANDROID);
    }

    @Override
    protected void updateInstance() {
        instance = this;
    }

    @Override
    protected void closeThisActivityOnShowFail(String reason) {
        InterstitialView.instanceWithLiveActivity.listener.onTempoAdShowFailed(reason);
        InterstitialView.instanceWithLiveActivity.createMetric(Constants.METRIC_AD_SHOW_FAIL);
        InterstitialView.instanceWithLiveActivity.activateInstance(false);
    }
}
