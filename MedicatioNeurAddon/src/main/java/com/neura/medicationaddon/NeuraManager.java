package com.neura.medicationaddon;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.neura.resources.authentication.AuthenticateCallback;
import com.neura.resources.authentication.AuthenticateData;
import com.neura.sdk.object.AuthenticationRequest;
import com.neura.sdk.object.Permission;
import com.neura.sdk.service.SubscriptionRequestCallbacks;
import com.neura.standalonesdk.events.NeuraEvent;
import com.neura.standalonesdk.service.NeuraApiClient;
import com.neura.standalonesdk.util.Builder;
import com.neura.standalonesdk.util.SDKUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

public class NeuraManager {

    private static NeuraManager sInstance;

    private NeuraApiClient mNeuraApiClient;

    public static NeuraManager getInstance() {
        if (sInstance == null)
            sInstance = new NeuraManager();
        return sInstance;
    }

    private static final String EVENT_WAKE_UP = "userWokeUp";
    private static final String EVENT_GOT_UP = "userGotUp";
    private static final String EVENT_BEDTIME = "userIsAboutToGoToSleep";
    private static final String EVENT_LEFT_HOME = "userLeftHome";

    /**
     * DO NOT change the values, it correlates with the service which receives the intent.
     */
    public static final String ACTION_MORNING_PILL = "com.neura.medicationaddon.MorningPill";
    public static final String ACTION_EVENING_PILL = "com.neura.medicationaddon.EveningPill";
    public static final String ACTION_PILLBOX_REMINDER = "com.neura.medicationaddon.PillBoxReminder";

    private static final long ONE_MINUTE = 60 * 1000;

    public NeuraApiClient getNeuraClient() {
        return mNeuraApiClient;
    }

    public void initNeuraConnection(Context applicationContext, String appUid, String appSecret) {
        Builder builder = new Builder(applicationContext);
        mNeuraApiClient = builder.build();
        mNeuraApiClient.setAppUid(appUid);
        mNeuraApiClient.setAppSecret(appSecret);
        mNeuraApiClient.connect();
    }

    /**
     * Mandatory for integration with Neura, 1st step - authenticating.
     */
    public void authenticateWithNeura(final Activity activity, final Handler.Callback callback) {

        final SubscriptionRequestCallbacks subscriptionRequestCallbacks = new SubscriptionRequestCallbacks() {
            @Override
            public void onSuccess(String s, Bundle bundle, String s1) {
                Log.i(getClass().getSimpleName(), "Successfully subscribed " + s);
            }

            @Override
            public void onFailure(String s, Bundle bundle, int i) {
                Log.i(getClass().getSimpleName(), "Failed to subscribe " + s + " Reason : " + SDKUtils.errorCodeToString(i));
            }
        };

        final Message message = new Message();

        final ArrayList<Permission> permissions = Permission.list(new String[]{
                EVENT_WAKE_UP, EVENT_GOT_UP, EVENT_BEDTIME, EVENT_LEFT_HOME});

        AuthenticationRequest request = new AuthenticationRequest(permissions);
        mNeuraApiClient.authenticate(request, new AuthenticateCallback() {
            @Override
            public void onSuccess(AuthenticateData authenticateData) {
                Log.i(getClass().getSimpleName(), "Successfully authenticate with neura." + "Token : " + authenticateData.getAccessToken());
                mNeuraApiClient.registerFirebaseToken(activity, FirebaseInstanceId.getInstance().getToken());
                for (int i = 0; i < permissions.size(); i++)
                    mNeuraApiClient.subscribeToEvent(permissions.get(i).getName(),
                            "identifier_" + permissions.get(i).getName(), subscriptionRequestCallbacks);
                message.arg1 = 1;
                callback.handleMessage(message);
                NeuraManager.getInstance().setMorningPillFallback(mNeuraApiClient.getContext());
            }

            @Override
            public void onFailure(int errorCode) {
                Log.e(getClass().getSimpleName(), "Failed to authenticate with neura. Reason : "
                        + SDKUtils.errorCodeToString(errorCode));
                message.arg1 = 0;
                callback.handleMessage(message);
            }
        });
    }

    /**
     * Since the user is playing with the application, we can guarantee that for toady, he/she
     * are already 'awake', so, for today there won't be an event for waking/got up.
     * because of that, we're setting fallback for waking up for tomorrow morning, at 11am.
     * <p>
     * Fallback will be at around 11am(Depending on Android OS, the alarm might be triggered later,
     * since on Marshmallow+ the phone might be on doze.
     *
     * @param context
     */
    public void setMorningPillFallback(Context context) {
        Intent intent = new Intent(context, PillsService.class);
        intent.setAction(ACTION_MORNING_PILL);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long currentTime = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        Calendar calendarNow = Calendar.getInstance();
        calendarNow.setTimeInMillis(currentTime);
        //If the current time now < 11, we're setting the alarm for the 1st trigger to be today at 11. if not - tomorrow at 11.
        calendar.setTimeInMillis(currentTime + (calendarNow.get(Calendar.HOUR_OF_DAY) < 11 ? 0 : ONE_MINUTE * 60 * 24));
        calendar.set(Calendar.HOUR_OF_DAY, 11); //Everyday at
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.setTimeZone(TimeZone.getDefault());

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    private void setTakePillboxReminder(Context context) {
        Intent intent = new Intent(context, PillsService.class);
        intent.setAction(ACTION_PILLBOX_REMINDER);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ONE_MINUTE * 30, pendingIntent);
    }

    /**
     * @param context   current application's context
     * @param eventName {@link NeuraEvent#getEventName()} as received from broadcast receiver which
     *                  listens to com.neura.android.ACTION_NEURA_STATE_ALERT.
     *                  Could be null, when coming from {@link PillsService}
     *                  in this case - this is a {@link #ACTION_MORNING_PILL} for sure.
     */
    public void eventReceived(Context context, String eventName) {
        if (EVENT_WAKE_UP.equalsIgnoreCase(eventName)) {
            generateNotification(context, ACTION_MORNING_PILL);
        } else if (EVENT_GOT_UP.equalsIgnoreCase(eventName)) {
            generateNotification(context, ACTION_MORNING_PILL);
            setTakePillboxReminder(context); //Setting reminder for taking pill box, when userGotUp event is received.
        } else if (EVENT_LEFT_HOME.equalsIgnoreCase(eventName)) {
            generateNotification(context, ACTION_MORNING_PILL);
            generateNotification(context, ACTION_PILLBOX_REMINDER);
        } else if (EVENT_BEDTIME.equalsIgnoreCase(eventName)) {
            generateNotification(context, ACTION_EVENING_PILL);
        }
    }

    /**
     * Call this method when your user 'takes the pill', and you don't want to be notified for it.
     *
     * @param context
     * @param actionPill taken from : {@link #ACTION_MORNING_PILL} or {@link #ACTION_EVENING_PILL}
     *                   or {@link #ACTION_PILLBOX_REMINDER}
     */
    public void stopNotifyPillForToday(Context context, String actionPill) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong(actionPill, System.currentTimeMillis()).commit();
    }

    public void generateNotification(Context context, String action) {
        if (!DateUtils.isToday(PreferenceManager.getDefaultSharedPreferences(context).getLong(action, 0)))
            context.sendBroadcast(new Intent(action));
    }
}
