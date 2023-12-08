// Colby Whitehouse
// 12/8/2023
// getlatitudelongitude
// This program creates a pixel app that 

package com.example.getlatitudelongitude;

import com.opencsv.CSVWriter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.os.Environment;
import java.io.IOException;
import java.util.Locale;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.io.FileWriter;
import java.io.File;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;


public class MainActivity extends AppCompatActivity {


    private Button logButton;
    private Button exportButton;
    private Button exportPlaceButton;
    private EditText placeNameEditText;
    private EditText fileNameEditText;
    private EditText filePlaceEditText;
    private TextView logOutputTextView;
    private List<String> logEntries = new ArrayList<>();
    private List<String> locationDataList = new ArrayList<>();



    private SensorManager sensorManager;
    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    private FusedLocationProviderClient client;
    private WifiManager wifiManager;

    @Override
    protected void onResume() {
        super.onResume();
        registerSensorListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterSensorListeners();
    }
// Get info from accelerometer
    private void registerSensorListeners() {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void unregisterSensorListeners() {
        sensorManager.unregisterListener(sensorListener);
    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accelerometerReading, 0, event.values.length);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magnetometerReading, 0, event.values.length);
            }

            // Calculate orientation
            if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles);
                float azimuth = orientationAngles[0]; // Compass direction in radians

                // Convert azimuth to degrees
                float azimuthDegrees = (float) Math.toDegrees(azimuth);


                updateCompassDirection(azimuthDegrees);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Handle accuracy changes if needed
        }
    };


    // Update a TextView to display compass direction
    private void updateCompassDirection(float azimuthDegrees) {
        // Update a TextView or any UI element to display the compass direction
        TextView compassDirectionTextView = findViewById(R.id.compassDirectionTextView);
        compassDirectionTextView.setText("Compass Direction: " + azimuthDegrees + "°");
    }

    private android.location.LocationRequest locationRequest;

    private List<String> locationHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create Buttons and text fields

        logButton = findViewById(R.id.logButton);
        exportButton = findViewById(R.id.exportButton);
        exportPlaceButton = findViewById(R.id.exportPlaceButton);
        placeNameEditText = findViewById(R.id.placeNameEditText);
        logOutputTextView = findViewById(R.id.logOutputTextView);

        fileNameEditText = findViewById(R.id.fileNameEditText);
        filePlaceEditText = findViewById(R.id.filePlaceEditText);
        // Call functions when the Buttons are clicked
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logPlaceName();
            }
        });
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportDataToCsv();
            }
        });
        exportPlaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportPlaceDataToCsv();
            }
        });


                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


                client = LocationServices.getFusedLocationProviderClient(MainActivity.this);
                wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

                requestLocationUpdates();


            }
            // This function exports the location data to a .csv
            private void exportDataToCsv() {
                String fileName = fileNameEditText.getText().toString();
                if (!fileName.isEmpty()) {
                    fileName = fileName + ".csv";
                    try {
                        File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(exportDir, fileName);
                        String filePath = file.getAbsolutePath();
                        CSVWriter writer = new CSVWriter(new FileWriter(file));

                        for (String locationData : locationDataList) {
                            String [] entries = locationData.split(",");
                            writer.writeNext(entries);
                        }
                        writer.close();
                        Toast.makeText(MainActivity.this, "Data exported to " + filePath, Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
                    }
                }
                fileNameEditText.setText("");
                locationDataList.clear();
            }
            // This function exports the logged places to a .csv
            private void exportPlaceDataToCsv() {
                String filePlaceName = filePlaceEditText.getText().toString();
                if (!filePlaceName.isEmpty()) {
                    filePlaceName = filePlaceName + ".csv";
                    try {
                        File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(exportDir, filePlaceName);
                        String filePath = file.getAbsolutePath();
                        CSVWriter writer = new CSVWriter(new FileWriter(file));

                        for (String locationData : logEntries) {
                            String [] entries = locationData.split(",");
                            writer.writeNext(entries);
                        }
                        writer.close();
                        Toast.makeText(MainActivity.this, "Data exported to " + filePath, Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
                    }
                }
                filePlaceEditText.setText("");
                logEntries.clear();
            }


            // This lets the user log a location
            private void logPlaceName() {
                String placeName = placeNameEditText.getText().toString();
                if (!placeName.isEmpty()) {
                    String timestamp = getCurrentTimestamp();
                    String logEntry = timestamp + " - " + placeName;
                    logEntries.add(logEntry);
                    updateLogOutput();
                    placeNameEditText.setText(""); // Clear the text field
                }
            }
            // This function gets the current timestamp
            private String getCurrentTimestamp() {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault());
                LocalTime time = LocalTime.now();
                return time.format(formatter);
            }

            private void updateLogOutput() {
                StringBuilder logText = new StringBuilder("Logged Entries:\n");
                for (String entry : logEntries) {
                    logText.append(entry).append("\n");
                }
                logOutputTextView.setText(logText.toString());
            }
            // This is the main function that gets location updates and displays them

            private void requestLocationUpdates() {
                // Check if permission is granted
                if (ActivityCompat.checkSelfPermission(MainActivity.this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                LocationRequest locationRequest = new LocationRequest.Builder(100)
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)  // Set the priority to high accuracy
                        .setIntervalMillis(5000)// Set the update interval in milliseconds
                        .build();  // Build the LocationRequest instance


                LocationCallback locationCallback = new LocationCallback() {

                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            // GGet all the different data

                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            float accuracy = location.getAccuracy();
                            double altitude = location.getAltitude();
                            float speed = location.getSpeed();
                            double displacement = Math.sqrt(Math.pow(latitude - latitude, 2) + Math.pow(longitude - longitude, 2));

                            double targetLatitude = latitude;
                            double targetLongitude = longitude + 90;

                            Location targetLocation = new Location("target");
                            targetLocation.setLatitude(targetLatitude);     // Replace with your target latitude
                            targetLocation.setLongitude(targetLongitude);   // Replace with your target longitude

                            float bearing = location.bearingTo(targetLocation);

                            String timestamp = DateFormat.getTimeInstance().format(new Date());
                            // Store the data in a string

                            String locationInfo = "Latitude: " + latitude + "\nLongitude: " + longitude +
                                    "\nAccuracy: " + accuracy + " meters" +
                                    "\nAltitude: " + altitude + " meters" +
                                    "\nSpeed: " + speed + "m/s" +
                                    "\nBearing: " + bearing + " degrees" +
                                    "\nTimestamp: " + timestamp;
                            TextView textView = findViewById(R.id.location);
                            String locationData = latitude + "," + longitude + "," + accuracy + "," + timestamp + "\n";
                            locationDataList.add(locationData);


                            // Display the data
                            textView.setText("Latitude: " + latitude + "\nLongitude: " + longitude +
                                    "\nAccuracy: " + accuracy + " meters" +
                                    "\nAltitude: " + altitude + " meters" +
                                    "\nSpeed: " + speed + "m/s" +
                                    "\nBearing: " + bearing + " degrees" +
                                    "\nTimestamp:" + timestamp);

                            if (!locationHistory.isEmpty()) {
                                String mostRecentLocation = locationHistory.get(locationHistory.size() - 1); // Get the last entry

                                String[] locationLines = mostRecentLocation.split("\n");
                                String latitudeLine = locationLines[0]; // First line should be Latitude
                                String longitudeLine = locationLines[1]; // Second line should be Longitude
                                String timestampLine = locationLines[6]; // Third line should be Timestamp

                                // Extract latitude and longitude values
                                double last_latitude = Double.parseDouble(latitudeLine.split(": ")[1]);
                                double last_longitude = Double.parseDouble(longitudeLine.split(": ")[1]);

                                // Extract the timestamp
                                String last_timestamp = timestampLine.split(": ")[1];

                                TextView mostRecentTextView = findViewById(R.id.mostRecentTextView);
                                mostRecentTextView.setText("Most Recent Location:\n" +
                                        "Previous Latitude: " + last_latitude + "\n" +
                                        "Previous Longitude: " + last_longitude + "\n" +
                                        "Previous Timestamp: " + last_timestamp);
                            }

                            locationHistory.add(locationInfo);


                            Log.d("LocationUpdate", locationInfo);
                            // Get the Wi-Fi information and display it
                            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                            String ssid = wifiInfo.getSSID();
                            String bssid = wifiInfo.getBSSID();
                            int signalStrength = wifiInfo.getRssi();

                            TextView ssidTextView = findViewById(R.id.ssid);
                            ssidTextView.setText("SSID: " + ssid);

                            TextView bssidTextView = findViewById(R.id.bssid);
                            bssidTextView.setText("BSSID: " + bssid);

                            TextView signalStrengthTextView = findViewById(R.id.signalStrength);
                            signalStrengthTextView.setText("Signal Strength: " + signalStrength + " dBm");
                        }


                    }

                };

                client.requestLocationUpdates(locationRequest, locationCallback, null);
            }

            private void displayLocationHistory() {
                Log.d("LocationHistory", "Displaying location history...");
                for (String locationInfo : locationHistory) {
                    Log.d("LocationHistory", locationInfo);
                }
            }

            private void requestPermission() {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{ACCESS_FINE_LOCATION}, 1);
            }
        }

