package com.tempoplatform.bl;

import android.content.Context;
import android.location.Location;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Metric {

    public String metric_type;
    public String ad_id;
    public String app_id;
    public String timestamp;
    public String bundle_id;
    public String campaign_id;
    public String session_id;
    public String os;
    public Boolean is_interstitial;
    public String location;
    public String country_code;
    public String placement_id;
    public Number cpm;
    public String sdk_version;
    public String adapter_version;
    public String adapter_type;
    public Boolean consent;
    public String consent_type;
    public LocationData location_data;

    // Static values - careful with these...
    public static String capturedAdapterVersion;
    private static ArrayList<Metric> interstitialList = new ArrayList<>();
    private static boolean interstitialOnHold = false; // TODO: Remove this?
    private static ArrayList<Metric> rewardedList= new ArrayList<>();
    private static boolean rewardedOnHold = false; // TODO: Remove this?

    public Metric(String metric_type, String ad_id, String app_id, String bundle_id, String campaign_id,
                  String session_id, String os, Boolean is_interstitial, String location, String placement_id,
                  Number cpm, String adapter_type, Boolean consent, String consent_type) {

        this.metric_type = metric_type;
        this.ad_id = ad_id;
        this.app_id = app_id;
        this.bundle_id = bundle_id;
        this.campaign_id = campaign_id;
        this.session_id = session_id;
        this.os = os;
        this.is_interstitial = is_interstitial;
        this.location = location;
        this.placement_id = placement_id;
        this.timestamp = Long.toString(System.currentTimeMillis()) ;
        this.country_code = location;
        this.sdk_version = Constants.SDK_VERSION;
        this.adapter_version = capturedAdapterVersion;
        this.cpm = cpm;
        this.adapter_type = adapter_type;
        this.consent = consent;
        this.consent_type = consent_type;
        this.location_data = getCleanedLocData();

        TempoUtils.metricOutput(this);
    }

    /**
     *  Clones current static loc data - removes all data if the consent is NONE
     */
    public static LocationData getCleanedLocData() {

        // TODO: Can I use :: LocationData locationData = TempoProfile.locData; // ??

        LocationData metricLocData = new LocationData();

        if(TempoProfile.locData != null) {

            metricLocData.consent = TempoProfile.locData.consent;
            if(metricLocData.consent != Constants.LocationConsent.NONE.toString()) {

                String lc_pc = TempoProfile.locData.postcode;
                metricLocData.postcode = lc_pc;

                String lc_state = TempoProfile.locData.state;
                metricLocData.state = lc_state;

                String lc_countryCode = TempoProfile.locData.country_code;
                metricLocData.country_code = lc_countryCode;

                String lc_postalCode = TempoProfile.locData.postal_code;
                metricLocData.postal_code = lc_postalCode;

                String lc_adminArea = TempoProfile.locData.admin_area;
                metricLocData.admin_area = lc_adminArea;

                String lc_subAdminArea = TempoProfile.locData.sub_admin_area;
                metricLocData.sub_admin_area = lc_subAdminArea;

                String lc_locality = TempoProfile.locData.locality;
                metricLocData.locality = lc_locality;

                String lc_subLocality = TempoProfile.locData.sub_locality;
                metricLocData.sub_locality = lc_subLocality;
            }
        }

        return metricLocData;
    }

    /**
     *  Adds new metric object to one of the ArrayLists, depending on ad type - sends off if specific metric_type
     */
    public static void addMetricToList(Metric metric, Context context) {

        // Hold if UNCHECKED/CHECKING - unless the metric is "AD_CLOSE"
        if((TempoProfile.locDataState == TempoProfile.LocationState.UNCHECKED || TempoProfile.locDataState == TempoProfile.LocationState.CHECKING) && metric.metric_type != Constants.METRIC_AD_CLOSE) {
            TempoUtils.Warn("[" + metric.metric_type + "(" + (metric.is_interstitial ? "I" : "R") + ")] metric is currently being held (" + TempoProfile.locDataState + ")");

            // Add metric to list and update static marker as on-hold
            if(metric.is_interstitial) {
                interstitialList.add(metric);
                interstitialOnHold = true;
            } else {
                rewardedList.add(metric);
                rewardedOnHold = true;
            }

            // Nothing more to do hear but play the waiting game...
            return;
        }

        // Add metric to list and update static marker as NOT on-hold
        if(metric.is_interstitial) {
            interstitialOnHold = false;
            interstitialList.add(metric);
        } else {
            rewardedOnHold = false;
            rewardedList.add(metric);
        }

        // Here we want to upload metrics immediately in case the rest of the steps don't occur.
        if (Arrays.asList(Constants.AUTO_PUSH).contains(metric.metric_type)) {

            // Create metrics copy and send offs
            if(metric.is_interstitial) {
                pushInterstitial(context);
            } else {
                pushRewarded(context);
            }
        }
    }

    /**
     * Static method that returns a new version of a given ArrayList with no object references to original.
     * This is useful when building and deleting the global metricLists which are being accessed and cleaned on multiple threads
     * @param origMetricList ArrayList to be cloned
     * @return clean ArrayList with no references to original
     */
    private static ArrayList<Metric> getCleanMetricList(ArrayList<Metric> origMetricList){
        ArrayList<Metric> clonedList = new ArrayList<>(origMetricList.size());

        for (Metric metric : origMetricList) {
            Metric clonedMetric = metric;
            clonedList.add(clonedMetric);
        }

        return clonedList;
    }

    /**
     * Convert the MetricJson object to a JSONArray object
     */
    private static JSONArray convertMetricsToJson(ArrayList<Metric> metrics) {
        if(metrics.size() == 0)
        {
            TempoUtils.Shout("Metrics arraylist is empty. Ignoring...");
            return null;
        }

        try {
            //Gson gson = new Gson();
            if(metrics == null || metrics.size() == 0)
            {
                TempoUtils.Shout("Metrics arraylist null/empty!");
                return null;
            }
            String jsonString = new Gson().toJson(metrics);
            JSONArray jsonArray = new JSONArray(jsonString);
            return jsonArray;
        } catch (JSONException e) {
            TempoUtils.Shout("JSON Conversion crashed: \" + e");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns a string summary for log output which provides summarised details of metrics being send/backed-up
     */
    private static String getMetricSummary(ArrayList<Metric> metricList) {

        if (metricList == null)
            return "METRICS: (NULL)";

        if (metricList.isEmpty())
            return "METRICS: (EMPTY)";

        String metricOutput = "METRICS: (" + metricList.size() + ")";
        for (int m = 0; m < metricList.size(); m++) {
            metricOutput += " [" + metricList.get(m).metric_type + "]";
        }
        return metricOutput + " " + TempoUtils.typeCheck(metricList.get(0).is_interstitial);
    }

    /**
     * Create Metric list clone of Interstitial Ads metrics, triggers sending method, and clears current static list
     */
    public static void pushInterstitial(Context context) {
        ArrayList<Metric> listToPush = getCleanMetricList(interstitialList);
        interstitialList.clear();
        // listToPush.get(0).adapter_version = "0.1"; // To test bad adapter version in metrics
        pushLatestMetrics(listToPush, context);
    }

    /**
     * Create Metric list clone of Rewarded Ads metrics, triggers sending method, and clears current static list
     */
    public static void pushRewarded(Context context) {
        ArrayList<Metric> listToPush = getCleanMetricList(rewardedList);
        rewardedList.clear();
        pushLatestMetrics(listToPush, context);
    }

    /**
     * Converts Metrics ArrayList to JSON format and sends to backend
     */
    private static void pushLatestMetrics(ArrayList<Metric> metricList, Context context) {

        // Output metrics payload
        String summary = getMetricSummary(metricList);
        JSONArray metricListJson = convertMetricsToJson(metricList);
        String jListString = metricListJson.toString();
        jListString = jListString.replaceAll("\\[", "[\n");
        jListString = jListString.replaceAll("]", "\n]");
        jListString = jListString.replaceAll(",", ", ");
        jListString = jListString.replaceAll("\\}, \\{", "},\n{");

        //TempoUtils.Warn("METRICS (body): " + metricListJson);
        TempoUtils.Warn("METRICS (body): " + jListString);

        // No point processing a null list
        if (metricListJson == null) {
            return;
        }

        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(TempoUtils.getMetricsUrl(true))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Create API service instance
        RetrofitApiService apiService = retrofit.create(RetrofitApiService.class);

        // Create X-Timestamp from device time
        String xTimestamp = Long.toString(System.currentTimeMillis() / 1000);

        // Make the POST request
        Call<String> call = apiService.sendMetric(xTimestamp, metricList);
        TempoUtils.Warn("METRICS (url): " + call.request().url());
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, retrofit2.Response<String> response) {

                // Good Responses (200...299)
                if (response.isSuccessful()) {
                    if(response.body() != null) {

                        TempoUtils.Say("TempoSDK[METRIC-" + response.code() + "]: ResponseBody=" + response.body(), true);

                        // Success
                        if (response.body().equals(Constants.RESPONSE_SUCCESS)) {
                            TempoUtils.Say(summary + " <SENT-SUCCESS>");
                            return;
                        }
                        // Partial
                        else if (response.body().equals(Constants.RESPONSE_PARTIAL)) {
                            TempoUtils.Say(summary + " <SENT-PARTIAL>");
                            return;
                        }
                        // In-Flight
                        else if (response.body().equals(Constants.RESPONSE_IN_FLIGHT)) {
                            TempoUtils.Say(summary + " <SENT-INFLIGHT>");
                            return;
                        }
                        // UNEXPECTED RESPONSE
                        else {
                            TempoUtils.Say(summary + " <SENT-UNKNOWN>");
                            // Update failure markers
                            return;
                        }
                    }
                }

                // Bad response (!200...299)
                if (response.errorBody() != null) {
                    try {
                        // Handle the error bytes, convert to JSON string
                        InputStream errorStream = response.errorBody().byteStream();
                        byte[] errorBytes = AdView.toByteArray(errorStream);
                        String errorString = new String(errorBytes);
                        JSONObject obj = new JSONObject(errorString);
                        TempoUtils.Warn("Error response body: " + errorString);

                        // 400 - Bad Request (i.e. invalid App ID)
                        if (response.code() == 400) {
                            WebResponse webResponse = new WebResponse(obj);
                            if(webResponse != null) {
                                TempoUtils.Say("TempoSDK[METRIC-400] - Error sending metric: status=\"" + webResponse.status + "\", error=\"" + webResponse.error + "\"", true);
                            }
                            else{
                                TempoUtils.Say("TempoSDK[METRIC-400] - Error sending metric: " + response.body(), true);
                            }
                        }
                        // 422 - Unprocessable Entity (i.e. missing AppID field)
                        else if (response.code() == 422) {
                            WebResponse webResponse = new WebResponse(obj);

                            if(webResponse != null) {
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
                                    TempoUtils.Say("TempoSDK[METRIC-422] - Error sending metric: detail[" + r + "] msg=\"" + webResponse.detail[r].msg + "\", type=\"" + webResponse.detail[r].type + "\", locs=" + locs, true);
                                }
                            }
                            else{
                                TempoUtils.Say("TempoSDK[METRIC-422] - Error sending metric: " + response.body(), true);
                            }

                        }
                        // Anything else
                        else {
                            TempoUtils.Say("TempoSDK[METRIC-" + response.code() + "] - Error sending metric: no details", true);
                        }
                    }
                    // Catch any exceptions that might bug out the process
                    catch (IOException e) {
                        TempoUtils.Say("TempoSDK[METRIC-" + response.code() + "] - Error sending metric: IOException=\"" + e + "\"", true);
                    } catch (JSONException e) {
                        TempoUtils.Shout("TempoSDK[METRIC-" + response.code() + "] - Error sending metric: JSONException=\"" + e + "\"", true);
                        e.printStackTrace();
                    }
                } else {
                    TempoUtils.Shout("TempoSDK[METRIC-" + response.code() + "] - Error sending metric: Unknown", true);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

                // Marker failures
                TempoBackupData.backupData(metricListJson, context);

                // Handle the failure (if differentiation needed)
                if (t instanceof IOException) {
                    TempoUtils.Shout("TempoSDK: Failed sending metric: IOException=\"" + t + "\"", true);
                } else {
                    TempoUtils.Shout("TempoSDK: Failed sending metric: Failure Unknown", true);
                }
            }
        });
    }

    private static ArrayList<Metric> convertJSONArrayToMetricArrayList(JSONArray metricJson) {

        // Convert JSONArray to ArrayList
        ArrayList<Metric> returningMetricList = new ArrayList<>();

        // Iterate through the JSONArray and convert each object to a Metric
        for (int i = 0; i < metricJson.length(); i++) {

            // Make sure this metric instance is valid
            JSONObject jsonMetric = null;
            try {
                jsonMetric = metricJson.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // Metric IS Valid
            if (jsonMetric != null) {

                // METRIC_TYPE
                String metric_type;
                try {
                    metric_type = jsonMetric.getString("metric_type");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'metric_type': " + e);
                    metric_type = null;
                }

                // AD ID
                String ad_id;
                try {
                    ad_id = jsonMetric.getString("ad_id");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'ad_id': " + e);
                    ad_id = null;
                }

                // APP ID
                String app_id;
                try {
                    app_id = jsonMetric.getString("app_id");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'app_id': " + e);
                    app_id = null;
                }

                // TIME STAMP
                String timestamp;
                try {
                    timestamp = jsonMetric.getString("timestamp");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'timestamp': " + e);
                    timestamp = null;
                }

                // BUNDLE ID
                String bundle_id;
                try {
                    bundle_id = jsonMetric.getString("bundle_id");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'bundle_id': " + e);
                    bundle_id = null;
                }

                // CAMPAIGN ID
                String campaign_id;
                try {
                    campaign_id = jsonMetric.getString("campaign_id");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'campaign_id': " + e);
                    campaign_id = null;
                }

                // SESSION ID
                String session_id;
                try {
                    session_id = jsonMetric.getString("session_id");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'session_id': " + e);
                    session_id = null;
                }

                // OS
                String os;
                try {
                    os = jsonMetric.getString("os");
                } catch (JSONException e) {
                    os = null;
                }

                // IS INTERSTITIAL
                Boolean is_interstitial;
                try {
                    is_interstitial = jsonMetric.getBoolean("is_interstitial");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'is_interstitial': " + e);
                    is_interstitial = null;
                }

                // LOCATION
                String location;
                try {
                    location = jsonMetric.getString("location");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'location': " + e);
                    location = null;
                }

                // COUNTRY CODE
                String country_code;
                try {
                    country_code = jsonMetric.getString("country_code");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'country_code': " + e);
                    country_code = null;
                }

                // PLACEMENT ID
                String placement_id;
                try {
                    placement_id = jsonMetric.getString("placement_id");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'placement_id': " + e);
                    placement_id = null;
                }

                // CPM
                Double cpm;
                try {
                    cpm = jsonMetric.getDouble("cpm");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'cpm': " + e);
                    cpm = null;
                }


                // ADAPTER TYPE
                String adapter_type;
                try {
                    adapter_type = jsonMetric.getString("adapter_type");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'adapter_type': " + e);
                    adapter_type = null;
                }


                // SDK VERSION
                String sdk_version;
                try {
                    sdk_version = jsonMetric.getString("sdk_version");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'sdk_version': " + e);
                    sdk_version = null;
                }

                // ADAPTER VERSION
                String adapter_version;
                try {
                    adapter_version = jsonMetric.getString("adapter_version");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'adapter_version': " + e);
                    adapter_version = null;
                }

                // CONSENT
                Boolean consent;
                try {
                    consent = jsonMetric.getBoolean("consent");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'consent': " + e);
                    consent = null;
                }

                // CONSENT TYPE
                String consent_type;
                try {
                    consent_type = jsonMetric.getString("consent_type");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'consent_type': " + e);
                    consent_type = null;
                }

                // LOCATION DATA
                LocationData ld = new LocationData();
                JSONObject location_data_json;
                try {
                    location_data_json = jsonMetric.getJSONObject("location_data");
                } catch (JSONException e) {
                    //TempoUtils.Shout("Failed getting 'location_data': " + e);
                    location_data_json = null;
                }
                if (location_data_json != null) {

                    // LOC CONSENT
                    try {
                        ld.consent = jsonMetric.getString("consent");
                    } catch (JSONException e) {
                        //TempoUtils.Shout("Failed getting 'location_consent': " + e);
                        ld.consent = null;
                    }

                    // LOC POSTAL CODE
                    try {
                        ld.postal_code = jsonMetric.getString("postal_code");
                    } catch (JSONException e) {
                        //TempoUtils.Shout("Failed getting 'location_postal_code': " + e);
                        ld.postal_code = null;
                    }

                    // LOC ADMIN AREA
                    try {
                        ld.admin_area = jsonMetric.getString("admin_area");
                    } catch (JSONException e) {
                        //TempoUtils.Shout("Failed getting 'location_admin_area': " + e);
                        ld.admin_area = null;
                    }

                    // LOC SUB ADMIN AREA
                    try {
                        ld.sub_admin_area = jsonMetric.getString("sub_admin_area");
                    } catch (JSONException e) {
                        //TempoUtils.Shout("Failed getting 'location_sub_admin_area': " + e);
                        ld.sub_admin_area = null;
                    }

                    // LOC LOCALITY
                    try {
                        ld.locality = jsonMetric.getString("locality");
                    } catch (JSONException e) {
                        //TempoUtils.Shout("Failed getting 'location_locality': " + e);
                        ld.locality = null;
                    }

                    // LOC SUB LOCALITY
                    try {
                        ld.sub_locality = jsonMetric.getString("sub_locality");
                    } catch (JSONException e) {
                        //TempoUtils.Shout("Failed getting 'location_sub_locality': " + e);
                        ld.sub_locality = null;
                    }
                }

                Metric newMetric = new Metric(metric_type, ad_id, app_id, bundle_id, campaign_id, session_id, os, is_interstitial, country_code, placement_id, cpm, adapter_type, consent, consent_type);
                newMetric.timestamp = timestamp;
                newMetric.sdk_version = sdk_version;
                newMetric.adapter_version = adapter_version;
                newMetric.location_data = ld;

                returningMetricList.add(newMetric);
            }
        }

        return returningMetricList;
    }

    /**
     * Backup method for sending metric ArrayList (in JSONArray form) that failed to send in  previous session.
     * If no errors occur and metric is sent, a deletion request is sent to remove from device's storage.
     */
    public static void pushBackupMetrics(JSONArray metricJson, String filename, Context context) {

        if (metricJson == null) {
            return;
        }
        else if(metricJson.length() == 0)
        {
            TempoUtils.Say("Removing empty file: " + filename);
            TempoBackupData.removeFile(filename, context);
        }
        else {

            // Create Retrofit instance
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(TempoUtils.getMetricsUrl(true))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            // Create API service instance
            RetrofitApiService apiService = retrofit.create(RetrofitApiService.class);

            // Create X-Timestamp from device time
            String xTimestamp = filename.substring(0, 10);





            // Make the POST request
            ArrayList<Metric> thisMetricList = convertJSONArrayToMetricArrayList(metricJson);
            Call<String> call = apiService.sendBackupMetric(xTimestamp, thisMetricList);

            TempoUtils.Shout("METRIC JSON: " + metricJson);
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, retrofit2.Response<String> response) {

                    // Good Responses (200...299)
                    if (response.isSuccessful()) {
                        if(response.body() != null) {

                            TempoUtils.Say("TempoSDK[METRIC-" + response.code() + "]: ResponseBody=" + response.body(), true);

                            // Success
                            if (response.body().equals(Constants.RESPONSE_SUCCESS)) {
                                TempoUtils.Say(filename + " <SENT-SUCCESS>");
                            }
                            // Partial
                            else if (response.body().equals(Constants.RESPONSE_PARTIAL)) {
                                TempoUtils.Say(filename + " <SENT-PARTIAL>");
                            }
                            // In-Flight
                            else if (response.body().equals(Constants.RESPONSE_IN_FLIGHT)) {
                                TempoUtils.Say(filename + " <SENT-INFLIGHT>");
                            }
                            // UNEXPECTED RESPONSE
                            else {
                                TempoUtils.Say(filename + " <SENT-UNKNOWN>");
                                // Update failure markers
                            }

                            TempoUtils.Say("TempoSDK: Successfully transferred backup data", true);
                            TempoBackupData.removeFile(filename, context);
                            return;
                        }
                    }

                    // Bad response (!200...299)
                    if (response.errorBody() != null) {
                        try {
                            // Handle the error bytes, convert to JSON string
                            InputStream errorStream = response.errorBody().byteStream();
                            byte[] errorBytes = AdView.toByteArray(errorStream);
                            String errorString = new String(errorBytes);
                            JSONObject obj = new JSONObject(errorString);
                            TempoUtils.Warn("Error response body: " + errorString);

                            // 400 - Bad Request (i.e. invalid App ID)
                            if (response.code() == 400) {
                                WebResponse webResponse = new WebResponse(obj);
                                if(webResponse != null) {
                                    TempoUtils.Say("TempoSDK[METRIC-400] - Error sending metric: status=\"" + webResponse.status + "\", error=\"" + webResponse.error + "\"", true);
                                }
                                else{
                                    TempoUtils.Say("TempoSDK[METRIC-400] - Error sending metric: " + response.body(), true);
                                }

                                TempoUtils.Say("TempoSDK: Deleting invalid backup data [" + response.code() + "]", true);
                                TempoBackupData.removeFile(filename, context);
                            }
                            // 422 - Unprocessable Entity (i.e. missing AppID field)
                            else if (response.code() == 422) {
                                WebResponse webResponse = new WebResponse(obj);

                                if(webResponse != null) {
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
                                        TempoUtils.Say("TempoSDK[METRIC-422] - Error sending metric: detail[" + r + "] msg=\"" + webResponse.detail[r].msg + "\", type=\"" + webResponse.detail[r].type + "\", locs=" + locs, true);
                                    }
                                }
                                else{
                                    TempoUtils.Say("TempoSDK[METRIC-422] - Error sending metric: " + response.body(), true);
                                }

                                TempoUtils.Say("TempoSDK: Deleting invalid backup data [" + response.code() + "]", true);
                                TempoBackupData.removeFile(filename, context);
                            }
                            // Anything else
                            else {
                                TempoUtils.Say("TempoSDK[METRIC-" + response.code() + "] - Error sending metric: no details", true);
                                TempoUtils.Shout( "Unknown Response: " + response.code() + "/noaction <BACKUP_FAILED-RESPONSE> | " + response);
                            }
                        }
                        // Catch any exceptions that might bug out the process
                        catch (IOException e) {
                            TempoUtils.Say("TempoSDK[METRIC-" + response.code() + "] - Error sending metric: IOException=\"" + e + "\"", true);
                        } catch (JSONException e) {
                            TempoUtils.Shout("TempoSDK[METRIC-" + response.code() + "] - Error sending metric: JSONException=\"" + e + "\"", true);
                            e.printStackTrace();
                        }
                    } else {
                        TempoUtils.Shout("TempoSDK[METRIC-" + response.code() + "] - Error sending metric: Unknown", true);
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {

                    TempoUtils.Shout("TempoSDK: Error transferring backup data: " + t.getMessage(), true);

                    // Handle the failure (if differentiation needed)
                    if (t instanceof IOException) {
                        TempoUtils.Shout("TempoSDK: Failed sending metric: IOException=\"" + t + "\"", true);
                    } else {
                        TempoUtils.Shout("TempoSDK: Failed sending metric: Failure Unknown", true);
                    }
                }
            });
        }
    }

    /**
     * Returns 'geo' field as stipulated by mediation parameters - NOTE: currently just 'US'
     */
    protected static String getGeoLocation() {
        return "US"; // TODO: This is filler while in external testing, will need value from mediation parameters for major release
    }

    /**
     * Create metrics copy, updates any valid values, and pushes latest saved metrics to backend
     */
    protected static void updateMetricsWithNewLocationData(boolean isInterstitial, Constants.LocationConsent consent, Context context, AdView adView) {

        // Interstitial
        if(isInterstitial) {
            for (Metric metric : interstitialList) {
                if(metric.location_data.consent != Constants.LocationConsent.NONE.toString()) {
                    if(metric.location_data.consent == Constants.LocationConsent.GENERAL.toString() && consent.toString() == Constants.LocationConsent.PRECISE.toString()) {
                        // Ignore - cannot use PRECISE location data when only GENERAL was approved at time of data capture
                    } else if (metric.location_data.consent == Constants.LocationConsent.PRECISE.toString() && consent.toString() == Constants.LocationConsent.GENERAL.toString()) {
                        // Ignore - shouldn't use GENERAL location data when PRECISE was probably more accurate when it was taken
                    } else {
                        // Consent values should match so good to update UNLESS it's overwriting that value with null values

                        if (TempoProfile.locData.postcode != null) { // POSTCODE
                            metric.location_data.postcode = TempoProfile.locData.postcode;
                            TempoUtils.Say("updateMetricsWithNewLocationData ("+ metric.metric_type + "/Postcode) " + metric.location_data.postcode + " => " + TempoProfile.locData.postcode);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData ("+ metric.metric_type +"/Postcode) " + metric.location_data.postcode + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.state != null) { // STATE
                            metric.location_data.state = TempoProfile.locData.state;
                            TempoUtils.Say("updateMetricsWithNewLocationData ("+ metric.metric_type + "/State) " + metric.location_data.state + " => " + TempoProfile.locData.state);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData ("+ metric.metric_type +"/State) " + metric.location_data.state + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.country_code != null) { // COUNTRY CODE
                            metric.location_data.country_code = TempoProfile.locData.country_code;

                            // Update top-level metrics
                            if(!TempoProfile.locData.country_code.isEmpty()) {
                                metric.country_code = TempoProfile.locData.country_code;
                                metric.location = TempoProfile.locData.country_code;
                            }

                            TempoUtils.Say("updateMetricsWithNewLocationData ("+ metric.metric_type + "/CountryCode) " + metric.location_data.country_code + " => " + TempoProfile.locData.country_code);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData ("+ metric.metric_type +"/CountryCode) " + metric.location_data.country_code + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.country_code != null) { // POSTAL CODE
                            metric.location_data.postcode = TempoProfile.locData.postcode;
                            TempoUtils.Say("updateMetricsWithNewLocationData ("+ metric.metric_type + "/PostalCode) " + metric.location_data.postcode + " => " + TempoProfile.locData.postcode);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData ("+ metric.metric_type +"/PostalCode) " + metric.location_data.postcode + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.admin_area != null) { // ADMIN AREA
                            metric.location_data.admin_area = TempoProfile.locData.admin_area;
                            TempoUtils.Say("updateMetricsWithNewLocationData ("+ metric.metric_type + "/AdminArea) " + metric.location_data.admin_area + " => " + TempoProfile.locData.admin_area);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData ("+ metric.metric_type +"/AdminArea) " + metric.location_data.admin_area + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.sub_admin_area != null) { // SUB ADMIN AREA
                            metric.location_data.sub_admin_area = TempoProfile.locData.sub_admin_area;
                            TempoUtils.Say("updateMetricsWithNewLocationData ("+ metric.metric_type + "/SubAdminArea) " + metric.location_data.sub_admin_area + " => " + TempoProfile.locData.sub_admin_area);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData ("+ metric.metric_type +"/SubAdminArea) " + metric.location_data.sub_admin_area + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.locality != null) { // LOCALITY
                            metric.location_data.locality = TempoProfile.locData.locality;
                            TempoUtils.Say("updateMetricsWithNewLocationData ("+ metric.metric_type + "/Locality) " + metric.location_data.locality + " => " + TempoProfile.locData.locality);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData ("+ metric.metric_type +"/Locality) " + metric.location_data.locality + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.sub_locality != null) { // SUB LOCALITY
                            metric.location_data.sub_locality = TempoProfile.locData.sub_locality;
                            TempoUtils.Say("updateMetricsWithNewLocationData ("+ metric.metric_type + "/SubLocality) " + metric.location_data.sub_locality + " => " + TempoProfile.locData.sub_locality);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData ("+ metric.metric_type +"/SubLocality) " + metric.location_data.sub_locality + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                    }
                }
            }
            pushInterstitial(context);
        }
        // Rewarded
        else {
            for (Metric metric : rewardedList) {
                if (metric.location_data.consent != Constants.LocationConsent.NONE.toString()) {
                    if (metric.location_data.consent == Constants.LocationConsent.GENERAL.toString() && consent.toString() == Constants.LocationConsent.PRECISE.toString()) {
                        // Ignore - cannot use PRECISE location data when only GENERAL was approved at time of data capture
                    } else if (metric.location_data.consent == Constants.LocationConsent.PRECISE.toString() && consent.toString() == Constants.LocationConsent.GENERAL.toString()) {
                        // Ignore - shouldn't use GENERAL location data when PRECISE was probably more accurate when it was taken
                    } else {
                        // Consent values should match so good to update UNLESS it's overwriting that value with null values

                        if (TempoProfile.locData.postcode != null) { // POSTCODE
                            metric.location_data.postcode = TempoProfile.locData.postcode;
                            TempoUtils.Say("updateMetricsWithNewLocationData (" + metric.metric_type + "/Postcode) " + metric.location_data.postcode + " => " + TempoProfile.locData.postcode);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData (Postcode) " + metric.location_data.postcode + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.state != null) { // STATE
                            metric.location_data.state = TempoProfile.locData.state;
                            TempoUtils.Say("updateMetricsWithNewLocationData (" + metric.metric_type + "/State) " + metric.location_data.state + " => " + TempoProfile.locData.state);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData (" + metric.metric_type + "/State) " + metric.location_data.state + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }


                        if (TempoProfile.locData.country_code != null) { // COUNTRY CODE
                            metric.location_data.country_code = TempoProfile.locData.country_code;
                            TempoUtils.Say("updateMetricsWithNewLocationData (" + metric.metric_type + "/CountryCode) " + metric.location_data.country_code + " => " + TempoProfile.locData.country_code);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData (" + metric.metric_type + "/CountryCode) " + metric.location_data.country_code + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.country_code != null) { // POSTAL CODE
                            metric.location_data.postcode = TempoProfile.locData.postcode;
                            TempoUtils.Say("updateMetricsWithNewLocationData (" + metric.metric_type + "/PostalCode) " + metric.location_data.postcode + " => " + TempoProfile.locData.postcode);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData (" + metric.metric_type + "/PostalCode) " + metric.location_data.postcode + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.admin_area != null) { // ADMIN AREA
                            metric.location_data.admin_area = TempoProfile.locData.admin_area;
                            TempoUtils.Say("updateMetricsWithNewLocationData (" + metric.metric_type + "/AdminArea) " + metric.location_data.admin_area + " => " + TempoProfile.locData.admin_area);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData (" + metric.metric_type + "/AdminArea) " + metric.location_data.admin_area + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.sub_admin_area != null) { // SUB ADMIN AREA
                            metric.location_data.sub_admin_area = TempoProfile.locData.sub_admin_area;
                            TempoUtils.Say("updateMetricsWithNewLocationData (" + metric.metric_type + "/SubAdminArea) " + metric.location_data.sub_admin_area + " => " + TempoProfile.locData.sub_admin_area);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData (" + metric.metric_type + "/SubAdminArea) " + metric.location_data.sub_admin_area + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.locality != null) { // LOCALITY
                            metric.location_data.locality = TempoProfile.locData.locality;
                            TempoUtils.Say("updateMetricsWithNewLocationData (" + metric.metric_type + "/Locality) " + metric.location_data.locality + " => " + TempoProfile.locData.locality);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData (" + metric.metric_type + "/Locality) " + metric.location_data.locality + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                        if (TempoProfile.locData.sub_locality != null) { // SUB LOCALITY
                            metric.location_data.sub_locality = TempoProfile.locData.sub_locality;
                            TempoUtils.Say("updateMetricsWithNewLocationData (" + metric.metric_type + "/SubLocality) " + metric.location_data.sub_locality + " => " + TempoProfile.locData.sub_locality);
                        } else {
                            TempoUtils.Warn("updateMetricsWithNewLocationData (" + metric.metric_type + "/SubLocality) " + metric.location_data.sub_locality + " => null (NOT UPDATED DUE TO NULL VALUES)");
                        }

                    }
                }
            }
            pushRewarded(context);
        }
    }
}
