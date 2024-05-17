package com.tempoplatform.bl;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.webkit.WebView;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class AdView {

    protected String currentUUID;
    protected String currentCampaignId;
    protected String currentCountryCode;
    protected String currentPlacementId;
    protected Double currentCPM;
    protected String currentAdapterType;
    protected Boolean currentConsent = null;
    protected String currentConsentType = null; // TODO: We aren't collecting this value yet
    protected String currentUrlSuffix = null;

    protected TempoAdListener listener = null;
    protected String appId;
    protected String adId;
    protected Context context;
    protected WebView preLoadingWebView = null;
    protected TempoProfile tempoProfile;

    protected boolean isInterstitial;

    private final BlockingQueue<Runnable> methodQueue = new LinkedBlockingQueue<>();

    public AdView(String appId, Context context) {
        this.appId = appId;
        this.context = context;
        new GetGAIDTask(this.context).execute();
        TempoBackupData.initCheck(context);
    }

    /**
     * Public method for mediation adapters to load ad content
     */
    public void loadAd(Context context, TempoAdListener listener, Float cpmFloor, String placementId) {

        // If 'adId' has not been defined yet it is placed in a queue until AdId has a value
        synchronized (this) {
            if (adId == null) {
                methodQueue.offer(() -> loadAd(context, listener, cpmFloor, placementId));
                return;
            }
        }

        // Update local properties
        this.listener = listener;
        this.currentCPM = Double.valueOf(cpmFloor);
        this.currentPlacementId = placementId;
        this.currentCountryCode = TempoProfile.getIso1366CountryCode();
        this.context = context;
        this.currentUUID = UUID.randomUUID().toString();
        this.currentAdapterType = this.listener.getTempoAdapterType();
        this.currentConsent = this.listener.hasUserConsent();
        this.preLoadingWebView = new WebView(context);

        // Update adapter variable in Metrics object
        Metric.capturedAdapterVersion = listener.getTempoAdapterVersion();
        TempoUtils.Say("TempoSDK: " + TempoUtils.typeCheck(isInterstitial) + " loadAd()", true);
        TempoUtils.Say( "TempoSDK: [SDK]" + Constants.SDK_VERSION + "/[ADAP]" + Metric.capturedAdapterVersion + " | AppID: " + this.appId, true);

        // Get LocationData consent type - the CURRENT consent type will determine how this ad session is handled
        Constants.LocationConsent lc = TempoProfile.getLocationConsent(context);

        // Create temp locData for ads request task
        LocationData adLocData = new LocationData();

        // No location consent allowed - treat campaign with no location specifics
        if(lc == Constants.LocationConsent.NONE) {
            TempoProfile.locDataState = TempoProfile.LocationState.UNAVAILABLE;
            TempoProfile.locData = new LocationData();
        } else {
            // Clone LocData for temp adLocData if needed
            try {
                adLocData = getAdLocData();
            } catch (CloneNotSupportedException e) {
                TempoUtils.Warn("TempoSDK: cloning LocData failed - using new LocationData()");
            }
        }

        // Request ad from backend
        getAdUsingRetrofit(this.currentUUID, this.adId, this.appId, this.currentCPM, this.isInterstitial, Constants.SDK_VERSION,
                Metric.capturedAdapterVersion, this.currentCountryCode, this.currentAdapterType, adLocData);

        // Get Location details (will update retrospectively if applies)
        tempoProfile = new TempoProfile(isInterstitial, context, this);
        tempoProfile.getLocationData(context, lc, this);

        // Send metric
        createMetric(Constants.METRIC_AD_LOAD_REQUEST);
    }

    /**
     *  Returns latest instance of location data (including any saved from previous sessions)
     */
    private LocationData getAdLocData() throws CloneNotSupportedException {

        // If no static data, check backups and return if not null
        if (TempoProfile.locData == null) {
            LocationData ld = TempoBackupData.loadFromFile(context);
            if (ld != null) {
                if(ld.country_code != null && !ld.country_code.isEmpty()) {
                    //TempoUtils.Say("Updating countryCode: " + (currentCountryCode == null ? "NULL" : currentCountryCode) + " => " + ld.country_code);
                    currentCountryCode = ld.country_code;
                }
                else{
                    TempoUtils.Shout("CountryCode NOT updated due to null/empty value, remains: " + (currentCountryCode == null ? "NULL" : currentCountryCode));
                }
                return ld;
            }
        }
        // ...or clone the static instance and return cloned object
        else {
            return (LocationData) TempoProfile.locData.clone();
        }

        return new LocationData();
    }

    /**
     * Public method for mediation adapters to close current ad instance
     */
    public void close() {
        TempoUtils.Say("TempoSDK: Ad Close Called", true);
        createMetric(Constants.METRIC_AD_CLOSE);
        currentCampaignId = null;
        this.listener.onTempoAdClosed();
        activateInstance(false);
    }

    /**
     * Public method for mediation adapters to show/play loaded ad content
     */
    public void showAd() {

        TempoUtils.Say("TempoSDK: " + TempoUtils.typeCheck(isInterstitial) + " showAd()", true);

        // Update to see if consent status changed
        Constants.LocationConsent lc = TempoProfile.getLocationConsent(context);
        tempoProfile.getLocationData(context, lc, this);

        createMetric(Constants.METRIC_AD_SHOW_ATTEMPT);
        if (currentCampaignId != null) {
            createMetric(Constants.METRIC_AD_SHOW);
            Intent intent = createAdIntent(isInterstitial);
            intent.putExtra(Constants.INTENT_EXTRAS_CAMPAIGN_ID_KEY, currentCampaignId);
            intent.putExtra(Constants.INTENT_EXTRAS_URL_SUFFIX_KEY, currentUrlSuffix);
            context.startActivity(intent);
            this.listener.onTempoAdDisplayed();
            activateInstance(true);
        } else {
            activateInstance(false);
            TempoUtils.Warn("TempoSDK: Call loadAd() before calling showAd(). Also check for Ad availability", true);
        }
    }

    /**
     * Creates and returns intent instance of extended AdActivity class
     */
    private Intent createAdIntent(boolean isInterstitial) {
        if(isInterstitial) {
            return new Intent(context, InterstitialActivity.class);
        } else {
            return new Intent(context, RewardedActivity.class);
        }
    }

    /**
     * Activates/deactivates extended class objects singleton instance
     */
    protected abstract void activateInstance(boolean activate);

    /**
     * Creates a method instance using type parameter and current class values defined earlier
     */
    public void createMetric(String metricType) {
        String bundleId = context.getPackageName();
        String os = "Android " + Build.VERSION.RELEASE;
        Metric metric = new Metric(
                metricType,
                adId,
                appId,
                bundleId,
                currentCampaignId,
                currentUUID,
                os,
                isInterstitial,
                currentCountryCode,
                currentPlacementId,
                currentCPM,
                currentAdapterType,
                currentConsent,
                currentConsentType
        );

        Metric.addMetricToList(metric, context);
    }

    /**
     * Asynchronous task used to get AdId (if activated) from device
     */
    protected class GetGAIDTask extends AsyncTask<String, Integer, String> {

        private Context context;

        public GetGAIDTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... strings) {

            AdvertisingIdClient.Info adInfo;
            try {
                adInfo = AdvertisingIdClient.getAdvertisingIdInfo(this.context.getApplicationContext());
                if (adInfo.isLimitAdTrackingEnabled()) // check if user has opted out of tracking
                    return Constants.GAID_NONE;
            } catch (IOException | GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException e) {
                return Constants.GAID_NONE;
            }

            return adInfo.getId();
        }

        @Override
        protected void onPostExecute(String s) {
            setAdId(s);
        }
    }

    /**
     * Sets the AdId properties following GetGAIDTask() call
     * @param newAdId
     */
    public void setAdId(String newAdId) {
        synchronized (this) {
            this.adId = newAdId;

            // Check the method queue for any queued method calls
            Runnable method;
            while ((method = methodQueue.poll()) != null) {
                method.run();
            }
        }
    }

    /**
     *  Helper method to convert InputStream to byte array
      */
    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, len);
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Attempt to get ad data from rest-ads-api web request using Retrofit2 libraries
     */
    private void getAdUsingRetrofit(String uuid, String adId, String appId, double cpmFloor, boolean isInterstitial, String sdkVersion, String adapterVersion, String geo, String adapterType, LocationData locData) {

        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(TempoUtils.getAdsApiUrl(true))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TempoUtils.Warn("GETADS: " + (locData == null ? "NO LOCATION DATA" : (locData.consent + ", " + locData.admin_area)));

        // Create API service instance
        RetrofitApiService apiService = retrofit.create(RetrofitApiService.class);

        // Make the GET request
        Call<WebResponse> call = apiService.getAd(uuid, adId, appId, cpmFloor, isInterstitial, sdkVersion, adapterVersion, geo, adapterType, locData.country_code,
                locData.postal_code, locData.admin_area, locData.sub_admin_area, locData.locality, locData.sub_locality);
        TempoUtils.Warn("API-URL (url):  " + call.request().url());
        call.enqueue(new Callback<WebResponse>() {

            @Override
            public void onResponse(Call<WebResponse> call, Response<WebResponse> response) {

                // Good Responses (200...299)
                if (response.isSuccessful()) {

                    // Build custom web response object
                    WebResponse responseBody = response.body();
                    TempoUtils.Say("TempoSDK[ADS-200]: CampaignID=" + responseBody.id + ", status=" + responseBody.status + ", CPM=" + responseBody.cpm + ", suffix=" + responseBody.location_url_suffix, true);

                    if (responseBody != null) {
                        if (responseBody.status != null) {
                            // OK
                            if (responseBody.status.equals(Constants.RESPONSE_OK)) {

                                // Cannot continue with no campaign ID
                                if (responseBody.id == null || responseBody.id.trim().isEmpty()) {
                                    createMetric(Constants.METRIC_AD_LOAD_FAIL);
                                    listener.onTempoAdFetchFailed(Constants.RESPONSE_NO_CAMPAIGN_ID);
                                    return;
                                }

                                // Update current values
                                currentCampaignId = TempoUtils.checkForTestCampaign(responseBody.id);
                                currentUrlSuffix = responseBody.location_url_suffix; // = "/some_additional_info";
                                currentCPM = responseBody.cpm;

                                // Preload URL
                                preLoadingWebView.loadUrl(TempoUtils.getFullWebUrl(isInterstitial, currentCampaignId, currentUrlSuffix));

                                // Update success markers
                                createMetric(Constants.METRIC_AD_LOAD_SUCCESS);
                                listener.onTempoAdFetchSucceeded();
                                return;
                            }
                            // NO FILL
                            else if (responseBody.status.equals(Constants.RESPONSE_NO_FILL)) {
                                // Update failure markers
                                TempoUtils.Shout("TempoSDK[ADS-200]: Error while fetching " + TempoUtils.typeCheck(isInterstitial) + " ad - NO_FILL", true);
                                createMetric(Constants.RESPONSE_NO_FILL);
                                listener.onTempoAdFetchFailed(Constants.RESPONSE_NO_FILL);
                                return;
                            }
                            // UNEXPECTED RESPONSE
                            else {
                                // Update failure markers
                                TempoUtils.Shout("TempoSDK[ADS-200]: Error while fetching " + TempoUtils.typeCheck(isInterstitial) + " ad - FAIL", true);
                                createMetric(Constants.METRIC_AD_LOAD_FAIL);
                                listener.onTempoAdFetchFailed(Constants.RESPONSE_INVALID);
                                return;
                            }
                        } else {
                            TempoUtils.Warn("TempoSDK[ADS-200]: status was NULL");
                        }
                    } else {
                        TempoUtils.Warn("TempoSDK[ADS-200]: response body was NULL");
                    }
                }

                // Bad response (!200...299)
                if (response.errorBody() != null) {
                    try {
                        // Handle the error bytes, convert to JSON string
                        InputStream errorStream = response.errorBody().byteStream();
                        byte[] errorBytes = toByteArray(errorStream);
                        String errorString = new String(errorBytes);
                        JSONObject obj = new JSONObject(errorString);
                        WebResponse webResponse = new WebResponse(obj);

                        // 400 - Bad Request (i.e. invalid App ID)
                        if (response.code() == 400) {
                            TempoUtils.Say("TempoSDK[ADS-400] - Error with fetching " + TempoUtils.typeCheck(isInterstitial) + ": status=\"" + webResponse.status + "\", error=\"" + webResponse.error + "\"", true);
                            listener.onTempoAdFetchFailed(Constants.RESPONSE_BAD_REQUEST);
                        }
                        // 422 - Unprocessable Entity (i.e. missing AppID field)
                        else if (response.code() == 422) {

                            // Loop through messages...
                            for (int r = 0; r < webResponse.detail.length; r++) {
                                // ...and loc arrays to get readable printout
                                String locs = "{ ";
                                for (int l = 0; l < webResponse.detail[r].loc.length(); l++) {
                                    locs += webResponse.detail[r].loc.toString();
                                    ;
                                    if (l < webResponse.detail[r].loc.length() - 1) {
                                        locs += ", ";
                                    }
                                }
                                locs += " }";

                                // Printout for each message line
                                TempoUtils.Say("TempoSDK[ADS-422] - Error with fetching " + TempoUtils.typeCheck(isInterstitial) + ": detail[" + r + "] msg=\"" + webResponse.detail[r].msg + "\", type=\"" + webResponse.detail[r].type + "\", locs=" + locs, true);
                            }
                            listener.onTempoAdFetchFailed(Constants.RESPONSE_UNPROCESSABLE);
                        }
                        // Anything else
                        else {
                            TempoUtils.Say("TempoSDK[ADS-" + response.code() + "] - Error with fetching " + TempoUtils.typeCheck(isInterstitial) + ": no details", true);
                            listener.onTempoAdFetchFailed(Constants.RESPONSE_UNKNOWN);
                        }
                    }
                    // Catch any exceptions that might bug out the process
                    catch (IOException e) {
                        TempoUtils.Say("TempoSDK[ADS-" + response.code() + "] - Error with fetching " + TempoUtils.typeCheck(isInterstitial) + ": IOException=\"" + e + "\"", true);
                        listener.onTempoAdFetchFailed(Constants.RESPONSE_UNKNOWN);
                    } catch (JSONException e) {
                        TempoUtils.Shout("TempoSDK[ADS-" + response.code() + "] - Error with fetching " + TempoUtils.typeCheck(isInterstitial) + ": JSONException=\"" + e + "\"", true);
                        e.printStackTrace();
                    }
                } else {
                    listener.onTempoAdFetchFailed(Constants.RESPONSE_UNKNOWN);
                    TempoUtils.Shout("TempoSDK[ADS-" + response.code() + "] - Error with fetching " + TempoUtils.typeCheck(isInterstitial) + ": Unknown", true);
                }

                // Marker failures
                createMetric(Constants.METRIC_AD_LOAD_FAIL);
            }

            @Override
            public void onFailure(Call<WebResponse> call, Throwable t) {

                // Marker failures
                createMetric(Constants.METRIC_AD_LOAD_FAIL);
                listener.onTempoAdFetchFailed(Constants.RESPONSE_UNKNOWN);

                // Handle the failure (if differentiation needed)
                if (t instanceof IOException) {
                    TempoUtils.Shout("TempoSDK: Failed while fetching (" + TempoUtils.typeCheck(isInterstitial) + "): IOException=\"" + t + "\"", true);
                } else {
                    TempoUtils.Shout("TempoSDK: Failed while fetching (" + TempoUtils.typeCheck(isInterstitial) + "): Failure Unknown", true);
                }
            }
        });
    }
}