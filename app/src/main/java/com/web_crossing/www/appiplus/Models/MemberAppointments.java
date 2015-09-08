package com.web_crossing.www.appiplus.Models;

import android.os.Parcel;
import android.os.Parcelable;

import com.web_crossing.www.appiplus.R;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p/>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class MemberAppointments implements Parcelable, Serializable {

    /**
     * Item text
     */
    @com.google.gson.annotations.SerializedName("title")
    private String title;

    /**
     * Item Id
     */
    @com.google.gson.annotations.SerializedName("id")
    private String id;

    @com.google.gson.annotations.SerializedName("clubid")
    private String clubid;


    /**
     * Indicates if the item is completed
     */
    @com.google.gson.annotations.SerializedName("description")
    private String description;

    @com.google.gson.annotations.SerializedName("location")
    private String location;

    @com.google.gson.annotations.SerializedName("address")
    private String address;

    @com.google.gson.annotations.SerializedName("zip")
    private String zip;

    @com.google.gson.annotations.SerializedName("city")
    private String city;

    @com.google.gson.annotations.SerializedName("__deleted")
    private boolean __deleted;

    private boolean dataChanged;

    public void setDataChanged(boolean value){
        dataChanged = value;
    }

    public boolean getDataChanged(){
        return dataChanged;
    }

    public Date start;

    public Date end;

    public String clubname;
    /**
     * ToDoItem constructor
     */
    public MemberAppointments() {

    }

    @Override
    public String toString() {
        return getTitle();
    }

    /**
     * Initializes a new ToDoItem
     *
     * @param text
     *            The item text
     * @param id
     *            The item id
     */
    public MemberAppointments(String title, String id) {
        this.setTitle(title);
        this.setId(id);
    }

    public boolean is__deleted(){
        return __deleted;
    }

    private long eventId;

    public long getEventId(){
        return eventId;
    }

    public void setEventid(long u_eventId){
        eventId = u_eventId;
    }

    public String getZip() { return zip; }
    /**
     * Returns the item text
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the item text
     *
     * @param text
     *            text to set
     */
    public final void setTitle(String u_title) {
        title = u_title;
    }

    /**
     * Returns the item id
     */
    public String getId() {
        return id;
    }

    public String getClubid() { return clubid; }

    DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public Date getStart(){
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public String getAddress(){
        return address;
    }

    public String getLocation() { return location; }

    public String getCity(){
        String result = "";

        if(zip != null){
            result += zip + " ";
        }

        if(city != null){
            result += city;
        }
        return result;
    }
    public String getDescription(){
        return description;
    }

    public String getCalendarDescription(String where){
        String calDescription = where + ": " + clubname;
        if(description != null){
            calDescription += "\n\n" + description;
        }

        return calDescription;
    }

    public String getCalendarAddress(){
        String calAddress = getAddress() + " " + getCity();
        if(location != null && !location.isEmpty()){
            calAddress = location + " " + calAddress;
        }

        return calAddress;
    }
    /**
     * Sets the item id
     *
     * @param id
     *            id to set
     */
    public final void setId(String u_id) {
        id = u_id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MemberAppointments && ((MemberAppointments) o).id == id;
    }

    protected MemberAppointments(Parcel in) {
        title = in.readString();
        id = in.readString();
        clubid = in.readString();
        description = in.readString();
        address = in.readString();
        zip = in.readString();
        city = in.readString();
        long tmpStart = in.readLong();
        start = tmpStart != -1 ? new Date(tmpStart) : null;
        long tmpEnd = in.readLong();
        end = tmpEnd != -1 ? new Date(tmpEnd) : null;
        df = (DateFormat) in.readValue(DateFormat.class.getClassLoader());
        clubname = in.readString();
        location = in.readString();
        __deleted = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(id);
        dest.writeString(clubid);
        dest.writeString(description);
        dest.writeString(address);
        dest.writeString(zip);
        dest.writeString(city);
        dest.writeLong(start != null ? start.getTime() : -1L);
        dest.writeLong(end != null ? end.getTime() : -1L);
        dest.writeValue(df);
        dest.writeString(clubname);
        dest.writeString(location);
        dest.writeByte((byte)(__deleted ? 1 : 0));
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<MemberAppointments> CREATOR = new Parcelable.Creator<MemberAppointments>() {
        @Override
        public MemberAppointments createFromParcel(Parcel in) {
            return new MemberAppointments(in);
        }

        @Override
        public MemberAppointments[] newArray(int size) {
            return new MemberAppointments[size];
        }
    };
}