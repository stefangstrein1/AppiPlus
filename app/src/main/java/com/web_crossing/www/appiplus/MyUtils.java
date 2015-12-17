package com.web_crossing.www.appiplus;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.CalendarContract;
import android.util.Log;

import com.web_crossing.www.appiplus.Models.MemberAppointments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by stefang on 5/21/2015.
 */
public class MyUtils {
    public static boolean compare(String str1, String str2){
        return (str1 == null ? str2 == null : str1.equals(str2));
    }

    public static String getCalendarId(ContentResolver contentResolver){
        // Fetch a list of all calendars synced with the device, their display names and whether the
        // user has them selected for display.

        final String[] EVENT_PROJECTION = new String[] {
                CalendarContract.Calendars._ID,                           // 0
                CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
                CalendarContract.Calendars.OWNER_ACCOUNT,                 // 3
                CalendarContract.Calendars.IS_PRIMARY
        };

        final Cursor cursor = contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
                EVENT_PROJECTION, null, null, null);
        // For a full list of available columns see http://tinyurl.com/yfbg76w

        List<String> calendarIds = new ArrayList<String>();

        String tmpCalendarPrimaryId = "";

        while (cursor.moveToNext()) {

            final String _id = cursor.getString(0);
            final String displayName = cursor.getString(1);

            if(cursor.getInt(4) == 1){
                tmpCalendarPrimaryId = _id;
                break;
            }

            calendarIds.add(_id);
        }
        if(tmpCalendarPrimaryId.isEmpty() && calendarIds.size() > 0){
            tmpCalendarPrimaryId = calendarIds.get(0);
        }

        return tmpCalendarPrimaryId;
    }

    public static Cursor getEvent(ContentResolver contentResolver, long eventId){
        String[] proj = new String[]{
                CalendarContract.Events._ID,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events.CUSTOM_APP_PACKAGE,
                CalendarContract.Events.CUSTOM_APP_URI};

        Cursor cursor = contentResolver.
                query(
                        CalendarContract.Events.CONTENT_URI,
                        proj,
                        CalendarContract.Events._ID + " = ? ",
                        new String[]{Long.toString(eventId)},
                        null);

        return cursor;
    }

    public static Cursor getEvent(ContentResolver contentResolver,
                                  MemberAppointments item){

        String[] proj = new String[]{
                CalendarContract.Events._ID};

        Cursor cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                proj,
                CalendarContract.Events.TITLE + " = ? OR " + CalendarContract.Events.DTSTART + " = ? OR " + CalendarContract.Events.DTEND + " = ?",
                new String[]{item.getTitle(), Long.toString(item.getStart().getTime()), Long.toString(item.getEnd().getTime())},
                null);

        return cursor;
    }

    public static long createEvent(ContentResolver resolver,
                                   String title,
                                   String description,
                                   String address,
                                   long start,
                                   long end,
                                   String calendarPrimaryId){

        ContentValues eventValues = new ContentValues();
        eventValues.put("calendar_id", calendarPrimaryId); // id, We need to choose from
        // our mobile for primary
        // its 1

        eventValues.put(CalendarContract.Events.TITLE, title);
        eventValues.put(CalendarContract.Events.DESCRIPTION, description);
        eventValues.put(CalendarContract.Events.EVENT_LOCATION, address);
        eventValues.put(CalendarContract.Events.DTSTART, start);
        eventValues.put(CalendarContract.Events.DTEND, end);

        // values.put("allDay", 1); //If it is bithday alarm or such
        // kind (which should remind me for whole day) 0 for false, 1
        // for true
        //eventValues.put("eventStatus", 0); // This information is
        // sufficient for most
        // entries tentative (0),
        // confirmed (1) or canceled
        // (2):
   /*Comment below visibility and transparency  column to avoid java.lang.IllegalArgumentException column visibility is invalid error */

    /*eventValues.put("visibility", 3); // visibility to default (0),
                                        // confidential (1), private
                                        // (2), or public (3):
    eventValues.put("transparency", 0); // You can control whether
                                        // an event consumes time
                                        // opaque (0) or transparent
                                        // (1).
      */
        eventValues.put("hasAlarm", 0); // 0 for false, 1 for true

        TimeZone tz = TimeZone.getDefault();
        eventValues.put(CalendarContract.Events.EVENT_TIMEZONE, tz.getID());

        Uri eventUri = resolver.insert(CalendarContract.Events.CONTENT_URI, eventValues);
        long eventID = Long.parseLong(eventUri.getLastPathSegment());

        return eventID;
    }

    public static int deleteEvent(ContentResolver resolver, long eventId){
        String[] selectionArgs = new String[] { Long.toString(eventId)};
        int count = resolver.delete(CalendarContract.Events.CONTENT_URI,
                CalendarContract.Events._ID + " = ? ",
                selectionArgs);

        return count;
    }
    public static long UpdateEvent(ContentResolver resolver,
                                   MemberAppointments item,
                                   Cursor cursor,
                                   long eventId,
                                   String where,
                                   String calendarId){
        // read event data
        Date dtstart = null;
        if(cursor.getString(1) != ""){
            dtstart = new Date(Long.parseLong(cursor.getString(1)));
        }

        Date dtend = null;
        if(cursor.getString(2) != ""){
            dtend = new Date(Long.parseLong(cursor.getString(2)));
        }

        String location = cursor.getString(3);

        String title = cursor.getString(4);

        String appointmentDescription = cursor.getString(5);

        String tmpAddress = item.getCalendarAddress();

        String appointmentCalendarId = cursor.getString(6);

        if(item.start.compareTo(dtstart) != 0 ||
                item.end.compareTo(dtend) != 0 ||
                !item.getTitle().equals(title) ||
                !compare(item.getCalendarAddress(), location) ||
                !compare(item.getCalendarDescription(where), appointmentDescription) ||
                !calendarId.equals(appointmentCalendarId))
        {
            if(!calendarId.equals(appointmentCalendarId)) {
                Uri deleteUri = null;
                deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
                int rows = resolver.delete(deleteUri, null, null);

                long eventID = createEvent(resolver, item.getTitle(), item.getCalendarDescription(where), item.getCalendarAddress(), item.start.getTime(),
                        item.end.getTime(), calendarId);

                return eventID;
            }
            else {
                ContentValues values = new ContentValues();
                values.put(CalendarContract.Events.TITLE, item.getTitle());
                values.put(CalendarContract.Events.DESCRIPTION, item.getCalendarDescription(where));
                values.put(CalendarContract.Events.DTSTART, item.start.getTime());
                values.put(CalendarContract.Events.DTEND, item.end.getTime());
                values.put(CalendarContract.Events.EVENT_LOCATION, item.getCalendarAddress());

                String[] selArgs =
                        new String[]{Long.toString(eventId)};

                int updated = resolver.
                        update(
                                CalendarContract.Events.CONTENT_URI,
                                values,
                                CalendarContract.Events._ID + " =? ",
                                selArgs
                        );
            }
        }

        return -1;
    }

    public static void setStoredAppointments(Map appointments, Activity activity){
        File appointmentFile = getStorageFile("appointmentsCache", true, activity);

        if(appointmentFile == null){
            return;
        }

        ObjectOutputStream out = null;
        try{
            FileOutputStream fileOut = new FileOutputStream(appointmentFile);
            out = new ObjectOutputStream(fileOut);

            out.writeObject(appointments);
        }
        catch(IOException exc){
            try{
                out.close();
            }
            catch(IOException excInner){
                return;
            }
        }

        return;
    }

    public static Map getStoredAppointments(Activity activity){
        File appointments = getStorageFile("appointmentsCache", true, activity);
        if(appointments == null){
            return null;
        }

        if(appointments.length() == 0){
            return new HashMap();
        }

        ObjectInputStream in = null;
        try{

            FileInputStream fileIn = new FileInputStream(appointments);
            in = new ObjectInputStream(fileIn);

            Map tmpMap = (Map)in.readObject();

            return tmpMap;
        }
        catch(IOException  | ClassNotFoundException exc){
            try{
                in.close();
            }
            catch(IOException excInner){
                return null;
            }
        }

        return null;
    }

    public static File getStorageFile(String filename, Boolean persistentData, Activity activity){

        if(isExternalStorageWritable() == false){
            return null;
        }

        String dataDir = "";
        if(persistentData){
            dataDir = Environment.getExternalStorageDirectory() + File.separator + "appiplus";
        }
        else{
            dataDir = activity.getExternalFilesDir(null) + File.separator + "appiplus";
        }

        File appiPlusDir = new File(dataDir);
        if(appiPlusDir.mkdir() || appiPlusDir.isDirectory() ){
            File newFile = new File(dataDir + File.separator + filename);
            if(newFile.exists() == false){
                try{
                    newFile.createNewFile();
                }
                catch(IOException exc){
                    return null;
                }
            }

            return newFile;
        }

        return null;
    }

    public static void writeToStorage(String fileName, String text, Boolean persistent, Activity activity){
        File file = getStorageFile(fileName, persistent, activity);

        if(file ==  null){
            return;
        }

        try{
            FileOutputStream fileOut = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(fileOut);

            pw.print(text.replace("\"", ""));
            pw.flush();

            pw.close();
            fileOut.close();
        }
        catch(IOException exc){
            exc.printStackTrace();
        }
    }

    public static boolean isExternalStorageWritable(){
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state)){
            return true;
        }

        return false;
    }

    public static String convertDate(Date date)
    {
        if(date == null)
        {
            return "-";
        }
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");

        return df.format(date);
    }

    public static boolean getNetworkState(Context pContext)
    {
        ConnectivityManager connect = null;
        connect =  (ConnectivityManager)pContext.getSystemService(pContext.CONNECTIVITY_SERVICE);

        if(connect != null)
        {
            NetworkInfo resultMobile = connect.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            boolean mobileConnected = false;

            if(resultMobile != null){
                mobileConnected = resultMobile.isConnectedOrConnecting();
            }

            NetworkInfo resultWifi = connect.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            boolean wifiConnected = false;

            if(resultWifi != null){
                wifiConnected = resultWifi.isConnectedOrConnecting();
            }

            if (mobileConnected || wifiConnected)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
            return false;
    }
}
