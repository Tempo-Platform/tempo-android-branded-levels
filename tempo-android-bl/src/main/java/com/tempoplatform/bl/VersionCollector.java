package com.tempoplatform.bl;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class VersionCollector {

    private String LOG = "TempoBL";

    public void getVersion(Context context, Profile profile) {
        PackageManager pm = context.getPackageManager();
        if(pm != null) {
            String pname = context.getPackageName();
            if(pname != null && pname != "") {
                try {
                    PackageInfo version = pm.getPackageInfo(pname, 0);
                    if(version != null) {
                        // Version code
                        int versionCode = version.versionCode;
                        Log.w(LOG, "versionCode: " + versionCode);
                        profile.unityCallbacks.onVersionCodeRequest(versionCode);
                        // Version
                        String versionName = version.versionName;
                        Log.w(LOG, "versionName: " + versionName);
                        profile.unityCallbacks.onVersionNameRequest(versionName);
                    } else {
                        Log.e(LOG, "PackageInfo was null");
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(LOG, "Error getting version details: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            } else {
                Log.e(LOG, "PackageName was null/empty");
            }
        } else {
            Log.e(LOG, "PackageManager was null");
        }
    }
}
