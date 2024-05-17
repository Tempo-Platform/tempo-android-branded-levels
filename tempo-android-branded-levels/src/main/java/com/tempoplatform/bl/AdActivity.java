package com.tempoplatform.bl;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.googlepaylauncher.GooglePayLauncher;
import com.tempoplatform.tempo_branded_levels.R;

import org.jetbrains.annotations.NotNull;

public abstract class AdActivity extends ComponentActivity {

    protected WebView webView = null;
    protected String campaignId;
    private boolean backButtonEnabled = true; // assumed true when starting
    protected boolean isInterstitial;

    @SuppressLint(Constants.SET_JS_ENABLED)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Disable back button and remove bottom nav buttons from view
        setBackButtonEnabled(false);

        // Get DecorView
        View decorView = getWindow().getDecorView();
        if(decorView == null) {
            closeThisActivityOnShowFail("DecorView is null");
            return;
        }

        // Update visibility options
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        // Set Tempo activity layout
        setContentView(R.layout.activity_web_view);

        // Update the static instance reference in derived class
        updateInstance();

        // Init Stripe
        PaymentConfiguration.init(this, TempoUtils.getStripeKey());

        // Find and display webview
        webView = findViewById(R.id.webview);
        if(webView == null) {
            closeThisActivityOnShowFail("WebView is null");
            return;
        }
        WebView.setWebContentsDebuggingEnabled(false);  // Set this to true for debugging.
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setUserAgentString(Constants.USER_AGENT_STRING);

        // Fetch and confirm valid Campaign ID
        campaignId = getIntent().getExtras().getString(Constants.INTENT_EXTRAS_CAMPAIGN_ID_KEY);
        if(campaignId == null || campaignId.isEmpty()) {
            closeThisActivityOnShowFail("Campaign ID is null");
            return;
        }

        addJSInterface();

        webView.loadUrl(TempoUtils.getFullWebUrl(isInterstitial, campaignId, getIntent().getExtras().getString(Constants.INTENT_EXTRAS_URL_SUFFIX_KEY)));
    }

    /**
     * Returns a newly instantiated GPayLauncher object
     */
    protected final GooglePayLauncher createGooglePayLauncher() { // TODO: Correct use of 'final' here?
        return new GooglePayLauncher(
                this, // TODO: Could this be problematic?
                new GooglePayLauncher.Config(
                        TempoUtils.getGooglePayEnvironment(),
                        Constants.GPAY_MERCHANT_COUNTRY,
                        Constants.GPAY_MERCHANT_NAME
                ),
                this::onGooglePayReady,
                this::onGooglePayResult
        );
    }

    /**
     * Behaviour after determining whether GPay is available and ready on this device
     */
    private void onGooglePayReady(boolean isReady) {
        if (isReady) {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(webView, url);
                    webView.evaluateJavascript(Constants.JS_PAGE_FINISHED, null);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    TempoUtils.Shout("TempoSDK: Error setting WebViewClient: " + description, true);
                }
            });
        }
    }

    /**
     * Behaviour when GPay process returns result
     */
    private void onGooglePayResult(@NotNull GooglePayLauncher.Result result) {
        if (result instanceof GooglePayLauncher.Result.Completed) {
            TempoUtils.Say("TempoSDK: Payment Succeeded", true);
            webView.loadUrl(Constants.THANKS_URL);
        } else if (result instanceof GooglePayLauncher.Result.Canceled) {
            TempoUtils.Say("TempoSDK: User canceled Google Pay", true);
        } else if (result instanceof GooglePayLauncher.Result.Failed) {
            Throwable error = ((GooglePayLauncher.Result.Failed) result).getError();
            TempoUtils.Shout("TempoSDK: Payment Failed! " + error.toString(), true);
            Toast.makeText(this, "Payment Failed: " + error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Enables/disables back button
     */
    public void setBackButtonEnabled(boolean enabled) {
        backButtonEnabled = enabled;
    }

    @Override
    protected void onStop() {
        super.onStop();
        TempoUtils.Say("STOPPING - " + TempoUtils.typeCheck(isInterstitial) + " ACTIVITY", false);

        // Re-enable back button
        setBackButtonEnabled(true);
    }

    @Override
    protected void onDestroy() {
        TempoUtils.Say("DESTROYING - " + TempoUtils.typeCheck(isInterstitial) + " ACTIVITY", false);
        super.onDestroy();;
    }

    @Override
    public void onBackPressed() {
        if (backButtonEnabled) {
            // Handle back button press as usual
            super.onBackPressed();
        } else {
            TempoUtils.Say("Back button disabled during ads", false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TempoUtils.Say("RESUMING - " + TempoUtils.typeCheck(isInterstitial) + " ACTIVITY", false);
        //TempoUtils.Say("RESUMING - ACTIVITY [BASE]", true);
        setBackButtonEnabled(false);
    }

    /**
     * Add object ot allow Java object's methods to be accessed from JavaScript/React
     */
    protected abstract void addJSInterface();

    /**
     * Updates extended class instance
     */
    protected abstract void updateInstance();

    /**
     * Closes this adtype's activity
     */
    protected abstract void closeThisActivityOnShowFail(String reason);



}
