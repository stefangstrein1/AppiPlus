package com.web_crossing.www.appiplus;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.web_crossing.www.appiplus.Models.MemberAppointments;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Adapter to bind a ToDoItem List to a view
 */
public class AppointmentAdapter extends ArrayAdapter<MemberAppointments> {

    private LayoutInflater layoutInflater;
    /**
     * Adapter context
     */
    Context mContext;

    /**
     * Adapter View layout
     */
    int mLayoutResourceId;

    String hours;

    public AppointmentAdapter(Context context, int layoutResourceId) {
        super(context, layoutResourceId);

        mContext = context;
        mLayoutResourceId = layoutResourceId;

        hours = context.getResources().getString(R.string.hours);

        layoutInflater = LayoutInflater.from(context);
    }

    /**
     * Returns the view for a specific item on the list
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        final MemberAppointments currentItem = getItem(position);

        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_row_layout, null);
            holder = new ViewHolder();
            holder.appointment_title = (TextView) convertView.findViewById(R.id.appointment_title);
            holder.appointment_subtitle = (TextView) convertView.findViewById(R.id.appointment_subtitle);
            holder.appointment_description = (TextView) convertView.findViewById(R.id.appointment_description);
            holder.club_logo = (ImageView) convertView.findViewById(R.id.club_logo);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.appointment_title.setText(currentItem.getTitle());

        String description = currentItem.getDescription();
        if(description != null){
            if(description.length() > 100){
                description = description.substring(0, 100) + "...";
            }
        }

        holder.appointment_description.setText(description);

        long diff = currentItem.getEnd().getTime() - currentItem.getStart().getTime();
        diff = diff / (60 * 60 * 1000);
        holder.appointment_subtitle.setText(MyUtils.convertDate(currentItem.getStart()) + " " + diff + " " + hours);

        DownloadImageTask task =  new DownloadImageTask(holder.club_logo);
        String url =  "http://portal.appiplus.com/Uploads/Clubs/%s/logo.png?w=%d&h=%d";
        url = String.format(url, currentItem.getClubid(), 75, 75);
        task.execute(url);

        return convertView;
    }

    static class ViewHolder {
        TextView appointment_title;
        TextView appointment_subtitle;
        TextView appointment_description;
        ImageView club_logo;
    }
}