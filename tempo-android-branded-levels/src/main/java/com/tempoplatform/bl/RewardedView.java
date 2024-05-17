package com.tempoplatform.bl;

import android.content.Context;

public class RewardedView extends AdView {

    public static RewardedView instanceWithLiveActivity;

    public RewardedView(String appId, Context context) {
        super(appId, context);
        this.isInterstitial = false;
    }

    @Override
    protected void activateInstance(boolean activate) {
        if(activate)
        {
            instanceWithLiveActivity = this;
        } else {
            instanceWithLiveActivity = null;
            if (RewardedActivity.instance != null && !RewardedActivity.instance.isFinishing()) {
                RewardedActivity.instance.finish();
            }
        }
    }
}