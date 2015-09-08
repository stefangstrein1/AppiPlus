package com.web_crossing.www.appiplus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Parcelable;

import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.web_crossing.www.appiplus.Models.AppointmentComments;
import com.web_crossing.www.appiplus.Models.MemberAppointments;

/**
 * Created by stefang on 5/7/2015.
 */
public class CommentTask extends AsyncTask<Void, Void, String> {
    Activity parent;
    AppointmentComments comment;
    MobileServiceTable<AppointmentComments> tblComments;
    MemberAppointments appointment;
    final ProgressDialog progress;

    public CommentTask(Activity parent, MemberAppointments appointment, AppointmentComments comment, MobileServiceTable<AppointmentComments> tblComments, ProgressDialog progress){
        this.parent = parent;
        this.comment = comment;
        this.tblComments = tblComments;
        this.appointment = appointment;
        this.progress = progress;
    }

    @Override
    protected String doInBackground(Void... params) {

        try{
            this.tblComments.insert(this.comment).get();
        }catch(Exception exception){
            return exception.toString();
        }

        return "OK";
    }

    @Override
    protected void onPostExecute(String result){
        super.onPostExecute(result);

        if(result == "OK"){
            Intent detailIntent = new Intent(this.parent, AppointmentDetailActivity.class);
            detailIntent.putExtra(AppointmentDetailFragment.ARG_MEMBER_APPOINTMENT, (Parcelable)appointment);

            this.parent.startActivity(detailIntent);

            if(progress != null){
                progress.dismiss();
            }
            return;
        }

        if(progress != null){
            progress.dismiss();
        }
        ((AddComment)parent).createAndShowDialog(result, "Fehler");
    }
}

