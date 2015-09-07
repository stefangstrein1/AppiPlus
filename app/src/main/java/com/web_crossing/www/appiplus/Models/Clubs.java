package com.web_crossing.www.appiplus.Models;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p/>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class Clubs  {

    /**
     * Item text
     */
    @com.google.gson.annotations.SerializedName("name")
    private String name;

    /**
     * Item Id
     */
    @com.google.gson.annotations.SerializedName("id")
    private String id;

    @com.google.gson.annotations.SerializedName("address")
    private String address;

    @com.google.gson.annotations.SerializedName("zip")
    private String zip;

    @com.google.gson.annotations.SerializedName("city")
    private String city;

    @com.google.gson.annotations.SerializedName("country")
    private String country;

    /**
     * ToDoItem constructor
     */
    public Clubs() {

    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns the item id
     */
    public String getId() {
        return id;
    }

    public final void setId(String u_id) {
        id = u_id;
    }

    public String getName() { return name; }
}