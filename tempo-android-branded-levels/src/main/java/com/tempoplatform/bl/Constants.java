package com.tempoplatform.bl;

import com.tempoplatform.tempo_branded_levels.BuildConfig;

public class Constants {

    public static final String SDK_VERSION = "1.4.3";
    public static final String TEST_LOG = "Tempo!";

    // Not so constants...
    protected static boolean isTesting = false;
    protected static boolean isProd = true;
    protected static boolean isStripeProd = true;

    // Development URL preferences
    public static final String ADS_API_URL_BASE_DEV = "https://ads-api.dev.tempoplatform.com"; // DEV
    public static final String METRIC_URL_BASE_DEV = "https://metric-api.dev.tempoplatform.com"; // DEV

    public static final String ADS_API_URL_DEV = "https://ads-api.dev.tempoplatform.com/ad"; // DEV
    public static final String METRIC_URL_DEV = "https://metric-api.dev.tempoplatform.com/metrics"; // DEV

    public static final String INTERSTITIAL_URL_DEV = "https://development--tempo-html-ads.netlify.app/interstitial/"; // DEV
    public static final String REWARDED_URL_DEV = "https://development--tempo-html-ads.netlify.app/campaign/"; // DEV
    public static final String TEST_APP_ID_DEV = "5"; // DEV

    // Production URL references
    public static final String ADS_API_URL_BASE_PROD = "https://ads-api.tempoplatform.com"; // PROD
    public static final String METRIC_URL_BASE_PROD = "https://metric-api.tempoplatform.com"; // PROD
    public static final String ADS_API_URL_PROD = "https://ads-api.tempoplatform.com/ad"; // PROD
    public static final String METRIC_URL_PROD = "https://metric-api.tempoplatform.com/metrics"; // PROD

    public static final String INTERSTITIAL_URL_PROD = "https://ads.tempoplatform.com/interstitial/"; // PROD
    public static final String REWARDED_URL_PROD = "https://ads.tempoplatform.com/campaign/"; // PROD
    public static final String TEST_APP_ID_PROD = "8"; // PROD

    // URL sections
    public static final String URL_APNX_REW = "campaign/";
    public static final String URL_APNX_INT = "interstitial/";
    public static final String URL_DEPLOY_PREVIEW_PREFIX = "https://deploy-preview-";
    public static final String URL_DEPLOY_PREVIEW_APPENDIX ="--tempo-html-ads.netlify.app/";

    public static final String THANKS_URL = "https://brands.tempoplatform.com/thank-you";

    public static final String METRIC_URL_APN = "metrics"; // DEV
    public static final String ADS_API_URL_APN = "ad"; // DEV

    public static final String INTENT_EXTRAS_CAMPAIGN_ID_KEY = "CAMPAIGN_ID";
    public static final String INTENT_EXTRAS_URL_SUFFIX_KEY = "URL_SUFFIX";
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSS Z (c)";

    public static final String STRIPE_LIVE_KEY = BuildConfig.STRIPE_LIVE;
    public static final String STRIPE_TEST_KEY = BuildConfig.STRIPE_TEST;

    public static final String TEST_PLACEMENT = "ANDROID_STUDIO_BREAKER";
    public static final String METRIC_AD_SHOW = "AD_SHOW";
    public static final String METRIC_AD_SHOW_FAIL = "AD_SHOW_FAIL";
    public static final String METRIC_AD_SHOW_ATTEMPT = "AD_SHOW_ATTEMPT";
    public static final String METRIC_AD_LOAD_REQUEST = "AD_LOAD_REQUEST";
    public static final String METRIC_AD_LOAD_SUCCESS = "AD_LOAD_SUCCESS";
    public static final String METRIC_AD_CLOSE = "AD_CLOSE";
    public static final String METRIC_AD_LOAD_FAIL= "AD_LOAD_FAIL";

    public static final String RESPONSE_NO_FILL = "NO_FILL";
    public static final String RESPONSE_UNKNOWN = "UNKNOWN";
    public static final String RESPONSE_BAD_REQUEST = "BAD_REQUEST";
    public static final String RESPONSE_UNPROCESSABLE = "UNPROCESSABLE";
    public static final String RESPONSE_INVALID = "INVALID_RESPONSE";
    public static final String RESPONSE_NO_CAMPAIGN_ID = "NO_CAMPAIGN_ID";
    public static final String RESPONSE_SUCCESS = "Success";
    public static final String RESPONSE_IN_FLIGHT = "In-Flight";
    public static final String RESPONSE_PARTIAL = "Partial";
    public static final String RESPONSE_OK = "OK";
    public static final String RESPONSE_ERROR = "ERROR";
    public static final String GAID_NONE = "NONE";

    public static final String[] AUTO_PUSH = {METRIC_AD_SHOW, METRIC_AD_LOAD_REQUEST, METRIC_AD_CLOSE};

    public static final int BACKUP_LIMIT = 100;
    public static final int DELETE_AFTER_DAYS = 7;
    public static final long DAYS_LONG = 86400000L;

    public static final String ANDROID = "Android";
    public static final String SET_JS_ENABLED = "SetJavaScriptEnabled";
    public static final String USER_AGENT_STRING = "Tempo-Android-SDK";
    public static final String GPAY_MERCHANT_COUNTRY = "US";
    public static final String GPAY_MERCHANT_NAME = "Tempo";
    public static final String JS_PAGE_FINISHED = "(function() { window.dispatchEvent(myEvent); })();";
    public static final String BACKUP_LOC_DIR = "tempoLocation";
    public static final String BACKUP_LOC_FILE = "location_data.json";
    public static final String BACKUP_METRIC_DIR = "tempoBackup";
    public static final String BACKUP_METRIC_FILETYPE = ".tempo";


    public static final String ADS_API_X_TIMESTAMP = "X-Timestamp";
    public static final String ADS_API_CONTENT_JSON = "Content-Type: application/json";
    public static final String ADS_API_UUID = "uuid";
    public static final String ADS_API_AD_ID = "ad_id";
    public static final String ADS_API_APP_ID = "app_id";
    public static final String ADS_API_CPM_FLOOR = "cpm_floor";
    public static final String ADS_API_IS_INITERSTIIAL = "is_interstitial";
    public static final String ADS_API_SDK_VER = "sdk_version";
    public static final String ADS_API_ADAPTER_VER = "adapter_version";
    public static final String ADS_API_GEO = "geo";
    public static final String ADS_API_ADAPTER_TYPE = "adapter_type";
    public static final String ADS_API_COUNTRY_CODE = "country_code";
    public static final String ADS_API_POSTAL_CODE = "postal_code";
    public static final String ADS_API_ADMIN_AREA = "admin_area";
    public static final String ADS_API_SUB_ADMIN_AREA = "sub_admin_area";
    public static final String ADS_API_LOCALITY = "locality";
    public static final String ADS_API_SUB_LOCALITY = "sub_locality";

    public static boolean isProd() {
        return isProd;
    }
    public enum LocationConsent { NONE, PRECISE, GENERAL }

}
