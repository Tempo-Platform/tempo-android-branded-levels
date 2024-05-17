package com.tempoplatform.bl;

import static androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale;

import com.stripe.android.googlepaylauncher.GooglePayEnvironment;
import java.text.SimpleDateFormat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Date;

public class TempoUtils {

    /**
     * Returns "INTERSTITIAL" or "REWARDED" depending on boolean input.
     * Mainly used for identifying metrics during testing/debugging
     */
    public static String typeCheck(Boolean is_interstitial)  {
        return is_interstitial ? "INTERSTITIAL" : "REWARDED";
    }

    /**
     *  Outputs a summary of metric details for testing purposes.
     *  Change displayed properties as you see best fit.
     */
    public static void metricOutput(Metric metric) {
        TempoUtils.Say("created [" + metric.metric_type + "] " +
                TempoUtils.typeCheck(metric.is_interstitial), true);
    }

    /**
     * Takes Unix timestamp and returns DT formatted for readability
     */
    public static String getFormattedDatetimeLong(Long unixTimestamp) {
        SimpleDateFormat dtFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
        return dtFormat.format(new Date(unixTimestamp));
    }

    /**
     * Converts Unix timestamp string and returns DT formatted for readability.
     * Returns 1/1/1970 if string is not valid
     */
    public static String getFormattedDatetimeString(String unixTimestampString) {
        // Convert first
        long dateTimeLong = 0;
        try {
            dateTimeLong = Long.parseLong(unixTimestampString);
        } catch (NumberFormatException e) {
            TempoUtils.Shout("ERR: The string [" + unixTimestampString + "] was not a valid long value.", true);
        }

        // Output result
        SimpleDateFormat dtFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
        return dtFormat.format(new Date(dateTimeLong));
    }

    /**
     * Log for URGENT output with 'TEST_LOG' tag stamp - not to be used in production
     */
    public static void Shout(String msg) {
        if(Constants.isTesting) {
            Log.e(Constants.TEST_LOG, msg);
        }
    }

    /**
     * Log for URGENT output with 'TEST_LOG' tag stamp, option of toggling production output or off completely
     */
    public static void Shout(String msg, boolean absoluteDisplay) {
        if (absoluteDisplay) {
            Log.e(Constants.TEST_LOG, msg);
        } else if (Constants.isTesting) {
            // Nothing - muted
        }
    }

    /**
     * Log for general output with 'TEST_LOG' tag stamp, never shows in production
     */
    public static void Say(String msg) {
        if(Constants.isTesting) {
            Log.d(Constants.TEST_LOG, msg);
        }
    }

    /**
     * Log for general test output with 'TEST_LOG' tag stamp, option of toggling production output or off completely
     */
    public static void Say(String msg, boolean absoluteDisplay) {
        if (absoluteDisplay) {
            Log.i(Constants.TEST_LOG, msg);
        } else if (Constants.isTesting) {
            // Nothing - muted
        }
    }

    /**
     * Log for WARNING output with 'TEST_LOG' tag stamp, never shows in production
     */
    public static void Warn(String msg) {
        if(Constants.isTesting) {
            Log.w(Constants.TEST_LOG, msg);
        }
    }

    /**
     * Log for WARNING test output with 'TEST_LOG' tag stamp, option of toggling production output or off completely
     */
    public static void Warn(String msg, boolean absoluteDisplay) {
        if (absoluteDisplay) {
            Log.w(Constants.TEST_LOG, msg);
        } else if (Constants.isTesting) {
            // Nothing - muted
        }
    }

    /**
     * Returns correct ads-api URL depending on current environment
     */
    protected static String getAdsApiUrl(Boolean baseOnly) {
        if(Constants.isProd) {
            if(baseOnly)
            {
                return Constants.ADS_API_URL_BASE_PROD;
            }
            else{
                return Constants.ADS_API_URL_PROD;
            }
        }
        else{
            if(baseOnly)
            {
                return Constants.ADS_API_URL_BASE_DEV;
            }
            else{
                return Constants.ADS_API_URL_DEV;
            }
        }
    }

    /**
     * Returns correct metrics URL depending on current environment
     */
    protected static String getMetricsUrl(Boolean baseOnly) {
        if(Constants.isProd) {
            if(baseOnly)
            {
                return Constants.METRIC_URL_BASE_PROD;
            }
            else{
                return Constants.METRIC_URL_PROD;
            }
        }
        else{
            if(baseOnly)
            {
                return Constants.METRIC_URL_BASE_DEV;
            }
            else{
                return Constants.METRIC_URL_DEV;
            }
        }
    }

    /**
     * Returns correct Interstitial Ads URL depending on current environment
     */
    protected static String getInterstitialUrl() {
        if(TempoTesting.isTestingDeployVersion)
        {
            if(TempoTesting.deployVersionId != null) {
                String deployPreviewUrl = Constants.URL_DEPLOY_PREVIEW_PREFIX +
                        TempoTesting.deployVersionId +
                        Constants.URL_DEPLOY_PREVIEW_APPENDIX +
                        Constants.URL_APNX_INT;

                //TempoUtils.Shout("DeployPreview (I) URL = " + deployPreviewUrl);
                return deployPreviewUrl;
            }
            else {
                TempoUtils.Shout("DeployVersion is null");
            }
        }

        if(Constants.isProd) {
            return Constants.INTERSTITIAL_URL_PROD;
        }
        else {
            return Constants.INTERSTITIAL_URL_DEV;
        }
    }

    /**
     * Returns correct Rewarded Ads URL depending on current environment
     */
    protected static String getRewardedUrl() {
        if(TempoTesting.isTestingDeployVersion)
        {
            if(TempoTesting.deployVersionId != null) {
                String deployPreviewUrl = Constants.URL_DEPLOY_PREVIEW_PREFIX +
                        TempoTesting.deployVersionId +
                        Constants.URL_DEPLOY_PREVIEW_APPENDIX +
                        Constants.URL_APNX_REW;

                //TempoUtils.Shout("DeployPreview (R) URL = " + deployPreviewUrl);
                return deployPreviewUrl;
            }
        }

        if(Constants.isProd) {
            return Constants.REWARDED_URL_PROD;
        }
        else{
            return Constants.REWARDED_URL_DEV;
        }
    }

    /**
     * Returns full ads URL depending on current environment/adType
     */
    protected static String getFullWebUrl(boolean isInterstitial, String campaignId, String urlSuffix) {

        String webAdUrl = (isInterstitial ? TempoUtils.getInterstitialUrl() : TempoUtils.getRewardedUrl()) + campaignId;
        if(urlSuffix != null && !urlSuffix.isEmpty()) {
            // No '/'  as the backend may handle suffix with query parameters (i.e. ?country=US rather than /country/US)
            webAdUrl += urlSuffix;
        }
        TempoUtils.Warn("WEBSITE (url): " + webAdUrl);
        return webAdUrl;
    }

    /**
     *  Checks if custom campaigns is in use, otherwise returns the campaign ID received via standard process
     */
    protected static String checkForTestCampaign(String campaignId) {
        if(TempoTesting.customCampaignId == null || TempoTesting.customCampaignId.trim().isEmpty()) {
            return campaignId;
        }

        if(TempoTesting.customCampaignId != null && !TempoTesting.customCampaignId.trim().isEmpty() && TempoTesting.isTestingCustomCampaigns) {
            return TempoTesting.customCampaignId;
        }

        return campaignId;
    }

    /**
     * Returns publishable key based on STRIPE_PROD environment
     */
    protected static String getStripeKey() {
        if(Constants.isStripeProd) {
            return Constants.STRIPE_LIVE_KEY;
        }
        else {
            return Constants.STRIPE_TEST_KEY;
        }
    }

    /**
     * Returns GooglePayEnvironment value based on STRIPE_PROD environment
     */
    protected static GooglePayEnvironment getGooglePayEnvironment(){
        if(Constants.isStripeProd) {
            return GooglePayEnvironment.Production;
        }
        else {
            return GooglePayEnvironment.Test;
        }
    }
}
