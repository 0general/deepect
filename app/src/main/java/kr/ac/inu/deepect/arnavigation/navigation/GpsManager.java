package kr.ac.inu.deepect.arnavigation.navigation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;




public class GpsManager {
    private static final int REQUEST_LOCATION = 0x123456;
    private static boolean init = false;

    private static GpsManager instance;

    private LocationManager locManager;
    private Activity appContext;

    private Location lastLocation;
    private LocationListener locationListener;

    private GpsManager() {}

    public void start() {
        try {
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1, locationListener);
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L,1, locationListener);
        } catch(SecurityException ex) {
        }
    }

    public void stop() {
        try {
            locManager.removeUpdates(locationListener);
        } catch(Exception ex) {
        }
    }

    public static GpsManager getInstance() throws Exception {
        if (!init) {
            throw new Exception("you must initialize before using gps manager.");
        }
        return instance;
    }

    public static void init(Context context) {
        if (!init) {
            try {
                instance = new GpsManager();

                instance.locManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
                instance.appContext = (Activity)context;

                init = true;
            } catch (Exception ex) {
                Log.d("Exception: ", "Failed to initialize GPS");
            }
        }
    }

    public void setOnLocationListener(LocationListener locationListener) {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(appContext, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }
        this.locationListener = locationListener;
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1, locationListener);
        locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1, locationListener);

    }

    public Location getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(appContext, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }

        Location location;
        location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if(location == null){
            location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        return location;

    }

    public Location getLastLocation() {
        if(lastLocation == null) {
            lastLocation = getCurrentLocation();
        }
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

}