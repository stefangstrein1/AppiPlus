package com.web_crossing.www.appiplus.Models;

import java.util.Date;

/**
 * Created by stefang on 4/29/2015.
 */
public class AppointmentComments {

    private String id;

    private String appointmentid;

    private String text;

    private String createdby;

    private Date __createdAt;

    public String getId()
    {
        return id;
    }

    public String getAppointmentid()
    {
        return appointmentid;
    }

    public String getCreatedby()
    {
        return createdby;
    }

    public Date get__createdAt()
    {
        return __createdAt;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String uText) { text = uText; }

    public void setAppointmentid(String uId) { appointmentid = uId; }

    public void setCreatedby(String u_createdby) { createdby = u_createdby; }

    public void setId(String uId) { id = uId; }

    public AppointmentComments()
    {

    }
}
