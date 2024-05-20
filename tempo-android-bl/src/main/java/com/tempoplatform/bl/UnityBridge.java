package com.tempoplatform.bl;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class UnityBridge extends Application {

    public UnityCallbacks unityCallbacks;
    public Profile profile;

    public UnityBridge(UnityCallbacks callback, Context context) {
        Log.d(BridgeRef.LOG, "UnityBridge initialised");
        unityCallbacks = callback;
        unityCallbacks.onInit();
    }

    /**
     *  Allows Unity to create Profile (location, ad ID etc)
     */
    public void createProfile(Context context) {
        // Instantiate new object and pass through callback delegates
        profile = new Profile(context, unityCallbacks);

        // Get Ad ID
        profile.requestAdID(context);

        // Check consent status
        BridgeRef.LocationConsent lc = Profile.getLocationConsent(context);
        unityCallbacks.onConsentTypeRequest(lc.toString());
        Log.d(BridgeRef.LOG, "Consent is: " + lc);

        // Find country code
        String cc = profile.getIso1366CountryCode();
        unityCallbacks.onCountryCodeRequest(cc == null ? "" : cc);

        // Get location data
        profile.getLocationData(context, lc);
    }

    /**
     *  Allows Unity to request precise location authorisation
     *  TODO: Currently using Unity LocationServices due to errors, delete/fix?
     */
    public void checkPrecise(Context context) {
        profile.getLocationData(context, BridgeRef.LocationConsent.PRECISE);
    }

    /**
     *  Allows Unity to request coarse location authorisation
     *  TODO: Currently using Unity LocationServices due to errors, delete/fix?
     */
    public void checkGeneral(Context context) {
        profile.getLocationData(context, BridgeRef.LocationConsent.GENERAL);
    }
}
