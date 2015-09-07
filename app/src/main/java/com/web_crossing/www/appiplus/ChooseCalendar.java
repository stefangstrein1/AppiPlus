package com.web_crossing.www.appiplus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class ChooseCalendar extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    private RadioGroup mCalendarsGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_calendar);

        mCalendarsGroup = (RadioGroup)findViewById(R.id.rdCalendars);

        final String[] EVENT_PROJECTION = new String[] {
                CalendarContract.Calendars._ID,                           // 0
                CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
                CalendarContract.Calendars.OWNER_ACCOUNT,                 // 3
                CalendarContract.Calendars.IS_PRIMARY,
                CalendarContract.Calendars.VISIBLE,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.NAME,
                CalendarContract.Calendars.CALENDAR_LOCATION,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        };

        final Cursor cursor = getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                EVENT_PROJECTION, null, null, null);
        // For a full list of available columns see http://tinyurl.com/yfbg76w

        SharedPreferences prefs = getSharedPreferences(AppointmentListActivity.SHAREDPREFFILE, Context.MODE_PRIVATE);

        //final String calendarPrimaryId = MyUtils.getCalendarId(getContentResolver());

        String tmpCalendarPrimaryId = prefs.getString(AppointmentListActivity.CALENDARID, "");

        while (cursor.moveToNext()) {

            int access_level = cursor.getInt(9);

            if(access_level < CalendarContract.Calendars.CAL_ACCESS_OVERRIDE){
                continue;
            }

            final String displayName = cursor.getString(1);

            RadioButton newRadioButton = new RadioButton(this);

            String txtCalendar = cursor.getString(2);

            if(cursor.getInt(4) == 1){
                txtCalendar += "\n(" + getResources().getString(R.string.main_calendar) + ")";
            }
            newRadioButton.setText(txtCalendar);

            long calendarId = cursor.getLong(0);

            if(!tmpCalendarPrimaryId.isEmpty()){
                if(Long.toString(calendarId).equals(tmpCalendarPrimaryId)){
                    newRadioButton.setChecked(true);
                }
            }

            newRadioButton.setId((int)calendarId);

            LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.WRAP_CONTENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT);

            layoutParams.setMargins(0, 12, 0, 12);
            mCalendarsGroup.addView(newRadioButton, 0, layoutParams);
        }

        mCalendarsGroup.setOnCheckedChangeListener(this);
     }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_choose_calendar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId){
        int selection = checkedId;

        SharedPreferences prefs = getSharedPreferences(AppointmentListActivity.SHAREDPREFFILE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(AppointmentListActivity.CALENDARID, Integer.toString(checkedId));
        editor.commit();

        navigateUpTo(new Intent(this, AppointmentListActivity.class));
    }
}
