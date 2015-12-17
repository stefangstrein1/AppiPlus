package com.web_crossing.www.appiplus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;

import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.web_crossing.www.appiplus.Models.AppointmentComments;
import com.web_crossing.www.appiplus.Models.MemberAppointments;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;


/**
 * An activity representing a single Appointment detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link AppointmentListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link AppointmentDetailFragment}.
 */

public class AppointmentDetailActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_detail);

        // Show the Up button in the action bar.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putParcelable(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT,
                    getIntent().getParcelableExtra(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT));

            //arguments.putString(AppointmentDetailFragment.ARG_ITEM_ID,
            //        getIntent().getStringExtra(AppointmentDetailFragment.ARG_ITEM_ID));


            AppointmentDetailFragment fragment = new AppointmentDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.appointment_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean connected = MyUtils.getNetworkState(this);

        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        if(fragments.size() == 0){
            navigateUpTo(new Intent(this, AppointmentListActivity.class));
            return true;
        }

        AppointmentDetailFragment fragment = (AppointmentDetailFragment)fragments.get(0);
        MemberAppointments appointment = fragment.getCurrentAppointment();

        if(appointment == null){
            navigateUpTo(new Intent(this, AppointmentListActivity.class));
            return true;
        }

        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUpTo(new Intent(this, AppointmentListActivity.class));
            return true;
        }
        else if(id == R.id.action_comment)
        {
            if(!connected){
                return true;
            }

            if(appointment == null){
                navigateUpTo(new Intent(this, AppointmentListActivity.class));
                return true;
            }

            Intent commentIntent = new Intent(this, AddComment.class);
            //detailIntent.putExtra(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT, appointment);

            commentIntent.putExtra(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT, (Parcelable)fragment.getCurrentAppointment());
            startActivity(commentIntent);

            return true;
        }
        else if(id == R.id.action_search_location){
            String address = "";
            String city = "";

            try{
                if(appointment.getAddress() == "" || appointment.getCity() == ""){
                    return true;
                }
                address = URLEncoder.encode(appointment.getAddress(), "utf-8");
                city = URLEncoder.encode(appointment.getCity(), "utf-8");

            }
            catch(Exception e){
                createAndShowDialog(e, getResources().getString(R.string.warning));
                return true;
            }

            String zip = appointment.getZip();

            String requestParams =  address;
            if(!requestParams.isEmpty() && !city.isEmpty()){
                requestParams += ",";
                requestParams += city;
            }

            if(!requestParams.isEmpty() && !zip.isEmpty()){
                requestParams += ",";
                requestParams += zip;
            }


            // Create a Uri from an intent string. Use the result to create an Intent.
            Uri gmmIntentUri = Uri.parse(String.format("geo:0,0?q=%s", requestParams));

// Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
// Make the Intent explicit by setting the Google Maps package
            mapIntent.setPackage("com.google.android.apps.maps");

// Attempt to start an activity that can handle the Intent
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            }
            else{
                createAndShowDialog(getResources().getString(R.string.no_map_found), getResources().getString(R.string.warning));
            }
        }
        else if(id == R.id.action_sync_to_calendar){
            SharedPreferences prefs = getSharedPreferences(AppointmentListActivity.SHAREDPREFFILE, Context.MODE_PRIVATE);
            String tmpCalendarPrimaryId = prefs.getString(AppointmentListActivity.CALENDARID, "");

            if(tmpCalendarPrimaryId.isEmpty()){
                tmpCalendarPrimaryId = MyUtils.getCalendarId(getContentResolver());

                if(tmpCalendarPrimaryId.isEmpty()){
                    createAndShowDialog(getResources().getString(R.string.no_calendar_found), getResources().getString(R.string.warning));
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(AppointmentListActivity.SYNCACTIVATED, false);
                    editor.commit();
                    return super.onOptionsItemSelected(item);
                }
                else{
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(AppointmentListActivity.CALENDARID, tmpCalendarPrimaryId);
                    editor.commit();
                }
            }

            Map calEntries = MyUtils.getStoredAppointments(this);
            long eventId = -1;
            if(calEntries.get(appointment.getId()) != null){
                MemberAppointments tmpappointment = (MemberAppointments)calEntries.get(appointment.getId());
                eventId = tmpappointment.getEventId();
            }

            Cursor cursor = null;
            if(eventId != -1){
                cursor = MyUtils.getEvent(getContentResolver(), eventId);
                if(!cursor.moveToFirst()){
                    // shared prefrence has key stored but event has been deleted
                    calEntries.remove(appointment.getId());
                    eventId = -1;
                }
            }

            if(eventId == -1) {
                long eventID = MyUtils.createEvent(getContentResolver(),
                        appointment.getTitle(),
                        appointment.getCalendarDescription(getResources().getString(R.string.where)),
                        appointment.getCalendarAddress(),
                        appointment.start.getTime(),
                        appointment.end.getTime(),
                        tmpCalendarPrimaryId);

                appointment.setEventid(eventID);
                calEntries.put(appointment.getId(), appointment);
            }
            else {
                if (cursor.moveToFirst()) {
                    long eventID = MyUtils.UpdateEvent(getContentResolver(),
                            appointment,
                            cursor,
                            eventId,
                            getResources().getString(R.string.where),
                            tmpCalendarPrimaryId);

                    appointment.setEventid(eventID);
                    calEntries.put(appointment.getId(), appointment);
                }
            }

            MyUtils.setStoredAppointments(calEntries, this);

            createAndShowDialog(getResources().getString(R.string.appointment_created), getResources().getString(R.string.note));
        }

        return super.onOptionsItemSelected(item);
    }

    public void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }

    private void createAndShowDialog(final String message, final String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.appointment_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onPause(){
        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        if(fragments.size() == 0){
            return;
        }

        AppointmentDetailFragment fragment = (AppointmentDetailFragment)fragments.get(0);
        if(fragment == null){
            return;
        }

        fragment.removeCallbacks();

        super.onPause();
    }
}
