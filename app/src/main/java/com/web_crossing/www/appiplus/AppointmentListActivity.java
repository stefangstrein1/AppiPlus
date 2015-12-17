package com.web_crossing.www.appiplus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;

import android.app.AlertDialog;
import android.os.AsyncTask;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.UserAuthenticationCallback;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.CookieSyncManager;
import android.webkit.CookieManager;

import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;

import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.web_crossing.www.appiplus.Models.MemberAppointments;

import com.microsoft.windowsazure.notifications.NotificationsManager;
/**
 * An activity representing a list of Appointments. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link AppointmentDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link AppointmentListFragment} and the item details
 * (if present) is a {@link AppointmentDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link AppointmentListFragment.Callbacks} interface
 * to listen for item selections.getActivity()
 */

class Appointment{
    public String id;
    public String title;
    public String description;
    public Date start;
    public Date end;
    public String location;
}

public class AppointmentListActivity extends AppCompatActivity
        implements AppointmentListFragment.Callbacks {

    public boolean bAuthenticating = false;
    public final Object mAuthenticationLock = new Object();

    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String USERNAME = "name";
    public static final String USERIDACTIVEDIRECTORY = "uid";
    public static final String TOKENPREF = "tkn";
    public static final String SYNCACTIVATED = "syncactivated";
    public static final String CALENDARID = "calendarid";

    public static final String CALENDARENTRIES = "calendarentries";

    public static final String SENDER_ID = "495027086453";
    /**
     * Mobile Service Client reference
     */
    public static MobileServiceClient mClient;

    /**
     * Mobile Service Table used to access data
     */

    private MobileServiceTable<MemberAppointments> mAppointmentTable;

    /**
     * Adapter to sync the items list with the view
     */
    private AppointmentAdapter mAdapter;

    private class RefreshTokenCacheFilter implements ServiceFilter {

        AtomicBoolean mAtomicAuthenticatingFlag = new AtomicBoolean();

        @Override
        public ListenableFuture<ServiceFilterResponse> handleRequest(
                final ServiceFilterRequest request,
                final NextServiceFilterCallback nextServiceFilterCallback
        )
        {
            // In this example, if authentication is already in progress we block the request
            // until authentication is complete to avoid unnecessary authentications as
            // a result of HTTP status code 401.
            // If authentication was detected, add the token to the request.
            waitAndUpdateRequestToken(request);

            // Send the request down the filter chain
            // retrying up to 5 times on 401 response codes.
            ListenableFuture<ServiceFilterResponse> future = null;
            ServiceFilterResponse response = null;
            int responseCode = 401;
            for (int i = 0; (i < 5 ) && (responseCode == 401); i++)
            {
                future = nextServiceFilterCallback.onNext(request);
                try {
                    response = future.get();
                    responseCode = response.getStatus().getStatusCode();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    if (e.getCause().getClass() == MobileServiceException.class)
                    {
                        MobileServiceException mEx = (MobileServiceException) e.getCause();
                        responseCode = mEx.getResponse().getStatus().getStatusCode();
                        if (responseCode == 401)
                        {
                            // Two simultaneous requests from independent threads could get HTTP status 401.
                            // Protecting against that right here so multiple authentication requests are
                            // not setup to run on the UI thread.
                            // We only want to authenticate once. Requests should just wait and retry
                            // with the new token.
                            if (mAtomicAuthenticatingFlag.compareAndSet(false, true))
                            {
                                // Authenticate on UI thread
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Force a token refresh during authentication.
                                        authenticate(true);
                                    }
                                });
                            }

                            // Wait for authentication to complete then update the token in the request.
                            waitAndUpdateRequestToken(request);
                            mAtomicAuthenticatingFlag.set(false);
                        }
                    }
                }
            }
            return future;
        }
    }

    private void authenticate(boolean bRefreshCache) {

        bAuthenticating = true;

        if (bRefreshCache || !loadUserTokenCache(mClient))
        {
            // New login using the provider and update the token cache.
            mClient.login(MobileServiceAuthenticationProvider.WindowsAzureActiveDirectory,
                    new UserAuthenticationCallback() {
                        @Override
                        public void onCompleted(MobileServiceUser user,
                                                Exception exception, ServiceFilterResponse response) {

                            synchronized(mAuthenticationLock)
                            {
                                if (exception == null) {
                                    cacheUserToken(mClient.getCurrentUser());
                                    refreshItemsFromTable();
                                } else {
                                    createAndShowDialog(exception.getMessage(), "Login Error");
                                }
                                bAuthenticating = false;
                                mAuthenticationLock.notifyAll();
                            }
                        }
                    });
        }
        else
        {
            // Other threads may be blocked waiting to be notified when
            // authentication is complete.
            synchronized(mAuthenticationLock)
            {
                bAuthenticating = false;
                mAuthenticationLock.notifyAll();
            }
            refreshItemsFromTable();
        }
    }

    private void cacheUserToken(MobileServiceUser user)
    {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.commit();

        ListenableFuture<JsonElement> result = mClient.invokeApi("getuserinfo", "GET", new ArrayList<Pair<String, String>>());

        Futures.addCallback(result, new FutureCallback<JsonElement>() {
            @Override
            public void onSuccess(JsonElement result) {
                SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
                Editor editor = prefs.edit();
                editor.putString(USERNAME, result.getAsJsonObject().get("userPrincipalName").toString().replace("\"", ""));
                editor.putString(USERIDACTIVEDIRECTORY, result.getAsJsonObject().get("objectId").toString());
                editor.commit();
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }

    private boolean loadUserTokenCache(MobileServiceClient client)
    {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userId = prefs.getString(USERIDPREF, "undefined");
        if (userId == "undefined")
            return false;
        String token = prefs.getString(TOKENPREF, "undefined");
        if (token == "undefined")
            return false;

        MobileServiceUser user = new MobileServiceUser(userId);
        user.setAuthenticationToken(token);
        client.setCurrentUser(user);

        return true;
    }

    /**
     * Detects if authentication is in progress and waits for it to complete.
     * Returns true if authentication was detected as in progress. False otherwise.
     */
    public boolean detectAndWaitForAuthentication()
    {
        boolean detected = false;
        synchronized(mAuthenticationLock)
        {
            do
            {
                if (bAuthenticating == true)
                    detected = true;
                try
                {
                    mAuthenticationLock.wait(1000);
                }
                catch(InterruptedException e)
                {}
            }
            while(bAuthenticating == true);
        }
        if (bAuthenticating == true)
            return true;

        return detected;
    }

    /**
     * Waits for authentication to complete then adds or updates the token
     * in the X-ZUMO-AUTH request header.
     *
     * @param request
     *            The request that receives the updated token.
     */
    private void waitAndUpdateRequestToken(ServiceFilterRequest request)
    {
        MobileServiceUser user = null;
        if (detectAndWaitForAuthentication())
        {
            user = mClient.getCurrentUser();
            if (user != null)
            {
                request.removeHeader("X-ZUMO-AUTH");
                request.addHeader("X-ZUMO-AUTH", user.getAuthenticationToken());
            }
        }
    }


    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    public String mNotificationAppointmentId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_list);


        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.logo);

        mNotificationAppointmentId = getIntent().getStringExtra("notificationAppointmentId");

    /*    ImageView banner = (ImageView)findViewById(R.id.banner);*/
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        ImageView banner = (ImageView)findViewById(R.id.banner);
        //banner.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
        DownloadImageTask task =  new DownloadImageTask(banner);
        String url =  "http://portal.appiplus.com/Content/Images/banner.png?w=%d&h=%d";

        //url = String.format(url, "1", size.x, 75);
        url = String.format(url, size.x, size.y / 12);
        task.execute(url);

        try {
            // Create the Mobile Service Client instance, using the provided
            // Mobile Service URL and key
            mClient = new MobileServiceClient(
                    "https://appiplus.azure-mobile.net/",
                    "eEXnmuvGaUMkxatSJqEAOqsthQhwLo28", this)
                    .withFilter(new RefreshTokenCacheFilter());

            NotificationsManager.handleNotifications(this, SENDER_ID, MyHandler.class);

            // Get the Mobile Service Table instance to use
            //mAppointmentTable = mClient.getTable(Appointment.class);

            mAppointmentTable = mClient.getTable(MemberAppointments.class);

            // Authenticate passing false to load the current token cache if available.
            authenticate(false);

            if (findViewById(R.id.appointment_detail_container) != null) {
                // The detail container view will be present only in the
                // large-screen layouts (res/values-large and
                // res/values-sw600dp). If this view is present, then the
                // activity should be in two-pane mode.
                mTwoPane = true;

                // In two-pane mode, list items should be given the
                // 'activated' state when touched.
                ((AppointmentListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.appointment_list))
                        .setActivateOnItemClick(true);
            }

            // TODO: If exposing deep links into your app, handle intents here.

        } catch (MalformedURLException e) {
            createAndShowDialog(new Exception("There was an error creating the Mobile Service. Verify the URL"), "Error");
        }

    }

    public void onTaskCompleted(String id){
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(AppointmentDetailFragment.ARG_ITEM_ID, id);
            AppointmentDetailFragment fragment = new AppointmentDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.appointment_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, AppointmentDetailActivity.class);
            detailIntent.putExtra(AppointmentDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }

    private AppointmentListFragment getCurrentFragment(){
        AppointmentListFragment fragment = ((AppointmentListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.appointment_list));

        if(fragment == null || fragment.getAdapter() == null){
            return null;
        }

        return fragment;
    }
    /**
     * Callback method from {@link AppointmentListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(MemberAppointments appointment) {

        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            //arguments.putString(AppointmentDetailFragment.ARG_ITEM_ID, id);
            arguments.putParcelable(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT, appointment);
            AppointmentDetailFragment fragment = new AppointmentDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.appointment_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, AppointmentDetailActivity.class);
            //detailIntent.putExtra(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT, appointment);
            detailIntent.putExtra(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT, (Parcelable)appointment);
            startActivity(detailIntent);
        }
    }

    /**
     * Refresh the list with the items in the Mobile Service Table
     */
    private void refreshItemsFromTable() {

        // Get the items that weren't marked as completed and add them in the
        // adapter

        final ProgressDialog progress;
        progress = ProgressDialog.show(this, "", getResources().getString(R.string.appointments_loading), true);

        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        Boolean tmpSync = prefs.getBoolean(SYNCACTIVATED, true);

        //final String calendarPrimaryId = MyUtils.getCalendarId(getContentResolver());

        String tmpCalendarPrimaryId = prefs.getString(CALENDARID, "");

        if(tmpSync){
            if(tmpCalendarPrimaryId.isEmpty()){
                tmpCalendarPrimaryId = MyUtils.getCalendarId(getContentResolver());

                if(tmpCalendarPrimaryId.isEmpty()){
                    createAndShowDialog(getResources().getString(R.string.no_calendar_found), getResources().getString(R.string.warning) );
                    Editor editor = prefs.edit();
                    editor.putBoolean(SYNCACTIVATED, false);
                    editor.commit();

                    tmpSync = false;
                }
                else{
                    Editor editor = prefs.edit();
                    editor.putString(CALENDARID, tmpCalendarPrimaryId);
                    editor.commit();
                }
            }
        }

        final Boolean sync = tmpSync;

        final Activity currentActivity = this;

        final String calendarPrimaryId = tmpCalendarPrimaryId;

        AppointmentListFragment tmpFragment = ((AppointmentListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.appointment_list));

        if(tmpFragment == null || tmpFragment.getAdapter() == null){
            return;
        }

        ArrayAdapter<MemberAppointments> tmpadapter = tmpFragment.getAdapter();

        tmpadapter.clear();
        //DataContent.ClearData();
        Map tmpcalEntries = MyUtils.getStoredAppointments(currentActivity);

        Iterator it = tmpcalEntries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            tmpadapter.add((MemberAppointments)pair.getValue());
        }

        tmpadapter.notifyDataSetChanged();

        AppointmentListFragment taskFragment = ((AppointmentListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.appointment_list));

        LoadingTask task = new LoadingTask(this,
                mAppointmentTable,
                mNotificationAppointmentId,
                calendarPrimaryId,
                progress,
                sync,
                getContentResolver(),
                taskFragment,
                getResources());

        task.execute();
    }

    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }

    /**
     * Creates a dialog and shows it
     *
     * @param message
     *            The dialog message
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(final String message, final String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(mTwoPane){
            // Inflate the menu items for use in the action bar
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.appointment_detail, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }
*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem item = menu.findItem(R.id.sync_appointments);

        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        Boolean sync = prefs.getBoolean(SYNCACTIVATED, true);

        if(sync){
            item.setTitle(getResources().getString(R.string.sync_appointments_off));
        }
        else {
            item.setTitle(getResources().getString(R.string.sync_appointments_on));
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.trigger_sync){
            refreshItemsFromTable();
        }
        else if(id == R.id.portal){
            Uri uriUrl = Uri.parse("http://www.appiplus.com");
            Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
            startActivity(launchBrowser);
        }
        else if (id == R.id.sync_appointments) {

            SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
            Boolean sync = prefs.getBoolean(SYNCACTIVATED, true);

            if(sync){
                sync = false;
            }
            else{
                sync = true;
            }

            Editor editor = prefs.edit();
            editor.putBoolean(SYNCACTIVATED, sync);
            editor.commit();

            return true;
        }
        else if(id == R.id.choose_calendar) {
            Intent calendarIntent = new Intent(this, ChooseCalendar.class);
            startActivity(calendarIntent);
        }
        else if(id == R.id.signout) {
            SharedPreferences preferences = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
            preferences.edit().remove(USERIDPREF).commit();
            preferences.edit().remove(TOKENPREF).commit();

            mClient.logout();
            trimCache(this);

            CookieSyncManager.createInstance(this);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();

            authenticate(false);
        }
        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }
}
