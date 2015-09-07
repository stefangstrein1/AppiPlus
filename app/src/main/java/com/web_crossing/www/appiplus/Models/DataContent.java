package com.web_crossing.www.appiplus.Models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by stefang on 4/29/2015.
 */
public class DataContent {

    /**
     * Helper class for providing sample content for user interfaces created by
     * Android template wizards.
     * <p/>
     * TODO: Replace all uses of this class before publishing your app.
     */

        /**
         * An array of sample (dummy) items.
         */
        public static List<MemberAppointments> Appointments = new ArrayList<MemberAppointments>();
        public static List<AppointmentComments> Comments = new ArrayList<AppointmentComments>();

        /**
         * A map of sample (dummy) items, by ID.
         */
        public static Map<String, MemberAppointments> AppointmentsMap = new HashMap<String, MemberAppointments>();
        public static Map<String, AppointmentComments> CommentsMap = new HashMap<String, AppointmentComments>();

        public static void ClearData()
        {
            Appointments.clear();
            AppointmentsMap.clear();

            Comments.clear();
            CommentsMap.clear();
        }

        public static void ClearComments()
        {
            Comments.clear();
            CommentsMap.clear();
        }

        public static void addAppointment(MemberAppointments item) {
            Appointments.add(item);
            AppointmentsMap.put(item.getId(), item);
        }

        public static void addComment(AppointmentComments comment) {
            Comments.add(comment);
            CommentsMap.put(comment.getId(), comment);
        }
    }
