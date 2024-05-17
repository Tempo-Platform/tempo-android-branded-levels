package com.tempoplatform.bl;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Dictionary;

public class TempoBackupData {

    public static Boolean readyForCheck = true;
    public static Dictionary<String, JSONArray> storedMetrics; // TODO: Delete?
    public static Boolean backupCapacityFull = false;
    public static LocationData lastLocationData; // TODO: Delete?

    /**
     * Initial method to be run when app is SDK is started.
     * Marks as accessed once run to ensure it only occurs once per session.
     */
    public static void initCheck(Context context) {

        if (readyForCheck) {
            TempoUtils.Say("TempoBackupData.initCheck() triggered");
            buildMetricArrays(context, false); // Change to true to clean folder
            readyForCheck = false;
        } else {
            TempoUtils.Say("TempoBackupData.initCheck() ignored, already checked this session");
        }
    }

    /**
     * Creates a backed-up file in the device storage in specific allocated directory
     */
    public static void backupData(JSONArray metricsArray, Context context) {

        if (!backupCapacityFull) {
            // Convert the JSONArray to a string
            String jsonString = metricsArray.toString();

            // Get a reference to the app's internal storage directory
            File directory = context.getDir(Constants.BACKUP_METRIC_DIR, Context.MODE_PRIVATE);

            // Create unique filename for the JSON file based on time
            String filename = Long.toString(System.currentTimeMillis());
            filename += Constants.BACKUP_METRIC_FILETYPE;

            TempoUtils.Say("Saving Filename: " + filename);

            // Create a new File object for the JSON file
            File file = new File(directory, filename);

            if(file != null) {
                // Write the JSON data to the file
                try {
                    FileWriter writer = new FileWriter(file);
                    writer.write(jsonString);
                    writer.close();
                } catch (IOException e) {
                    TempoUtils.Warn("Could not write backup metrics: " + e);
                }
            }
        } else {
            TempoUtils.Warn("Cannot store any more backups: FULL", true);
        }
    }

    /**
     * Removes a specific file from the allocation Tempo backups folder
     */
    public static void removeFile(String filename, Context context) {
        // Get a reference to the directory named "my_directory" in the app's internal storage
        File directory = context.getDir("tempoBackup", Context.MODE_PRIVATE);

        // Create a File object for the file you want to delete
        File fileToDelete = new File(directory, filename);

        // Check if the file exists before attempting to delete it
        if (fileToDelete.exists()) {
            boolean deleted = fileToDelete.delete();
            if (deleted) {
                TempoUtils.Say("File deleted successfully! (" + filename + ")");
            } else {
                TempoUtils.Shout("Failed to delete file! (" + filename + ")");
            }
        } else {
            TempoUtils.Shout("File does not exist! (" + filename + ")");
        }
    }

    /**
     * Debugging feature that parses allocated directory and prints out count and error totals
     */
    private static void countOutMetrics(File[] files) {

        if(files != null)
        {
            // Loop through the files and print their names and contents
            int jsonCount = 0;
            int jsonErrors = 0;
            int fileErrors = 0;
            for (File file : files) {
                try {
                    FileReader reader = new FileReader(file);
                    StringBuilder stringBuilder = new StringBuilder();
                    int character;
                    while ((character = reader.read()) != -1) {
                        stringBuilder.append((char) character);
                    }
                    String fileContents = stringBuilder.toString();
                    reader.close();

                    try {
                        JSONArray savedJsonArray = new JSONArray(fileContents);
                        // Cycle through members of JSONArray
                        for (int i = 0; i < savedJsonArray.length(); i++) {
                            jsonCount++;
                        }

                    } catch (JSONException e) {
                        jsonErrors++;
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            TempoUtils.Say("TempoSDK: b/u=" + files.length + "(" + jsonCount + ") err[" + jsonErrors + "|" + fileErrors + "]", true);
        }
        else {
            TempoUtils.Shout("Backup null-value found with passed file object");
        }
    }

    /**
     * Searches the 'tempoBackup' folder created on the device and performs action depending on method parameters
     */
    public static void buildMetricArrays(Context context, Boolean deleteAll) {

        // Get a reference to the directory named "tempoBackup" in the app's internal storage
        File directory = context.getDir("tempoBackup", Context.MODE_PRIVATE);

        if (directory == null) {
            TempoUtils.Shout("Backup null-value found in directory");
        } else {
            // Get an array of all the files in the directory
            File[] files = directory.listFiles();

            if (files == null) {
                TempoUtils.Shout("Backup null-value found in files of " + directory);
            } else {
                TempoUtils.Say("Backup directory/files located");
                // Console output of backups if testing
                countOutMetrics(files);

                // Loop through the files and print their names and contents
                if (files.length > Constants.BACKUP_LIMIT) {
                    backupCapacityFull = true;
                    TempoUtils.Shout("Backup Capacity Full! [" + files.length + "]");
                }

                for (File file : files) {
                    String fileContents = "--";
                    try {
                        FileReader reader = new FileReader(file);
                        StringBuilder stringBuilder = new StringBuilder();
                        int character;
                        while ((character = reader.read()) != -1) {
                            stringBuilder.append((char) character);
                        }
                        fileContents = stringBuilder.toString();
                        reader.close();

                        // If flagged for deletion deletes file and moves on to next on the list
                        if (deleteAll) {
                            removeFile(file.getName(), context);
                            continue;
                        }

                        // Delete any empty files that may have been saved
                        if (fileContents == "[ ]" || fileContents == "[]") {
                            removeFile(file.getName(), context);
                            TempoUtils.Shout("Removing empty array file: " + file.getName());
                            continue;
                        }

                        // Remove if record is too old
                        String fileString = file.getName();
                        String modifiedString = fileString.substring(0, fileString.lastIndexOf("."));
                        long utcTimestamp = Long.parseLong(modifiedString);
                        long currentTime = System.currentTimeMillis();
                        // Using Math.abs in case they change their to some time in the future
                        long diffInMs = Math.abs(currentTime - utcTimestamp);
                        if (diffInMs > Constants.DAYS_LONG * Constants.DELETE_AFTER_DAYS) {
                            removeFile(file.getName(), context);
                        }

                        // Print the file name and contents
                        TempoUtils.Say("PUSHING - Filename: " + file.getName() + " | " + fileContents);

                        // Resend backed up file
                        try {
                            JSONArray savedJsonArray = new JSONArray(fileContents);
                            Metric.pushBackupMetrics(savedJsonArray, file.getName(), context);
                        } catch (JSONException e) {
                            TempoUtils.Shout("ERROR - ** " + e + " ** Filename: " + file.getName() + " | " + fileContents);
                            e.printStackTrace();
                        }

                    } catch (IOException e) {
                        TempoUtils.Shout("ERROR - ** " + e + " ** Filename: " + file.getName() + " | " + fileContents);
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     *  Fetches backup copy of current instance values - returns null if any issues
     */
    public static LocationData loadFromFile(Context context) {

        File directory = context.getDir(Constants.BACKUP_LOC_DIR, Context.MODE_PRIVATE);
        String filename = Constants.BACKUP_LOC_FILE;

        // Do not continue is file/directory null or issue creating file
        if (directory != null && (filename != null && !filename.isEmpty())) {
            File file = new File(directory, filename);
            if (file != null) {

                LocationData ld = null;
                try {
                    TempoUtils.Warn("Backup location fetching: " + file.getAbsolutePath());
                    FileInputStream fileInputStream = new FileInputStream(file);
                    ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                    ld = (LocationData) objectInputStream.readObject();
                    objectInputStream.close();
                    fileInputStream.close();

                    // Output return results
                    if(ld == null) {
                        TempoUtils.Say("Backup location fetched: null");
                    } else {
                        TempoUtils.Say("Backup location fetched: consent=" + ld.consent + ", countryCode=" + ld.country_code + ", adminArea==" + ld.admin_area + ", subAdminArea==" + ld.sub_admin_area + ", locality==" + ld.locality);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    TempoUtils.Shout("Backup location fetch failed: '" + e + "'");
                    e.printStackTrace();
                }

                return ld;
            }
        }

        TempoUtils.Warn("Backup location failed - could not create file/dir");
        return null;
    }
}
