package com.tempoplatform.bl;

import android.content.Intent;
import android.webkit.JavascriptInterface;

import com.stripe.android.googlepaylauncher.GooglePayLauncher;

public abstract class AdWebAppInterface {

    private GooglePayLauncher googlePayLauncher;

    public AdWebAppInterface(GooglePayLauncher googlePayLauncher) {
        this.googlePayLauncher = googlePayLauncher;
    }

    /**
     * Displays GPay button for purchasing using Google Pay
     */
    @JavascriptInterface
    public void launchGooglePay(String clientSecret) {
        int lastFour = clientSecret.length() - 4;
        if (lastFour < 0) {
            TempoUtils.Say("Launching Google Pay. Client secret is unusually short (less than 4 chars).");
        } else {
            TempoUtils.Say("Launching Google Pay, client secret ending %s".format(
                    clientSecret.substring(lastFour)));
        }
        googlePayLauncher.presentForPaymentIntent(clientSecret);
    }

    /**
     * Creates intent to deliver some data to someone else
     */
    @JavascriptInterface
    public void nativeShare(String title, String url) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TITLE, title);
        sendIntent.putExtra(Intent.EXTRA_TEXT, url);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivityWithShareIntent(shareIntent);
    }

    /**
     * Starts activity with passed 'share data' intent object
     */
    protected abstract void startActivityWithShareIntent(Intent intent);

    /**
     *  Sends event name to trigger creation of metric object
     */
    @JavascriptInterface
    public abstract void logEvent(String eventType);

    /**
     * Close the currently showing ad view
     */
    @JavascriptInterface
    public abstract void closeAd();

}
