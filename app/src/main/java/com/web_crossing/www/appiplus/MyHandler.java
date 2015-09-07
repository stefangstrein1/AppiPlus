package com.web_crossing.www.appiplus;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.microsoft.windowsazure.notifications.NotificationsHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by stefang on 5/11/2015.
 */
public class MyHandler extends NotificationsHandler {

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    Context ctx;

    @Override
    public void onRegistered(Context context,  final String gcmRegistrationId) {
        super.onRegistered(context, gcmRegistrationId);

        final SharedPreferences prefs = context.getSharedPreferences(AppointmentListActivity.SHAREDPREFFILE, Context.MODE_PRIVATE);
        final String userId = prefs.getString(AppointmentListActivity.USERIDACTIVEDIRECTORY, "");

        if(userId.isEmpty()){
            return;
        }

        String isInitialized = prefs.getString("PUSHINITIALIZED", "");

        if(isInitialized.equals("1")){
            return;
        }

        new AsyncTask<Void, Void, Void>() {

            protected Void doInBackground(Void... params) {
                try {
                    String tag = "";

                    if(!userId.isEmpty()){
                        tag = "objectid:" + userId.replace("\"", "");
                    }
                    else{
                        return null;
                    }

                    if(AppointmentListActivity.mClient != null){
                        //List<String> tags = new ArrayList<String>();
                        //tags.add(tag);

                        String[] tags = new String[] {tag};
                        AppointmentListActivity.mClient.getPush().register(gcmRegistrationId, tags);

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("PUSHINITIALIZED", "1");
                        editor.commit();
                    }

                    return null;
                }
                catch(Exception e) {
                     //Log.d("Registration", "Push registration failed " + e.toString());
                }
                return null;
            }
        }.execute();
    }

    @Override
    public void onReceive(Context context, Bundle bundle) {
        ctx = context;
         String nhMessage = bundle.getString("msg");

        sendNotification(nhMessage, context);
    }

    private void sendNotification(String msg, Context context) {
        mNotificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, AppointmentListActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(ctx)
                        .setSmallIcon(R.drawable.ic_action_chat)
                        .setContentTitle(msg)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
