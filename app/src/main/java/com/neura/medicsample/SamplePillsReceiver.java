package com.neura.medicsample;

import android.content.Context;

import com.neura.medicationaddon.ActionPillsReceiver;

/**
 * Created by ojussave on 12/12/16.
 */

public class SamplePillsReceiver extends ActionPillsReceiver {

    @Override
    protected int getNotificationSmallIcon() {
        return R.mipmap.ic_launcher;
    }

    @Override
    protected void handlePillActionReceived(Context context, String actionPill) {
        super.handlePillActionReceived(context, actionPill);
    }

}
