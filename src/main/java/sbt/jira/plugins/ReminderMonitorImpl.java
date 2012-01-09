package sbt.jira.plugins;

import java.util.Date;
import java.util.HashMap;

import javax.annotation.PreDestroy;

import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;

public class ReminderMonitorImpl implements ReminderMonitor, LifecycleAware 
{
	public static final String KEY = ReminderMonitorImpl.class.getName() + ":reminderService";
	private static final String JOB_NAME = ReminderMonitorImpl.class.getName() + ":job";
    private final PluginScheduler pluginScheduler;
    private final ReminderService reminderService;
 
    private long interval = 3600000L; //every hour
//    private long interval = 30000L; //every 30 seconds
 
    public ReminderMonitorImpl(PluginScheduler pluginScheduler, ReminderService reminderService) {
        this.pluginScheduler = pluginScheduler;
        this.reminderService = reminderService;
    }
 
    public void onStart() {
        reschedule(interval);
    }
 
    public void reschedule(long interval) {
        this.interval = interval;
        pluginScheduler.scheduleJob(JOB_NAME, ReminderTask.class, 
        		new HashMap<String,Object>() {{put(KEY, reminderService);}}, new Date(), interval);
    }
    
    @PreDestroy
	public void Destory(){
    	pluginScheduler.unscheduleJob(JOB_NAME);
	}
}
