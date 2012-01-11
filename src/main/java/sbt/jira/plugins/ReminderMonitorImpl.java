package sbt.jira.plugins;

import java.util.Date;
import java.util.HashMap;

import javax.annotation.PreDestroy;

import sbt.jira.plugins.webwork.AdminWebworkModuleAction;

import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.scheduling.PluginScheduler;

public class ReminderMonitorImpl implements ReminderMonitor, LifecycleAware 
{
	public static final String KEY = ReminderMonitorImpl.class.getName() + ":reminderService";
	private static final String JOB_NAME = ReminderMonitorImpl.class.getName() + ":job";
    private final PluginScheduler pluginScheduler;
    private final ReminderService reminderService;
 
    private long millisecondsInSecond = 1000L;
    private long defaultInterval = 3600000L; //every hour
    private long interval;
 
    public ReminderMonitorImpl(PluginScheduler pluginScheduler, ReminderService reminderService, PluginSettingsFactory pluginSettingsFactory) {
        this.pluginScheduler = pluginScheduler;
        this.reminderService = reminderService;
        
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        String intervalSetting = (String) settings.get(AdminWebworkModuleAction.class.getName() + ".interval");
        if (intervalSetting != null)
        	interval = Integer.parseInt(intervalSetting) * millisecondsInSecond;
        else{
        	PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
	        pluginSettings.put(AdminWebworkModuleAction.class.getName()  +".interval", Long.toString(defaultInterval/millisecondsInSecond));
	        interval = defaultInterval;
        }
    }
 
    public void onStart() {
        reschedule(interval, false);
    }
 
    public void reschedule(long interval, boolean isSeconds) {
    	long toSchedule = interval;
    	if(isSeconds)
    		toSchedule = interval * millisecondsInSecond;
        try{
        	pluginScheduler.unscheduleJob(JOB_NAME);
        }catch(IllegalArgumentException iae){ /* just means the job hasn't been scheduled yet */}
        pluginScheduler.scheduleJob(JOB_NAME, ReminderTask.class, 
        		new HashMap<String,Object>() {{put(KEY, reminderService);}}, new Date(), toSchedule);
    }
    
    @PreDestroy
	public void Destory(){
    	pluginScheduler.unscheduleJob(JOB_NAME);
	}
}
