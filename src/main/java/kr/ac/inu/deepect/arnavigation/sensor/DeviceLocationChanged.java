package kr.ac.inu.deepect.arnavigation.sensor;

import android.location.Location;

public interface DeviceLocationChanged {
    void onChange(Location location);
}