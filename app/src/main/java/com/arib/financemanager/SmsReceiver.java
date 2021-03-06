package com.arib.financemanager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Collections;

//handles reading SMS and finding data for the app
public class SmsReceiver extends BroadcastReceiver {

    //log tag for this class
    private String TAG = SmsReceiver.class.getSimpleName();
    //boolean that holds if we should notify the user
    boolean shouldNotify;

    //constructor
    public SmsReceiver() {
        Log.d(TAG, "Receiver: Listening!");
    }

    //Handles what to do on receiving a text
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        shouldNotify = prefs.getBoolean(context.getString(R.string.readsms_key), false);
        // Get the data (SMS data) bound to intent
        Bundle bundle = intent.getExtras();

        SmsMessage[] msgs = null;

        String msg = "";
        String str = "";
        if (bundle != null) {
            // Retrieve the SMS Messages received
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];
            msgs[0] = SmsMessage.createFromPdu((byte[]) pdus[0]);
            str = msgs[0].getOriginatingAddress() + " : ";

            // For every SMS message received
            boolean isEncrypt = false;
            for (int i=0; i < msgs.length; i++) {
                // Convert Object array
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                // Sender's phone number
                if(msgs[i].getMessageBody().length() < 4)
                    continue;
                if(msgs[i].getMessageBody().substring(0,4).equals("!#@$") || isEncrypt) {
                    isEncrypt = true;
                    if((msgs[i].getOriginatingAddress() + " : ").equals(str)) {
                        msg += msgs[i].getMessageBody();
                    }
                }
            }

        }

        //if we have a message and the sms reading is enabled by the user then create and send notification
        if(!msg.equals("") && shouldNotify) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
            Notification.Builder notification = new Notification.Builder(context).setContentTitle("Finance Manager").setContentText("You have received new data!").setSmallIcon(R.drawable.ic_notifications_black_24dp);
            Notification notificationBuilt = notification.build();
            notificationBuilt.flags |= Notification.FLAG_AUTO_CANCEL;
            Intent resultIntent = new Intent(context, MainActivity.class);
            resultIntent.putExtra(context.getString(R.string.notification_key), true);
            resultIntent.putExtra(context.getString(R.string.notification_data_key), msg);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notification.setContentIntent(pendingIntent);
            notificationManager.notify(30, notificationBuilt);
        }

    }
}