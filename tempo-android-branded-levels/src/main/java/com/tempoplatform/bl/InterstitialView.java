package com.tempoplatform.bl;

import android.content.Context;

public class InterstitialView extends AdView {

    public static InterstitialView instanceWithLiveActivity;

    public InterstitialView(String appId, Context context) {
        super(appId, context);
        this.isInterstitial = true;
    }

    @Override
    protected void activateInstance(boolean activate) {
        if(activate)
        {
            instanceWithLiveActivity = this;
        } else {
            instanceWithLiveActivity = null;
            if (InterstitialActivity.instance != null && !InterstitialActivity.instance.isFinishing()) {
                InterstitialActivity.instance.finish();
            }
        }
    }
}