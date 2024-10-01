package com.tempoplatform.bl;

public interface UnityCallbacks {
    void onInit();
    void onConsentTypeRequest(String consentType);
    void onLocDataSuccess(String consent, String state, String postcode, String country_code, String postal_code,
                           String admin_area, String sub_admin_area, String locality, String sub_locality);
    void onLocDataFailure(String consent, String state, String postcode, String country_code, String postal_code,
                          String admin_area, String sub_admin_area, String locality, String sub_locality);
    void onCountryCodeRequest(String countryCode);
    void onAdIdRequest(String adId);
    void onVersionCodeRequest(int versionCode);
    void onVersionNameRequest(String versionName);
}
