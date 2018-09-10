package com.ustadmobile.test.port.android.testutil;

import com.ustadmobile.core.controller.ClazzListPresenter;
import com.ustadmobile.core.controller.ClazzLogDetailPresenter;
import com.ustadmobile.core.controller.ClazzLogListPresenter;
import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.dao.ClazzDao;
import com.ustadmobile.core.db.dao.ClazzLogAttendanceRecordDao;
import com.ustadmobile.core.db.dao.ClazzLogDao;
import com.ustadmobile.core.db.dao.ClazzMemberDao;
import com.ustadmobile.core.db.dao.FeedEntryDao;
import com.ustadmobile.core.db.dao.PersonDao;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.core.util.UMCalendarUtil;
import com.ustadmobile.core.view.ClassLogDetailView;
import com.ustadmobile.lib.db.entities.Clazz;
import com.ustadmobile.lib.db.entities.ClazzLog;
import com.ustadmobile.lib.db.entities.ClazzMember;
import com.ustadmobile.lib.db.entities.FeedEntry;
import com.ustadmobile.lib.db.entities.Person;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;

/**
 * Database related method util go here. eg: setting the database with fixtures, etc.
 */
public class UmDbTestUtil {

    public static final String TEST_FEED_DESCRIPTION = "This is a test feed. Created from the tests.";
    public static final String TEST_FEED_TITLE = "This is your regular test feed notification.";


    public static void addData(boolean clearDb, Object context){

        if (clearDb){
            UmAppDatabase.getInstance(context).clearAllTables();
        }


    }

    /**
     * Creates a Class/Clazz with given name, assigns members with person Uid, sets percentage.
     * @param className The name of the Class / Clazz
     * @param classPercentage   The Class/Clazz attendance percentage
     * @param peopleMap Map of people to create ClazzMembers with
     * @param personUidAssigned The PersonUid override to have access.
     * @param context   The context.
     * @return  The Clazz Class object.
     */
    public static Clazz createClazzWithClazzMembers(String className, float classPercentage,
                                                    Hashtable peopleMap, long personUidAssigned,
                                                    Object context){
        ClazzDao clazzDao = UmAppDatabase.getInstance(context).getClazzDao();
        ClazzMemberDao clazzMemberDao = UmAppDatabase.getInstance(context).getClazzMemberDao();
        PersonDao personDao = UmAppDatabase.getInstance(context).getPersonDao();

        //Create the Clazz
        Clazz testClazz = new Clazz();
        testClazz.setClazzName(className);
        testClazz.setAttendanceAverage(classPercentage);
        testClazz.setClazzUid(clazzDao.insert(testClazz));

        if(personUidAssigned > 0){
            //Create a ClazzMember so that we can view the Classes. this is ideally logged in user
            // or teacher assigned to Class. For testing and start we hardcode it.
            ClazzMember loggedInClazzMember = new ClazzMember();
            loggedInClazzMember.setClazzMemberPersonUid(personUidAssigned);
            loggedInClazzMember.setClazzMemberClazzUid(testClazz.getClazzUid());
            loggedInClazzMember.setRole(ClazzMember.ROLE_TEACHER);
            loggedInClazzMember.setClazzMemberClazzUid(clazzMemberDao.insert(loggedInClazzMember));
        }

        Set<String> peopleMapKeys = peopleMap.keySet();
        for(String name: peopleMapKeys){
            //Create people
            //float personAttendancePercentage = Float.parseFloat(peopleMap.get(name).toString());
            float personAttendancePercentage = (float) peopleMap.get(name);
            Person testPerson = new Person();
            testPerson.setFirstNames(name.split(" ")[0]);
            testPerson.setLastName(name.split(" ")[1]);
            testPerson.setPersonUid(personDao.insert(testPerson));

            //Create ClazzMember
            ClazzMember clazzMember = new ClazzMember();
            clazzMember.setClazzMemberClazzUid(testClazz.getClazzUid());
            clazzMember.setRole(ClazzMember.ROLE_STUDENT);

            clazzMember.setClazzMemberPersonUid(testPerson.getPersonUid());
            clazzMember.setAttendancePercentage(personAttendancePercentage);
            clazzMemberDao.insert(clazzMember);

        }

        return testClazz;

    }

    public static void createClazzLogs(Hashtable clazzLogsToCreate, Clazz thisClazz, Object context){
        //Create a ClassLog for today

        ClazzLogDao clazzLogDao = UmAppDatabase.getInstance(context).getClazzLogDao();
        ClazzLogAttendanceRecordDao clazzLogAttendanceRecordDao =
                UmAppDatabase.getInstance(context).getClazzLogAttendanceRecordDao();


        Set<Long> allDates = clazzLogsToCreate.keySet();
        for(Long thisDate : allDates){
            Hashtable peopleAttendanceMap = (Hashtable) clazzLogsToCreate.get(thisDate);

            ClazzLog newClazzLog = new ClazzLog();
            newClazzLog.setLogDate(thisDate);
            newClazzLog.setDone(false);
            newClazzLog.setClazzClazzUid(thisClazz.getClazzUid());
            newClazzLog.setClazzLogUid(clazzLogDao.insert(newClazzLog));

            clazzLogAttendanceRecordDao.insertAllAttendanceRecords(thisClazz.getClazzUid(),
                    newClazzLog.getClazzLogUid(), null);

        }
    }

    public static void createFeedEntries(Hashtable feedsToCreate, long feedClazzUid,
                                               long feedEntryPersonUid, Object context) {
        FeedEntryDao feedEntryDao = UmAppDatabase.getInstance(context).getFeedEntryDao();

        Set<String> feedMapKeys = feedsToCreate.keySet();
        for(String feedTitle: feedMapKeys){
            long feedLogDate = (long)feedsToCreate.get(feedTitle);
            String feedEntryLink = ClassLogDetailView.VIEW_NAME + "?" +
                    ClazzListPresenter.ARG_CLAZZ_UID + "=" + feedClazzUid + "&" +
                    ClazzLogDetailPresenter.ARG_LOGDATE + "=" + feedLogDate;
            long feedEntryHash = 456;

            FeedEntry thisFeedEntry =  new FeedEntry();
            thisFeedEntry.setDescription(TEST_FEED_DESCRIPTION);
            thisFeedEntry.setTitle(feedTitle);
            thisFeedEntry.setDeadline(feedLogDate);
            thisFeedEntry.setFeedEntryPersonUid(feedEntryPersonUid);
            thisFeedEntry.setLink(feedEntryLink);
            thisFeedEntry.setFeedEntryHash(feedEntryHash);
            thisFeedEntry.setFeedEntryUid(feedEntryDao.insert(thisFeedEntry));

        }






    }
}