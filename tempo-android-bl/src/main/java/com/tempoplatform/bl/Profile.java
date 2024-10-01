package com.tempoplatform.bl;

//import static androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Locale;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;

public class Profile implements AdIdListener
{
    public Boolean currentHasBeenChecked;
    public LocationData locData;
    public LocationState locDataState = LocationState.UNCHECKED;
    public CountryCodeConverter ccConverter;
    public UnityCallbacks unityCallbacks;
    //private String adId = BridgeRef.AD_ID;
    //private final BlockingQueue<Runnable> methodQueue = new LinkedBlockingQueue<>();

    /**
     * Different states of the location checking process
     */
    public enum LocationState {
        UNCHECKED,
        CHECKING,
        CHECKED,
        FAILED,
        UNAVAILABLE
    }

    /**
     * Constructor - creates new static locData instance if currently null, and updates consent to NONE
     */
    public Profile(Context context, UnityCallbacks unityCallbacks) {
        this.unityCallbacks = unityCallbacks;

        initSetup(context);
    }

    /**
     * Handles the setup data for constructors
     */
    private void initSetup(Context context) { // TODO: context only needed for backups (i.e. may not be needed here)
        this.ccConverter = new CountryCodeConverter();

        // Configure static instance if currently null
        if (locData == null) {
            locData = new LocationData();
            locData.consent = BridgeRef.LocationConsent.NONE.toString();
            Log.d(BridgeRef.LOG, "Profile created: Static 'locData' was created from scratch");
        } else {
            Log.d(BridgeRef.LOG, "Profile created: Static 'locData' already existed, using current");
        }

        VersionCollector vc = new VersionCollector();
        vc.getVersion(context, this);
    }

    /**
     *  Updates state of location data request (and outputs message)
     */
    public void updateDataState(LocationState newState) {
        Log.w(BridgeRef.LOG,"--> NEW STATE: " + newState);
        locDataState = newState;
    }

    /**
     *  Updates state of location data request (and outputs message)
     */
    public void updateLocDataConsent(BridgeRef.LocationConsent newConsent) {
        Log.w(BridgeRef.LOG, "--> NEW CONSENT: " + locData.consent + "=>" + newConsent);
        locData.consent = newConsent.toString();
        if(newConsent == BridgeRef.LocationConsent.NONE) {
            locData = new LocationData();
        }
    }

    /**
     *  Sends updated loc data values via Unity callback, following successful fetch
     */
    private void successLocToUnity() {
        unityCallbacks.onLocDataSuccess(
                locData.consent == null ? "" : locData.consent,
                locData.state == null ? "" : locData.state,
                locData.postcode == null ? "" : locData.postcode,
                locData.country_code == null ? "" : locData.country_code,
                locData.postal_code == null ? "" : locData.postal_code,
                locData.admin_area == null ? "" : locData.admin_area,
                locData.sub_admin_area == null ? "" : locData.sub_admin_area,
                locData.locality == null ? "" : locData.locality,
                locData.sub_locality == null ? "" : locData.sub_locality);
    }

    /**
     *  Sends previous/default values via Unity callback, following failed fetch
     */
    private void failureLocToUnity() {
        // Find existing record?
        if(locData != null) {
            unityCallbacks.onLocDataFailure(
                    locData.consent == null ? "" : locData.consent,
                    locData.state == null ? "" : locData.state,
                    locData.postcode == null ? "" : locData.postcode,
                    locData.country_code == null ? "" : locData.country_code,
                    locData.postal_code == null ? "" : locData.postal_code,
                    locData.admin_area == null ? "" : locData.admin_area,
                    locData.sub_admin_area == null ? "" : locData.sub_admin_area,
                    locData.locality == null ? "" : locData.locality,
                    locData.sub_locality == null ? "" : locData.sub_locality);
        }
        // Create NONE default
        else {
            unityCallbacks.onLocDataFailure(
                    BridgeRef.LocationConsent.NONE.toString(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "");
        }
    }

    /**
     * Takes input Consent type and attempts to update Location Data
     */
    public void getLocationData(Context context, BridgeRef.LocationConsent consentType) {
        Log.d(BridgeRef.LOG, "getLocationData(context, " + consentType + " ) called");
        // NONE
        if (consentType == BridgeRef.LocationConsent.NONE) {
            updateDataState(LocationState.UNAVAILABLE);

            // Update locData consent to NONE
            updateLocDataConsent(consentType);
            successLocToUnity();
            return;
        }

        // Start checking...
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        updateDataState(LocationState.CHECKING);

        // GENERAL
        if (consentType == BridgeRef.LocationConsent.GENERAL) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Update locData consent to GENERAL
                //updateLocDataConsent(consentType);

                // Get last (quicker) location if available
                if (currentHasBeenChecked != null && currentHasBeenChecked) {
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener((Activity) context , new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    Log.d(BridgeRef.LOG,consentType + "=" + locData.consent +  " | AdminArea=" + locData.admin_area + ", Locality=" + locData.locality + ", PostalCode=" + locData.postal_code);
                                    updateLocationDataOnSuccessfulFetch(context, location, consentType);
                                    successLocToUnity();
                                }
                            })
                            .addOnFailureListener((Activity) context, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(BridgeRef.LOG,"GENERAL: Failed! Something failed [" + e.toString() + "]");
                                    updateDataState(LocationState.FAILED);
                                    failureLocToUnity();
                                }
                            });

                } else {
                    fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationTokenSource().getToken())
                            .addOnSuccessListener((Activity) context, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    Log.d(BridgeRef.LOG,consentType + "=" + locData.consent +  " | AdminArea=" + locData.admin_area + ", Locality=" + locData.locality + ", PostalCode=" + locData.postal_code);
                                    currentHasBeenChecked = true;
                                    updateLocationDataOnSuccessfulFetch(context, location, consentType);
                                    successLocToUnity();
                                }
                            })
                            .addOnFailureListener((Activity) context, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(BridgeRef.LOG,"GENERAL: Failed! Something failed [" + e.toString() + "]");
                                    updateDataState(LocationState.FAILED);
                                    failureLocToUnity();
                                }
                            });
                }
            } else {
                updateDataState(LocationState.FAILED);
                Log.e(BridgeRef.LOG,"GENERAL: Failed to collect...");
            }
        }
        // PRECISE
        else if (consentType == BridgeRef.LocationConsent.PRECISE) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Update locData consent to PRECISE
                //updateLocDataConsent(consentType);

                if (currentHasBeenChecked != null && currentHasBeenChecked) {
                    Log.d(BridgeRef.LOG,"PRECISE: Using last checked data, should only do this if PRECISE has ALREADY been requested and approved");
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener((Activity) context, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    updateLocationDataOnSuccessfulFetch(context, location, consentType);
                                    successLocToUnity();
                                    Log.d(BridgeRef.LOG,consentType + "=" + locData.consent +  " | AdminArea=" + locData.admin_area + ", Locality=" + locData.locality + ", PostalCode=" + locData.postal_code);
                                }
                            })
                            .addOnFailureListener((Activity) context, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    updateDataState(LocationState.FAILED);
                                    failureLocToUnity();
                                    Log.e(BridgeRef.LOG,"PRECISE: Failed! Something failed [" + e.toString() + "]");
                                }
                            });
                } else {
                    Log.d(BridgeRef.LOG,"PRECISE: Checking current, should only do this if PRECISE has ALREADY been requested and approved");
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY , new CancellationTokenSource().getToken())
                            .addOnSuccessListener((Activity) context, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    Log.d(BridgeRef.LOG,consentType + "=" + locData.consent +  " | AdminArea=" + locData.admin_area + ", Locality=" + locData.locality + ", PostalCode=" + locData.postal_code);
                                    updateLocationDataOnSuccessfulFetch(context, location, consentType);
                                    successLocToUnity();
                                }
                            })
                            .addOnFailureListener((Activity) context, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    updateDataState(LocationState.FAILED);
                                    failureLocToUnity();
                                    Log.e(BridgeRef.LOG,"PRECISE: Failed! Something failed [" + e.toString() + "]");
                                }
                            });
                }
            } else {
                updateDataState(LocationState.FAILED);
                Log.e(BridgeRef.LOG,"PRECISE: Failed to collect...");
            }
        }
        // (Shouldn't get here...)
        else {
            updateDataState(LocationState.FAILED);
            locData.consent = BridgeRef.LocationConsent.NONE.toString();
            Log.d(BridgeRef.LOG, "NONE confirmed - are you sure that is what you were looking for...?");
        }
    }

    /**
     *  Uses Geocoder to create Address instance with which to get location properties that then get added to static locData instance
     */
    private void updateLocationDataOnSuccessfulFetch(Context context, Location location, BridgeRef.LocationConsent consentType) {
        Log.d(BridgeRef.LOG, consentType + ": Success! We have a " + consentType+ " location object now");

        LocationData savedLocData = loadSavedLocation(context);
        if(savedLocData != null )
        {
            locData = savedLocData;
        }
        else{
            Log.w(BridgeRef.LOG, "savedLocData was NULL");
        }

        // Got last known location. In some rare situations this can be null.
        if (location == null) {
            Log.e(BridgeRef.LOG, consentType + ": No wait - Failed! Dang, the location object was null");
            updateDataState(LocationState.FAILED);
            return;
        }

        // Logic to handle location object
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            // Create address list of 1
            List<Address> addresses = geocoder.getFromLocation( location.getLatitude(), location.getLongitude(),1);

            // Assuming it's not empty, update the LocationData object
            if (addresses != null && !addresses.isEmpty()) {
                Log.d(BridgeRef.LOG,"AdminArea:\t\t " + (addresses.get(0).getAdminArea() == null ? "[UNAVAILABLE]" : addresses.get(0).getAdminArea())); // STATE
                Log.d(BridgeRef.LOG,"CountryCode:\t\t " + (addresses.get(0).getCountryCode() == null ? "[UNAVAILABLE]" : addresses.get(0).getCountryCode())); // COUNTRY CODE
                Log.d(BridgeRef.LOG,"Locality:\t\t\t " + (addresses.get(0).getLocality() == null ? "[UNAVAILABLE]" : addresses.get(0).getLocality()));
                Log.d(BridgeRef.LOG,"PostalCode:\t\t " + (addresses.get(0).getPostalCode() == null ? "[UNAVAILABLE]" : addresses.get(0).getPostalCode())); // POSTCODE
                Log.d(BridgeRef.LOG,"SubAdminArea:\t\t " + (addresses.get(0).getSubAdminArea() == null ? "[UNAVAILABLE]" : addresses.get(0).getSubAdminArea()));
                Log.d(BridgeRef.LOG,"SubLocality:\t\t " + (addresses.get(0).getSubLocality() == null ? "[UNAVAILABLE]" : addresses.get(0).getSubLocality()));
                Log.d(BridgeRef.LOG,"\n");

                locData.consent = consentType.toString();
                locData.state = addresses.get(0).getAdminArea() != null ? addresses.get(0).getAdminArea() : null;
                locData.postcode = addresses.get(0).getPostalCode() != null ? addresses.get(0).getPostalCode() : null;
                locData.country_code = addresses.get(0).getCountryCode() != null ? addresses.get(0).getCountryCode() : null;
                locData.postal_code = addresses.get(0).getPostalCode() != null ? addresses.get(0).getPostalCode() : null;
                locData.admin_area = addresses.get(0).getAdminArea() != null ? addresses.get(0).getAdminArea() : null;
                locData.sub_admin_area = addresses.get(0).getSubAdminArea() != null ? addresses.get(0).getSubAdminArea() : null;
                locData.locality = addresses.get(0).getLocality() != null ? addresses.get(0).getLocality() : null;
                locData.sub_locality = addresses.get(0).getSubLocality() != null ? addresses.get(0).getSubLocality() : null;

                updateDataState(LocationState.CHECKED);

                // Now that we have a confirmed address instance, we store this version as the latest valid version
                locData.saveLocation(context);

            } else {
                Log.w(BridgeRef.LOG,"Address array for captured Location was empty");
                updateDataState(LocationState.FAILED);
            }
        } catch (IOException e) {
            updateDataState(LocationState.FAILED);
            Log.e(BridgeRef.LOG,"Failed to update location details while capturing current location: " + e);
        }
    }

    /**
     * Checks location permissions for this app and returns true if either COURSE location or FINE location settings are accepted
     */
    public static BridgeRef.LocationConsent getLocationConsent(Context context) {
        Log.w(BridgeRef.LOG, "getLocationConsent(context) called");
        // PRECISE
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.w(BridgeRef.LOG,"PRECISE: Precise location permission is granted, you can proceed with location-related tasks");
            return BridgeRef.LocationConsent.PRECISE;
        }
        // GENERAL
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.w(BridgeRef.LOG,"GENERAL: General location permission is granted, you can proceed with location-related tasks");
            return BridgeRef.LocationConsent.GENERAL;
        }
        // NONE
        else {
            // Check if the user has denied the permission before
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Log.w(BridgeRef.LOG,"FALSE - User has denied the permission before, you can show a rationale explaining why you need the permission and request it again if the user agrees");
            } else {
                Log.w(BridgeRef.LOG,"FALSE - User has not been asked for permission yet or has denied it with 'Never ask again' checked. You can prompt the user to grant the permission from the settings");
            }
            return BridgeRef.LocationConsent.NONE;
        }
    }

    /**
     * Returns 2-digit ISO 1366-1 alpha-2 country code based on user's device location settings
     */
    public String getIso1366CountryCode() {
        Locale defaultLocale = Locale.getDefault();

        String isoCode = defaultLocale.getCountry();
        //String isoCode3 = defaultLocale.getISO3Country();

        if (isoCode.length() == 3) {
            return ccConverter.get2DigitCode(isoCode);
        } else if (isoCode.length() == 2) {
            return isoCode;
        }
        return null;
    }

    /**
     *  Fetches backup copy of current instance values - returns null if any issues
     */
    public LocationData loadSavedLocation(Context context) {

        File directory = context.getDir(BridgeRef.BACKUP_LOC_DIR, Context.MODE_PRIVATE);
        String filename = BridgeRef.BACKUP_LOC_FILE;

        // Do not continue is file/directory null or issue creating file
        if (directory != null) {
            LocationData ld = null;
            File file = new File(directory, filename);
            if (file.exists()) {
                try {
                    Log.w(BridgeRef.LOG, "Backup location fetching: " + file.getAbsolutePath());
                    FileInputStream fileInputStream = new FileInputStream(file);
                    ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                    ld = (LocationData) objectInputStream.readObject();
                    objectInputStream.close();
                    fileInputStream.close();

                    // Output return results
                    if (ld == null) {
                        Log.d(BridgeRef.LOG, "Backup location fetched: null");
                    } else {
                        Log.d(BridgeRef.LOG, "Backup location fetched: consent=" + ld.consent + ", countryCode=" + ld.country_code + ", adminArea==" + ld.admin_area + ", subAdminArea==" + ld.sub_admin_area + ", locality==" + ld.locality);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    Log.w(BridgeRef.LOG, "Backup location fetch failed: '" + e + "'");
                    e.printStackTrace();
                }
            } else {
                Log.w(BridgeRef.LOG, "Could not load previous location file (does not exist) @" + file.getAbsolutePath());
            }

            return ld;
        }

        Log.w(BridgeRef.LOG, "Backup location failed - could not create file/dir");
        return null;
    }

    /**
     * Listener method to trigger Ad ID request callback
     */
    @Override
    public void sendAdId(String adId) {

        if(adId == null || adId.isEmpty()){
            adId = BridgeRef.AD_ID;
        }
        unityCallbacks.onAdIdRequest(adId);
    }

    /**
     * Starts async process to check device Ad ID (if configured)
     */
    public void requestAdID(Context context) {

        new GetGAIDTask(context, this).execute();
    }
}
