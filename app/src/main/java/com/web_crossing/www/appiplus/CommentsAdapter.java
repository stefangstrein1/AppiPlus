package com.web_crossing.www.appiplus;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.web_crossing.www.appiplus.Models.AppointmentComments;
import com.web_crossing.www.appiplus.Models.MemberAppointments;

/**
 * Adapter to bind a ToDoItem List to a view
 */
public class CommentsAdapter extends ArrayAdapter<AppointmentComments> {

    private LayoutInflater layoutInflater;
    /**
     * Adapter context
     */
    Context mContext;

    /**
     * Adapter View layout
     */
    int mLayoutResourceId;

    public CommentsAdapter(Context context, int layoutResourceId) {
        super(context, layoutResourceId);

        mContext = context;
        mLayoutResourceId = layoutResourceId;

        layoutInflater = LayoutInflater.from(context);
    }

    /**
     * Returns the view for a specific item on the list
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        final AppointmentComments currentItem = getItem(position);

        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.comment_row_layout, null);
            holder = new ViewHolder();
            holder.comment_user = (TextView) convertView.findViewById(R.id.comment_user);
            holder.comment_created = (TextView) convertView.findViewById(R.id.comment_created);
            holder.comment_text = (TextView) convertView.findViewById(R.id.comment_text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        convertView.setBackgroundColor(0xE8E8E8E8);
        String createdBy = "";
        if(currentItem.getCreatedby() != null){
            createdBy = currentItem.getCreatedby().replace("@appiplus.com", "");
        }
        holder.comment_user.setText(createdBy);

        holder.comment_created.setText(MyUtils.convertDate(currentItem.get__createdAt()));
        holder.comment_text.setText(currentItem.getText());

        return convertView;
    }

    static class ViewHolder {
        TextView comment_user;
        TextView comment_created;
        TextView comment_text;
    }
}