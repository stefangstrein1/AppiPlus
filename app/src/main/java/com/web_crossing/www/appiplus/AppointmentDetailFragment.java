package com.web_crossing.www.appiplus;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;


import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;
import com.web_crossing.www.appiplus.Models.AppointmentComments;
import com.web_crossing.www.appiplus.Models.Clubs;
import com.web_crossing.www.appiplus.Models.MemberAppointments;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A fragment representing a single Appointment detail screen.
 * This fragment is either contained in a {@link AppointmentListActivity}
 * in two-pane mode (on tablets) or a {@link AppointmentDetailActivity}
 * on handsets.
 */
public class AppointmentDetailFragment extends Fragment {

    public static final String DETAILSTATE = "detail_state";
    public static final String CURRENTID = "current_id";

    /**
     * Mobile Service Client reference
     */
    private MobileServiceClient mClient;
    private MobileServiceTable<AppointmentComments> mComments;
    private MobileServiceTable<Clubs> mClubsTable;

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    public static final String ARG_MEMBER_APPOINTMENT = "selected_appointment";
    public static final String PREVIOUS_APPOINTMENT = "previous_appointment";

    /**
     * The dummy content this fragment is presenting.
     */
    private MemberAppointments mItem;
    private CommentsAdapter mAdapter;

    /*Timer t;
    TimerTask timer = new TimerTask(){
        @Override
        public void run(){
            getActivity().runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    if(mItem != null){
                        loadComments(mItem.getId(), null);
                    }
                }
            });
        }
    };*/

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable(){
      @Override
      public void run(){
          if(mItem != null){
              loadComments(mItem.getId(), null);
              timerHandler.postDelayed(this, 60000);
          }
      }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AppointmentDetailFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);

        outState.putParcelable(PREVIOUS_APPOINTMENT, mItem);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_MEMBER_APPOINTMENT)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            //mItem = DataContent.AppointmentsMap.get(getArguments().getString(ARG_ITEM_ID));
            try {
                mClient = new MobileServiceClient(
                        "https://appiplus.azure-mobile.net/",
                        "eEXnmuvGaUMkxatSJqEAOqsthQhwLo28", this.getActivity());

                SharedPreferences prefs = getActivity().getSharedPreferences(AppointmentListActivity.SHAREDPREFFILE, Context.MODE_PRIVATE);
                String userId = prefs.getString(AppointmentListActivity.USERIDPREF, "undefined");

                String token = prefs.getString(AppointmentListActivity.TOKENPREF, "undefined");


                MobileServiceUser user = new MobileServiceUser(userId);
                user.setAuthenticationToken(token);
                mClient.setCurrentUser(user);

                mComments = mClient.getTable(AppointmentComments.class);
                mClubsTable = mClient.getTable(Clubs.class);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            mItem = getArguments().getParcelable(ARG_MEMBER_APPOINTMENT);
        }
        else if(savedInstanceState != null){
            mItem = savedInstanceState.getParcelable(PREVIOUS_APPOINTMENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_appointment_detail, container, false);
        final View headerView = inflater.inflate(R.layout.fragment_appointment_detail_header, null, false);

        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            ((TextView) headerView.findViewById(R.id.detail_title)).setText(mItem.getTitle());
            ((TextView) headerView.findViewById(R.id.detail_where)).setText(mItem.getAddress());
            ((TextView) headerView.findViewById(R.id.detail_city)).setText(mItem.getCity());
            ((TextView) headerView.findViewById(R.id.detail_description)).setText(mItem.getDescription());

            long diff = mItem.getEnd().getTime() - mItem.getStart().getTime();
            diff = diff / (60 * 60 * 1000);

            ((TextView) headerView.findViewById(R.id.detail_when_start)).setText(MyUtils.convertDate(mItem.getStart()) + " " + diff + " " + getResources().getString(R.string.hours));

            if(mClient == null){
                // Create the Mobile Service Client instance, using the provided
                // Mobile Service URL and key
            }

            final String clubId = mItem.getClubid();

            final ProgressDialog progress;
            progress = ProgressDialog.show(getActivity(), "", getResources().getString(R.string.comments_loading), true);

            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        final MobileServiceList<Clubs> results = mClubsTable.where().field("id").eq(clubId).execute().get();

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //DataContent.ClearComments();

                                if(results == null){
                                    return;
                                }

                                for (Clubs item : results) {
                                    //DataContent.addComment(item);
                                    ((TextView) headerView.findViewById(R.id.detail_club)).setText(item.getName());
                                }

                                ListView lstComments = (ListView)rootView.findViewById(R.id.listComments);
                                headerView.setPadding(0,12,0,8);
                                lstComments.addHeaderView(headerView);

                                mAdapter = new CommentsAdapter(
                                        getActivity(),
                                        android.R.id.text2);

                                lstComments.setAdapter(mAdapter);


                                if(mItem != null && mComments != null){
                                    loadComments(mItem.getId(), progress);
                                }
                            }
                        });
                    } catch (Exception e){
                        progress.dismiss();
                        AppointmentDetailActivity activity = (AppointmentDetailActivity)getActivity();
                        if(activity != null) {
                            activity.createAndShowDialog(e, "Error");
                        }
                    }

                    return null;
                }
            }.execute();
        }

        timerHandler.postDelayed(timerRunnable, 60000);

        return rootView;
    }

    public CommentsAdapter getAdapter()
    {
        return mAdapter;
    }

    private void loadComments(final String appointmentId, final ProgressDialog progress) {


        // Get the items that weren't marked as completed and add them in the
        // adapter

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                            /*mAppointmentTable.where().field("complete").
                                    eq(val(false)).execute().get();
                    */
                    //final List<Appointments> results = mAppointmentTable.execute().get();

                    MobileServiceList<AppointmentComments> tmpResults = null;

                    if(progress == null && mAdapter.getCount() > 0){
                        AppointmentComments comment = mAdapter.getItem(0);
                        Date created = comment.get__createdAt();

                        tmpResults = mComments.where().field("appointmentid").eq(appointmentId).and().field("__createdAt").gt(created).orderBy("__createdAt", QueryOrder.Ascending).execute().get();
                    }
                    else{
                        tmpResults = mComments.where().field("appointmentid").eq(appointmentId).orderBy("__createdAt", QueryOrder.Descending).execute().get();
                    }


                    final MobileServiceList<AppointmentComments> results = tmpResults;

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //DataContent.ClearComments();

                            if (results == null) {
                                if (progress != null) {
                                    progress.dismiss();
                                }
                                return;
                            }

                            if(progress != null){
                                mAdapter.clear();
                            }

                            for (AppointmentComments item : results) {
                                //DataContent.addComment(item);
                                if(progress != null){
                                    mAdapter.add(item);
                                }
                                else{
                                    mAdapter.insert(item, 0);
                                }
                            }

                            mAdapter.notifyDataSetChanged();
                            if (progress != null) {
                                progress.dismiss();
                            }


                        }
                    });
                } catch (Exception e){
                    if(progress != null){
                        progress.dismiss();
                    }

                    AppointmentDetailActivity activity = (AppointmentDetailActivity)getActivity();
                    if(activity != null) {
                        activity.createAndShowDialog(e, "Error");
                    }
                }

                return null;
            }
        }.execute();
    }

    public MemberAppointments getCurrentAppointment(){
        return mItem;
    }

    public void removeCallbacks(){
        if(timerHandler != null && timerRunnable != null){
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}
