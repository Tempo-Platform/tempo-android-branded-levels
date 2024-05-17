package com.tempoplatform.bl;

public class TempoTesting {

    // DEPLOY VERSION
    public static boolean isTestingDeployVersion = false;
    public static String deployVersionId;

    // CUSTOM CAMPAIGN IDS
    public static boolean isTestingCustomCampaigns = false;
    public static String customCampaignId;

    // UPDATE 'CONSTANTS' (yes I'm aware of what that says)
    public static void makeProd(boolean isProd) {
        Constants.isProd = isProd;
        Constants.isStripeProd = isProd;
    }

    public static boolean getEnvironmentState() {
        //TempoUtils.Say("--> ENV = " +  (Constants.isProd ? "PROD" : "DEV"), true);
        return Constants.isProd;
    }

    public static boolean getTestState() {
        //TempoUtils.Say("--> TESTING = " +  Constants.isTesting, true);
        return Constants.isTesting;
    }

    public static void toggleDebugOutput() {
        Constants.isTesting = !Constants.isTesting;
    }
}
