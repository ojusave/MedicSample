package com.neura.medicationaddon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by hadas on 16/11/2016.
 */

public abstract class ActionPillsReceiver extends BroadcastReceiver {

    /**
     * If you replace the inner string on these actions, make sure to handle the intent filter of
     * {@link ActionPillsReceiver} in the AndroidManifest.xml file.
     */
    public static final String ACTION_NOTIFICATION_TOOK_PILLS = "com.neura.medication.ACTION_NOTIFICATION_TOOK_PILLS";
    public static final String ACTION_NOTIFICATION_REMIND_ME_LATER = "com.neura.medication.ACTION_NOTIFICATION_REMIND_ME_LATER";
    public static final String ACTION_CURRENT_PILL = "actionCurrentPill";
    public static final String NOTIFICATION_ID = "NotificationId";

    /**
     * In this sample, we're displaying a notification when it's time to take morning/evening pill
     * or a pill box reminder to take when leaving home.
     * This is done with a notification presented to the user.
     * There are 2 options here :
     * 1. If you wish to use oa notifications as well, you can use this class's mechanism, and
     * only provide the morning, evening and pillbox reminder icon yu wish to present in the notification.
     * 2. If you don't want to use a notification to show your user its time to take his/her morning
     * pill, you may override the method {@link #handlePillActionReceived(Context, String)}
     * in a receiver you set in your own class.
     */
    protected int getNotificationSmallIcon() {
        return -1;
    }

    protected int getMorningPillIcon() {
        return R.mipmap.icon_morning;
    }

    protected int getEveningPillIcon() {
        return R.mipmap.icon_evening;
    }

    protected int getPillBoxReminderIcon() {
        return R.mipmap.icon_pillbox;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(getClass().getSimpleName(), "ActionPillsReceiver handle click");

        String action = intent.getAction();

        if (ACTION_NOTIFICATION_TOOK_PILLS.equals(action)) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(intent.getExtras().getInt(NOTIFICATION_ID));
            String actionPill = intent.getExtras().getString(ACTION_CURRENT_PILL);
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putLong(ACTION_NOTIFICATION_TOOK_PILLS + actionPill, System.currentTimeMillis()).commit();

            //Letting the Medication addon know not to bother me again with the pill, since the user
            //clicked 'took the pill' option from the notification action.
            NeuraManager.getInstance().stopNotifyPillForToday(context, actionPill);
        } else if (ACTION_NOTIFICATION_REMIND_ME_LATER.equals(action)) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(intent.getExtras().getInt(NOTIFICATION_ID));
        } else {
            handlePillActionReceived(context, action);
        }
    }

    protected void handlePillActionReceived(Context context, String pillAction) {
        if (NeuraManager.ACTION_MORNING_PILL.equals(pillAction)) {
            displayNotification(context, pillAction, "Good morning!", "Time for your morning pill",
                    getMorningPillIcon(), "I took my pills");
        } else if (NeuraManager.ACTION_EVENING_PILL.equals(pillAction)) {
            displayNotification(context, pillAction, "Good evening!", "Time for your evening pill",
                    getEveningPillIcon(), "I took my pills");
        } else if (NeuraManager.ACTION_PILLBOX_REMINDER.equals(pillAction)) {
            displayNotification(context, pillAction, "Hi, wait..", "Don't forget to take your pillbox",
                    getPillBoxReminderIcon(), "I took my pillbox");
        }
    }

    private void displayNotification(Context context, String actionPill, String header,
                                     String content, int icon, String acceptMessage) {
        PendingIntent defaultPendingIntent = PendingIntent.getBroadcast(context, 1, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentTitle(header)
                .setContentText(content)
                .setSmallIcon(getNotificationSmallIcon())
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), icon))
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .addAction(new NotificationCompat.Action(0, acceptMessage,
                        PendingIntent.getBroadcast(context, icon, new Intent(ActionPillsReceiver.ACTION_NOTIFICATION_TOOK_PILLS).
                                putExtra(ActionPillsReceiver.NOTIFICATION_ID, icon).
                                putExtra(ActionPillsReceiver.ACTION_CURRENT_PILL, actionPill), PendingIntent.FLAG_CANCEL_CURRENT)));
        if (!NeuraManager.ACTION_PILLBOX_REMINDER.equals(actionPill))
            builder.addAction(new NotificationCompat.Action(0, "Remind Me Later",
                    PendingIntent.getBroadcast(context, icon + 1, new Intent(ActionPillsReceiver.ACTION_NOTIFICATION_REMIND_ME_LATER).
                            putExtra(ActionPillsReceiver.NOTIFICATION_ID, icon).
                            putExtra(ActionPillsReceiver.ACTION_CURRENT_PILL, actionPill), PendingIntent.FLAG_CANCEL_CURRENT)));
        builder.setContentIntent(defaultPendingIntent);

        //Need a unique actionId, so we're setting the timestamp for 'remind me later', and timestamp+1 for the 'took pills' action.

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(icon, notification);

    }
}
