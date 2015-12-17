package com.web_crossing.www.appiplus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.widget.ArrayAdapter;

import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.web_crossing.www.appiplus.Models.MemberAppointments;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by stefang on 12/3/2015.
 */
public class LoadingTask extends AsyncTask<Void, Void, String> {
    Activity currentActivity;
    MobileServiceTable<MemberAppointments> mAppointmentTable;
    String mNotificationAppointmentId;
    String calendarPrimaryId;
    ProgressDialog progress;
    Boolean sync;
    ContentResolver resolver;
    AppointmentListFragment fragment;
    Resources resources;

    public LoadingTask(Activity u_parent,
                       MobileServiceTable<MemberAppointments> u_Table,
                       String u_appointmentId,
                       String u_calId,
                       ProgressDialog u_progress,
                       Boolean u_sync,
                       ContentResolver u_resolver,
                       AppointmentListFragment u_fragment,
                       Resources u_resources){

        this.currentActivity = u_parent;
        this.mAppointmentTable = u_Table;
        this.mNotificationAppointmentId = u_appointmentId;
        this.calendarPrimaryId = u_calId;
        this.progress = u_progress;
        this.sync = u_sync;
        this.resolver = u_resolver;
        this.fragment = u_fragment;
        this.resources = u_resources;
    }

    @Override
    protected void onPostExecute(String result){
        super.onPostExecute(result);

        if(result != "OK"){
            return;
        }

        if(mNotificationAppointmentId != null && !mNotificationAppointmentId.isEmpty()){
            Map calEntries = MyUtils.getStoredAppointments(currentActivity);
            if (calEntries.get(mNotificationAppointmentId) != null) {
                MemberAppointments appointment = (MemberAppointments) calEntries.get(mNotificationAppointmentId);

                Intent detailIntent = new Intent(this.currentActivity, AppointmentDetailActivity.class);
                detailIntent.putExtra(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT, (Parcelable) appointment);
                progress.dismiss();
                this.currentActivity.startActivity(detailIntent);

                return;
            }
        }

        progress.dismiss();
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            final MobileServiceList<MemberAppointments> results = mAppointmentTable.execute().get();

            currentActivity.runOnUiThread(new Runnable() {
                                     @Override
                                     public void run() {

                     if (fragment == null || fragment.getAdapter() == null) {
                         return;
                     }

                     ArrayAdapter<MemberAppointments> adapter = fragment.getAdapter();

                     //DataContent.ClearData();
                     Map calEntries = MyUtils.getStoredAppointments(currentActivity);

                     adapter.clear();

                     Cursor cur = null;

                     Map newCalEntries = new HashMap();

                     //SharedPreferences calEntries = getSharedPreferences(CALENDARENTRIES, 0);

                     for (MemberAppointments item : results) {
                         //DataContent.addAppointment(item);

                         ContentValues eventValues = new ContentValues();

                         //long eventId = calEntries.getLong(item.getId(), -1);

                         if (calEntries == null) {
                             continue;
                         }

                         item.setDataChanged(false);

                         Cursor cursor = null;
                         long eventId = -1;

                         if (calEntries.get(item.getId()) != null) {
                             MemberAppointments appointment = (MemberAppointments) calEntries.get(item.getId());
                             // get eventid
                             eventId = appointment.getEventId();
                             if (eventId != -1) {
                                 // check if event still exists in calendar
                                 cursor = MyUtils.getEvent(resolver, eventId);
                                 if (!cursor.moveToFirst()) {
                                     // if event is not found in calendar reset eventid
                                     eventId = -1;
                                 }
                             }

                             if (item.start.compareTo(appointment.start) != 0 ||
                                     item.end.compareTo(appointment.end) != 0) {

                                 item.setDataChanged(true);
                             }
                         }


                         if (!sync) {
                             continue;
                         }

                         if (eventId == -1 && item.is__deleted() == false) {
                             // check if event exists in calender but not in backup file
                             Cursor eventCursor = MyUtils.getEvent(resolver, item);
                             if (eventCursor.moveToFirst()) {
                                 eventId = eventCursor.getLong(0);
                             } else {
                                 eventId = MyUtils.createEvent(resolver,
                                         item.getTitle(),
                                         item.getCalendarDescription(resources.getString(R.string.where)),
                                         item.getCalendarAddress(),
                                         item.start.getTime(),
                                         item.end.getTime(),
                                         calendarPrimaryId);
                             }

                             item.setEventid(eventId);
                         } else if (eventId >= 0 && item.is__deleted() == true) {
                             // delete event from calendar
                             int count = MyUtils.deleteEvent(resolver, eventId);
                         } else if (eventId >= 0 && item.is__deleted() == false) {
                             if (cursor.moveToFirst()) {
                                 long tmpEventId = MyUtils.UpdateEvent(resolver,
                                         item, cursor, eventId, resources.getString(R.string.where),
                                         calendarPrimaryId);

                                 if (tmpEventId != -1) {
                                     eventId = tmpEventId;
                                     item.setEventid(eventId);
                                 }
                             }
                         }

                         if (item.is__deleted() == false) {
                             MemberAppointments tmpAppointment = (MemberAppointments) calEntries.get(item.getId());
                             if(tmpAppointment != null && tmpAppointment.numcomments_old != null){
                                 item.numcomments_old = tmpAppointment.numcomments_old;
                             }
                             else{
                                 item.numcomments_old = 0;
                             }

                             adapter.add(item);
                             item.setEventid(eventId);
                             newCalEntries.put(item.getId(), item);
                         }
                     }

                     if (sync) {
                         MyUtils.setStoredAppointments(newCalEntries, currentActivity);
                     }

                     adapter.notifyDataSetChanged();
                     }
                 }
            );
        } catch (Exception e){
            return "Error";
        }

        return "OK";
    }
}
