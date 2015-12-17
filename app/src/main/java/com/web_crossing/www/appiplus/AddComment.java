package com.web_crossing.www.appiplus;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.web_crossing.www.appiplus.Models.AppointmentComments;
import com.web_crossing.www.appiplus.Models.MemberAppointments;

import java.lang.reflect.Member;
import java.net.MalformedURLException;
import java.util.UUID;


public class AddComment extends ActionBarActivity {

    private MobileServiceClient mClient;
    private MemberAppointments mItem;
    private MobileServiceTable<AppointmentComments> mComments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_comment);

        mItem = getIntent().getParcelableExtra(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT);

        if(mItem == null && savedInstanceState != null){
            mItem = savedInstanceState.getParcelable(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT);
        }

        try {
            mClient = new MobileServiceClient(
                    "https://appiplus.azure-mobile.net/",
                    "eEXnmuvGaUMkxatSJqEAOqsthQhwLo28", this);

            SharedPreferences prefs = getSharedPreferences(AppointmentListActivity.SHAREDPREFFILE, Context.MODE_PRIVATE);
            String userId = prefs.getString(AppointmentListActivity.USERIDPREF, "undefined");

            String token = prefs.getString(AppointmentListActivity.TOKENPREF, "undefined");


            MobileServiceUser user = new MobileServiceUser(userId);
            user.setAuthenticationToken(token);
            mClient.setCurrentUser(user);

            mComments = mClient.getTable(AppointmentComments.class);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);

        outState.putParcelable(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT, mItem);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_comment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //

            Intent detailIntent = new Intent(this, AppointmentDetailActivity.class);
            detailIntent.putExtra(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT, (Parcelable)mItem);

            navigateUpTo(detailIntent);
            return true;
        }
        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    public void addComment(View view) {
        boolean connected = MyUtils.getNetworkState(this);

        if(connected == false){
            navigateUpTo(new Intent(this, AppointmentListActivity.class));
            return;
        }

        //Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        if(message.isEmpty()){
            createAndShowDialog(getResources().getString(R.string.empty_comment), "");
            return;
        }

        AppointmentComments comment = new AppointmentComments();
        comment.setId(UUID.randomUUID().toString());
        comment.setText(message);
        comment.setAppointmentid(mItem.getId().toString());

        SharedPreferences prefs = getSharedPreferences(AppointmentListActivity.SHAREDPREFFILE, Context.MODE_PRIVATE);
        comment.setCreatedby(prefs.getString(AppointmentListActivity.USERNAME, ""));

        InsertRecord(comment);
    }

    private void InsertRecord(final AppointmentComments comment){

        final ProgressDialog progress;
        progress = ProgressDialog.show(this, "", getResources().getString(R.string.insert_comment), true);

        CommentTask task = new CommentTask(this, mItem, comment, mComments, progress);
        task.execute();
    }

    public void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }

    public void createAndShowDialog(final String message, final String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }
}
