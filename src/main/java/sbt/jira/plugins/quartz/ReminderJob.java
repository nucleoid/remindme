package sbt.jira.plugins.quartz;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import sbt.jira.plugins.ReminderService;
import sbt.jira.plugins.entities.Reminder;

public class ReminderJob implements Job
{
	private ReminderService reminderService;
	
	public ReminderJob(ReminderService reminderService) {
		this.reminderService = reminderService;
	}
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		Timestamp currentTimestamp =  new Timestamp(Calendar.getInstance().getTime().getTime());
		List<Reminder> reminders = reminderService.findNeededReminders(currentTimestamp);
		reminderService.sendReminderNotifications(reminders);
	}	
}
