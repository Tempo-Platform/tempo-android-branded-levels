package com.tempoplatform.bl;

import static androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale;

import com.google.android.gms.location.LocationRequest;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class TempoProfile {

    public static Boolean currentHasBeenChecked;
    public static LocationData locData;
    public static LocationState locDataState = LocationState.UNCHECKED;
    public static CountryCodeConverter ccConverter;
    public boolean isInterstitial;
    private AdView adView;

    /**
     * Constructor - creates new static locData instance if currently null, and updates consent to NONE
     */
    public TempoProfile(boolean isInterstitial, Context context) {
        this(isInterstitial, context, null); // Call the constructor with the additional parameter and pass null
    }
//
//    public TempoProfile(boolean isInterstitial, Context context) {
////        this.isInterstitial = isInterstitial;
////
////        // Configure static instance if currently null
////        if (TempoProfile.locData == null) {
////
////            TempoProfile.locData = TempoBackupData.loadFromFile(context);
////            if(TempoProfile.locData == null) {
////
////                TempoProfile.locData = new LocationData();
////                TempoProfile.locData.consent = Constants.LocationConsent.NONE.toString();
////                TempoUtils.Say("TempoProfile created: Static 'locData' was created from scratch");
////            } else {
////                TempoUtils.Say("TempoProfile created: Static 'locData' was created from backup");
////            }
////        } else {
////            TempoUtils.Say("TempoProfile created: Static 'locData' already existed, using current");
////        }
//
//        this(isInterstitial, context, null);
//    }

    /**
     * Constructor - creates new static locData instance if currently null, and updates consent to NONE
     */
    public TempoProfile(boolean isInterstitial, Context context, AdView adView) {
        this.adView = adView;
        initSetup(isInterstitial, context);
    }

    /**
     * Handles the setup data for constructors
     */
    private void initSetup(boolean isInterstitial, Context context) {
        this.isInterstitial = isInterstitial;
        this.ccConverter = new CountryCodeConverter();
        // Configure static instance if currently null
        if (TempoProfile.locData == null) {

            TempoProfile.locData = TempoBackupData.loadFromFile(context);
            if(TempoProfile.locData == null) {

                TempoProfile.locData = new LocationData();
                TempoProfile.locData.consent = Constants.LocationConsent.NONE.toString();
                TempoUtils.Say("TempoProfile created: Static 'locData' was created from scratch");
            } else {
                TempoUtils.Say("TempoProfile created: Static 'locData' was created from backup");
            }
        } else {
            TempoUtils.Say("TempoProfile created: Static 'locData' already existed, using current");
        }

    }

    /**
     *  Updates state of location data request (and outputs message)
     */
    public void updateDataState(LocationState newState) {
        TempoUtils.Warn("--> NEW STATE: " + newState);
        locDataState = newState;
    }

    /**
     *  Updates state of location data request (and outputs message)
     */
    public void updateLocDataConsent(Constants.LocationConsent newConsent) {
        TempoUtils.Warn("--> NEW CONSENT: " + TempoProfile.locData.consent + "=>" + newConsent);
        locData.consent = newConsent.toString();
    }

    /**
     * Takes input Consent type and attempts to update Location Data
     */
    public void getLocationData(Context context, Constants.LocationConsent consentType, AdView adView) {

        // NONE
        if (consentType == Constants.LocationConsent.NONE) {
            updateDataState(LocationState.UNAVAILABLE);
            // Update locData consent to NONE
            updateLocDataConsent(consentType);
            return;
        }

        // Start checking...
        FusedLocationProviderClient fusedLocationClient = new FusedLocationProviderClient(context);
        updateDataState(LocationState.CHECKING);

        // GENERAL
        if (consentType == Constants.LocationConsent.GENERAL) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Update locData consent to GENERAL
                updateLocDataConsent(consentType);

                // Get last (quicker) location if available
                if (currentHasBeenChecked != null && currentHasBeenChecked) {
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener((Activity) context, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    TempoUtils.Say(consentType + "=" + TempoProfile.locData.consent +  " | AdminArea=" + TempoProfile.locData.admin_area + ", Locality=" + TempoProfile.locData.locality + ", PostalCode=" + TempoProfile.locData.postal_code);
                                    updateLocationDataOnSuccessfulFetch(context, location);
                                    Metric.updateMetricsWithNewLocationData(isInterstitial, Constants.LocationConsent.GENERAL, context, adView);
                                }
                            })
                            .addOnFailureListener((Activity) context, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    TempoUtils.Shout("GENERAL: Failed! Something failed [" + e.toString() + "]");
                                    updateDataState(LocationState.FAILED);
                                    Metric.updateMetricsWithNewLocationData(isInterstitial, Constants.LocationConsent.GENERAL, context, adView);
                                }
                            });

                } else {
                    fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationTokenSource().getToken())
                            .addOnSuccessListener((Activity) context, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    TempoUtils.Say(consentType + "=" + TempoProfile.locData.consent +  " | AdminArea=" + TempoProfile.locData.admin_area + ", Locality=" + TempoProfile.locData.locality + ", PostalCode=" + TempoProfile.locData.postal_code);
                                    currentHasBeenChecked = true;
                                    updateLocationDataOnSuccessfulFetch(context, location);
                                    Metric.updateMetricsWithNewLocationData(isInterstitial, Constants.LocationConsent.GENERAL, context, adView);
                                }
                            })
                            .addOnFailureListener((Activity) context, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    TempoUtils.Shout("GENERAL: Failed! Something failed [" + e.toString() + "]");
                                    updateDataState(LocationState.FAILED);
                                    Metric.updateMetricsWithNewLocationData(isInterstitial, Constants.LocationConsent.GENERAL, context, adView);
                                }
                            });
                }
            } else {
                updateDataState(LocationState.FAILED);
                TempoUtils.Shout("GENERAL: Failed to collect...");
            }
        }
        // PRECISE
        else if (consentType == Constants.LocationConsent.PRECISE) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Update locData consent to PRECISE
                updateLocDataConsent(consentType);

                if (currentHasBeenChecked != null && currentHasBeenChecked) {
                    TempoUtils.Say("PRECISE: Using last checked data, should only do this if PRECISE has ALREADY been requested and approved");
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener((Activity) context, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    updateLocationDataOnSuccessfulFetch(context, location);
                                    Metric.updateMetricsWithNewLocationData(isInterstitial, Constants.LocationConsent.PRECISE, context, adView);
                                    TempoUtils.Say(consentType + "=" + TempoProfile.locData.consent +  " | AdminArea=" + TempoProfile.locData.admin_area + ", Locality=" + TempoProfile.locData.locality + ", PostalCode=" + TempoProfile.locData.postal_code);
                                }
                            })
                            .addOnFailureListener((Activity) context, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    updateDataState(LocationState.FAILED);
                                    Metric.updateMetricsWithNewLocationData(isInterstitial, Constants.LocationConsent.PRECISE, context, adView);
                                    TempoUtils.Shout("PRECISE: Failed! Something failed [" + e.toString() + "]");
                                }
                            });
                } else {
                    TempoUtils.Say("PRECISE: Checking current, should only do this if PRECISE has ALREADY been requested and approved");
                    fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationTokenSource().getToken())
                            .addOnSuccessListener((Activity) context, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    TempoUtils.Say(consentType + "=" + TempoProfile.locData.consent +  " | AdminArea=" + TempoProfile.locData.admin_area + ", Locality=" + TempoProfile.locData.locality + ", PostalCode=" + TempoProfile.locData.postal_code);
                                    updateLocationDataOnSuccessfulFetch(context, location);
                                    Metric.updateMetricsWithNewLocationData(isInterstitial, Constants.LocationConsent.PRECISE, context, adView);
                                }
                            })
                            .addOnFailureListener((Activity) context, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    updateDataState(LocationState.FAILED);
                                    Metric.updateMetricsWithNewLocationData(isInterstitial, Constants.LocationConsent.PRECISE, context, adView);
                                    TempoUtils.Shout("PRECISE: Failed! Something failed [" + e.toString() + "]");
                                }
                            });
                }
            } else {
                updateDataState(LocationState.FAILED);
                TempoUtils.Shout("PRECISE: Failed to collect...");
            }
        }
        // (Shouldn't get here...)
        else {
            updateDataState(LocationState.FAILED);
            TempoProfile.locData.consent = Constants.LocationConsent.NONE.toString();
            TempoUtils.Say("NONE confirmed - are you sure that is what you were looking for...?");
        }
    }

    /**
     *  Uses Geocoder to create Address instance with which to get location properties that then get added to static locData instance
     */
    private void updateLocationDataOnSuccessfulFetch(Context context, Location location) {
        TempoUtils.Say(TempoProfile.locData.consent + ": Success! We have a " + TempoProfile.locData.consent + " location object now");

        // Got last known location. In some rare situations this can be null.
        if (location == null) {
            TempoUtils.Shout(TempoProfile.locData.consent + ": No wait - Failed! Dang, the location object was null");
            updateDataState(LocationState.FAILED);
            return;
        }

        // Logic to handle location object
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            // Create address list of 1
            List<Address> addresses = geocoder.getFromLocation( location.getLatitude(), location.getLongitude(),1);

            // Assuming it's not empty, update the LocationData object
            if (!addresses.isEmpty()) {
                TempoUtils.Say("AdminArea:\t\t " + (addresses.get(0).getAdminArea() == null ? "[UNAVAILABLE]" : addresses.get(0).getAdminArea())); // STATE
                TempoUtils.Say("CountryCode:\t\t " + (addresses.get(0).getCountryCode() == null ? "[UNAVAILABLE]" : addresses.get(0).getCountryCode())); // COUNTRY CODE
                TempoUtils.Say("Locality:\t\t\t " + (addresses.get(0).getLocality() == null ? "[UNAVAILABLE]" : addresses.get(0).getLocality()));
                TempoUtils.Say("PostalCode:\t\t " + (addresses.get(0).getPostalCode() == null ? "[UNAVAILABLE]" : addresses.get(0).getPostalCode())); // POSTCODE
                TempoUtils.Say("SubAdminArea:\t\t " + (addresses.get(0).getSubAdminArea() == null ? "[UNAVAILABLE]" : addresses.get(0).getSubAdminArea()));
                TempoUtils.Say("SubLocality:\t\t " + (addresses.get(0).getSubLocality() == null ? "[UNAVAILABLE]" : addresses.get(0).getSubLocality()));
                TempoUtils.Say("\n");

                TempoProfile.locData.consent = TempoProfile.locData.consent == null ? Constants.LocationConsent.NONE.toString() : TempoProfile.locData.consent;
                TempoProfile.locData.state = addresses.get(0).getAdminArea() != null ? addresses.get(0).getAdminArea() : null;
                TempoProfile.locData.postcode = addresses.get(0).getPostalCode() != null ? addresses.get(0).getPostalCode() : null;
                TempoProfile.locData.country_code = addresses.get(0).getCountryCode() != null ? addresses.get(0).getCountryCode() : null;
                TempoProfile.locData.postal_code = addresses.get(0).getPostalCode() != null ? addresses.get(0).getPostalCode() : null;
                TempoProfile.locData.admin_area = addresses.get(0).getAdminArea() != null ? addresses.get(0).getAdminArea() : null;
                TempoProfile.locData.sub_admin_area = addresses.get(0).getSubAdminArea() != null ? addresses.get(0).getSubAdminArea() : null;
                TempoProfile.locData.locality = addresses.get(0).getLocality() != null ? addresses.get(0).getLocality() : null;
                TempoProfile.locData.sub_locality = addresses.get(0).getSubLocality() != null ? addresses.get(0).getSubLocality() : null;

                if(adView != null) {
                    // Update top level country_code value
                    if (TempoProfile.locData.country_code != null && !TempoProfile.locData.country_code.isEmpty()) {
                        TempoUtils.Shout("Updating countryCode: " + (adView.currentCountryCode == null ? "NULL" : adView.currentCountryCode) + " => " + TempoProfile.locData.country_code);
                        adView.currentCountryCode = TempoProfile.locData.country_code;
                    } else {
                        TempoUtils.Shout("CountryCode NOT updated due to null/empty value, remains: " + (adView.currentCountryCode == null ? "NULL" : adView.currentCountryCode));
                    }
                }

                updateDataState(LocationState.CHECKED);

                // Now that we have a confirmed address instance, we store this version as the latest valid version
                TempoProfile.locData.backupAsJsonFile(context);
            } else {
                TempoUtils.Warn("Address array for captured Location was empty");
                updateDataState(LocationState.FAILED);
            }
        } catch (IOException e) {
            updateDataState(LocationState.FAILED);
            TempoUtils.Shout("Failed to update location details while capturing current location");
        }
    }

    /**
     * Checks location permissions for this app and returns true if either COURSE location or FINE location settings are accepted
     */
    public static Constants.LocationConsent getLocationConsent(Context context) {

        // PRECISE
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            TempoUtils.Warn("PRECISE: Precise location permission is granted, you can proceed with location-related tasks");
            return Constants.LocationConsent.PRECISE;
        }
        // GENERAL
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            TempoUtils.Warn("GENERAL: General location permission is granted, you can proceed with location-related tasks");
            return Constants.LocationConsent.GENERAL;
        }
        // NONE
        else {
            // Check if the user has denied the permission before
            if (shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                TempoUtils.Warn("FALSE - User has denied the permission before, you can show a rationale explaining why you need the permission and request it again if the user agrees");
            } else {
                TempoUtils.Warn("FALSE - User has not been asked for permission yet or has denied it with 'Never ask again' checked. You can prompt the user to grant the permission from the settings");
            }
            return Constants.LocationConsent.NONE;
        }
    }

    /**
     * Returns 2-digit ISO 1366-1 alpha-2 country code based on user's device location settings
     */
    protected static String getIso1366CountryCode() {
        Locale defaultLocale = Locale.getDefault();

        if (defaultLocale != null) {
            String isoCode = defaultLocale.getCountry();
            //String isoCode3 = defaultLocale.getISO3Country();

            if(isoCode != null) {
                if (isoCode.length() == 3) {
                    return ccConverter.get2DigitCode(isoCode);
                } else if (isoCode.length() == 2) {
                    return isoCode;
                }
            }
        }
        return null;
    }

    public enum LocationState {
        UNCHECKED,
        CHECKING,
        CHECKED,
        FAILED,
        UNAVAILABLE
    }
}

