package sbt.jira.plugins;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import sbt.jira.plugins.entities.Reminder;

import com.atlassian.sal.api.scheduling.PluginJob;

public class ReminderTask implements PluginJob 
{
	private ReminderService reminderService;
	
	public ReminderTask(ReminderService reminderService) {
		this.reminderService = reminderService;
	}

	@Override
	public void execute(Map<String, Object> jobDataMap) {
		Timestamp currentTimestamp =  new Timestamp(Calendar.getInstance().getTime().getTime());
		List<Reminder> reminders = reminderService.findNeededReminders(currentTimestamp);
		reminderService.sendReminderNotifications(reminders);
	}
}
