package com.tempoplatform.bl;

import org.json.JSONArray;

import java.util.ArrayList;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import retrofit2.http.POST;

public interface RetrofitApiService {
    @Headers(Constants.ADS_API_CONTENT_JSON)

    @GET(Constants.ADS_API_URL_APN)
    Call<WebResponse> getAd(@Query(Constants.ADS_API_UUID) String uuid,
                             @Query(Constants.ADS_API_AD_ID) String adId,
                             @Query(Constants.ADS_API_APP_ID) String appId,
                             @Query(Constants.ADS_API_CPM_FLOOR) double cpmFloor,
                             @Query(Constants.ADS_API_IS_INITERSTIIAL) boolean isInterstitial,
                             @Query(Constants.ADS_API_SDK_VER) String sdkVersion,
                             @Query(Constants.ADS_API_ADAPTER_VER) String adapterVersion,
                             @Query(Constants.ADS_API_GEO) String geo,
                             @Query(Constants.ADS_API_ADAPTER_TYPE) String adapterType,
                             @Query(Constants.ADS_API_COUNTRY_CODE) String countryCode,
                             @Query(Constants.ADS_API_POSTAL_CODE) String postalCode,
                             @Query(Constants.ADS_API_ADMIN_AREA) String adminArea,
                             @Query(Constants.ADS_API_SUB_ADMIN_AREA) String subAdminArea,
                             @Query(Constants.ADS_API_LOCALITY) String locality,
                             @Query(Constants.ADS_API_SUB_LOCALITY) String subLocality);

    @POST(Constants.METRIC_URL_APN)
    Call<String> sendMetric(@Header(Constants.ADS_API_X_TIMESTAMP) String timestamp, @Body ArrayList<Metric> metrics);

    @POST(Constants.METRIC_URL_APN)
    Call<String> sendBackupMetric(@Header(Constants.ADS_API_X_TIMESTAMP) String timestamp, @Body ArrayList<Metric> metrics);
}
