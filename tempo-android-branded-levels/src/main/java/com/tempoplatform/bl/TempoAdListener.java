package com.tempoplatform.bl;

/**
 * A Tempo ad listener to listen for ad events.
 */
public class TempoAdListener {

    /**
     * Called when an ad is successfully fetched.
     */
    public void onTempoAdFetchSucceeded() {
        // Default is to do nothing.
        TempoUtils.Say("TempoAdListener.onTempoAdFetchSucceeded()");
    }

    /**
     * Called when an ad fetch fails.
     */
    public void onTempoAdFetchFailed(String reason) {
        reason = reason == null || reason.isEmpty() ? "" : reason;
        // Default is to do nothing.
        TempoUtils.Say("TempoAdListener.onTempoAdFetchFailed(" + reason + ")");
    }

    /**
     * Called when an ad goes full screen.
     */
    public void onTempoAdDisplayed() {
        // Default is to do nothing.
        TempoUtils.Say("TempoAdListener.onTempoAdDisplayed()");
    }

    /**
     * Called when an ad is closed
     */
    public void onTempoAdClosed() {
        // Default is to do nothing.
        TempoUtils.Say("TempoAdListener.onTempoAdClosed()");
    }

    /**
     * Called when an ad show fails
     */
    public void onTempoAdShowFailed(String reason) {
        // Default is to do nothing.
        reason = reason == null || reason.isEmpty() ? "" : reason;
        TempoUtils.Say("TempoAdListener.onTempoAdClosed(" + reason + ")");
    }

    /**
     * Called when an a request is made for the mediation adapter's version
     * Includes parameter to the update SDK version in adapter script
     */
    public String getTempoAdapterVersion() {  return null;  }

    /**
     * Called when an a request is made for the mediation adapter's platform type
     */
    public String getTempoAdapterType() { return null; }

    /**
     * Called when an a request is made for userConsent
     */
    public Boolean hasUserConsent() { return null;  }

}
