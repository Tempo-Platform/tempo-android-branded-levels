package com.tempoplatform.bl;

// Consent
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Backups
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class LocationData implements Serializable, Cloneable  {
    public String consent = "_consent";
    public String state = "_state";
    public String postcode = "_postcode";
    public String country_code = "_country_code";
    public String postal_code = "_postal_code";
    public String admin_area = "_admin_area";
    public String sub_admin_area = "_sub_admin_area";
    public String locality = "_locality";
    public String sub_locality = "_sub_locality";

    public LocationData() {
        consent = Constants.LocationConsent.NONE.toString();
    }

    public static Constants.LocationConsent getLocationConsent(Context context) {

        // PRECISE
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Utils.Warn("PRECISE: Precise location permission is granted, you can proceed with location-related tasks");
            return Constants.LocationConsent.PRECISE;
        }
        // GENERAL
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Utils.Warn("GENERAL: General location permission is granted, you can proceed with location-related tasks");
            return Constants.LocationConsent.GENERAL;
        }
        // NONE
        else {
            // Check if the user has denied the permission before
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Utils.Warn("FALSE - User has denied the permission before, you can show a rationale explaining why you need the permission and request it again if the user agrees");
            } else {
                Utils.Warn("FALSE - User has not been asked for permission yet or has denied it with 'Never ask again' checked. You can prompt the user to grant the permission from the settings");
            }
            return Constants.LocationConsent.NONE;
        }
    }

    public void backupAsJsonFile(Context context) {

        // Create a new File object for the JSON file
        File directory = context.getDir(Constants.LOC_DIR, Context.MODE_PRIVATE);
        String filename = Constants.LOC_FILE;

        // Do not continue is file/directory null or issue creating file
        if (directory != null && (filename != null && !filename.isEmpty())) {
            File file = new File(directory, filename);
            if (file != null) {
                try {
                    Utils.Say("Backup location started: " + file.getAbsolutePath());
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(this);
                    objectOutputStream.close();
                    fileOutputStream.close();
                    Utils.Say("Backup location saved: consent=" + consent + ", countryCode=" + country_code + ", adminArea==" + admin_area + ", subAdminArea==" + sub_admin_area + ", locality==" + locality);
                    return;
                } catch (IOException e) {
                    Utils.Warn("Backup location failed: '" + e + "'");
                    e.printStackTrace();
                    return;
                }
            }

            Utils.Warn("Backup location failed - could not create file/dir");
        }
    }
}
