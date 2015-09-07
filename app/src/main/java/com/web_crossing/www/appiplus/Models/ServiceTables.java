package com.web_crossing.www.appiplus.Models;

import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

/**
 * Created by stefang on 4/29/2015.
 */
public class ServiceTables {

    public static MobileServiceTable<MemberAppointments> mAppointmentTable;

    public static MobileServiceTable<AppointmentComments> mComments;
}
