package com.tempoplatform.bl;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class LocationData implements Serializable, Cloneable {
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
        consent = BridgeRef.LocationConsent.NONE.toString();
    }

    @NonNull
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     *  Makes backup copy of current instance values - only to be implemented when a valid address has been identified
     */
    public void saveLocation(Context context) {

        // Create a new File object for the JSON file
        File directory = context.getDir(BridgeRef.BACKUP_LOC_DIR, Context.MODE_PRIVATE);
        String filename = BridgeRef.BACKUP_LOC_FILE;

        // Do not continue is file/directory null or issue creating file
        if (directory != null) {
            File file = new File(directory, filename);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(this);
                objectOutputStream.close();
                fileOutputStream.close();
                Log.d(BridgeRef.LOG, "Backup location saved: consent=" + consent + ", countryCode=" + country_code + ", adminArea=" + admin_area + ", subAdminArea=" + sub_admin_area + ", locality=" + locality);
                return;
            } catch (IOException e) {
                Log.e(BridgeRef.LOG, "Backup location failed: '" + e + "'");
                e.printStackTrace();
                return;
            }

        }

    }

}
