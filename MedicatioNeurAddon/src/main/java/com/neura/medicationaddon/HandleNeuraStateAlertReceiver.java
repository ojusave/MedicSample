package com.neura.medicationaddon;

import android.content.Context;

import com.neura.resources.sensors.SensorType;
import com.neura.standalonesdk.util.NeuraStateAlertReceiver;

/**
 * This class handles missing permissions flow : It's critical to neura to have permissions to
 * locations and notify the user when wifi and network sensors are disabled.
 * for more information go to {@link com.neura.standalonesdk.service.NeuraApiClient#enableNeuraHandingStateAlertMessages(boolean)}
 */

public class HandleNeuraStateAlertReceiver extends NeuraStateAlertReceiver {

    @Override
    public void onDetectedMissingPermission(Context context, String permission) {
    }

    @Override
    public void onDetectedMissingPermissionAfterUserPressedNeverAskAgain(Context context, String permission) {
    }

    @Override
    public void onSensorStateChanged(Context context, SensorType sensorType, boolean isEnabled) {
    }
}
