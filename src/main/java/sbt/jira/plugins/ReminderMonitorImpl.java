package sbt.jira.plugins;

import java.util.Date;

import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;

public class ReminderMonitorImpl implements ReminderMonitor, LifecycleAware {
 
    /* package */ static final String KEY = ReminderMonitorImpl.class.getName() + ":instance";
    private static final String JOB_NAME = ReminderMonitorImpl.class.getName() + ":job";
 
    private final PluginScheduler pluginScheduler;  // provided by SAL
 
    private long interval = 5000L;      // default job interval (5 sec)
 
    public ReminderMonitorImpl(PluginScheduler pluginScheduler) {
        this.pluginScheduler = pluginScheduler;
    }
 
    // declared by LifecycleAware
    public void onStart() {
        reschedule(interval);
    }
 
    public void reschedule(long interval) {
        this.interval = interval;
         
//        pluginScheduler.scheduleJob(jobKey, jobClass, jobDataMap, startTime, repeatInterval)
//        scheduleJob(
//                JOB_NAME,                   // unique name of the job
//                ReminderTask.class,     // class of the job
//                null,                         // data that needs to be passed to the job
//                new Date(),                 // the time the job is to start
//                interval);                  // interval between repeats, in milliseconds
    }
}
