package com.tempoplatform.bl;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class LocationData implements Serializable, Cloneable  {
    public String consent;
    public String state;
    public String postcode;
    public String country_code;
    public String postal_code;
    public String admin_area;
    public String sub_admin_area;
    public String locality;
    public String sub_locality;

    // Always create as NONE (should be default anyway...)
    public LocationData() {
        consent = Constants.LocationConsent.NONE.toString();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     *  Makes backup copy of current instance values - only to be implemented when a valid address has been identified
     */
    public void backupAsJsonFile(Context context) {

        // Create a new File object for the JSON file
        File directory = context.getDir(Constants.BACKUP_LOC_DIR, Context.MODE_PRIVATE);
        String filename = Constants.BACKUP_LOC_FILE;

        // Do not continue is file/directory null or issue creating file
        if (directory != null && (filename != null && !filename.isEmpty())) {
            File file = new File(directory, filename);
            if (file != null) {
                try {
                    TempoUtils.Say("Backup location started: " + file.getAbsolutePath());
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(this);
                    objectOutputStream.close();
                    fileOutputStream.close();
                    TempoUtils.Say("Backup location saved: consent=" + consent + ", countryCode=" + country_code + ", adminArea==" + admin_area + ", subAdminArea==" + sub_admin_area + ", locality==" + locality);
                    return;
                } catch (IOException e) {
                    TempoUtils.Warn("Backup location failed: '" + e + "'");
                    e.printStackTrace();
                    return;
                }
            }

            TempoUtils.Warn("Backup location failed - could not create file/dir");
        }
    }
}
