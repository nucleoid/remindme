package sbt.jira.plugins;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;
import sbt.jira.plugins.entities.Reminder;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.mail.MailService;
import com.atlassian.jira.notification.NotificationRecipient;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.mail.MailFactory;
import com.atlassian.mail.server.SMTPMailServer;

public class ReminderServiceImpl implements ReminderService 
{
	public final static String SUBJECT_TEMPLATE = "templates/email/subject/reminder-email-subject";
	public final static String BODY_TEMPLATE = "templates/email/html/reminder-email-body";
	
	private final ActiveObjects ao;
	private final IssueManager issueManager;
	private UserManager userManager;
	private MailService mailService;
	
	public ReminderServiceImpl(ActiveObjects ao, IssueManager issueManager, UserManager userManager, MailService mailService){
		this.ao = ao;
		this.issueManager = issueManager;
		this.userManager = userManager;
		this.mailService = mailService;
	}
	
	@Override
	public Reminder add(Long issueId, String assigneeId,
			Timestamp reminderDate, String comment) {
		final Reminder reminder = ao.create(Reminder.class);
		reminder.setIssueId(issueId);
		reminder.setAssigneeId(assigneeId);
		reminder.setReminderDate(reminderDate);
		reminder.setComment(comment);
        reminder.save();
        return reminder;
	}

	@Override
	public Reminder findById(int id) {
		return ao.get(Reminder.class, id);
	}
	
	@Override
	public List<Reminder> findByIssueId(Long issueId) {
		return new ArrayList<Reminder>(Arrays.asList(ao.find(Reminder.class, Query.select().where("issue_id = ?", issueId))));
	}

	@Override
	public int countByIssueId(Long issueId) {
		return ao.count(Reminder.class, Query.select().where("issue_id = ?", issueId));
	}
	
	@Override
	public List<Reminder> findNeededReminders(Timestamp today) {
		List<Reminder> reminders = new ArrayList<Reminder>(Arrays.asList(ao.find(Reminder.class, Query.select().where("reminder_date <= ?", today))));
		List<Reminder> toDeletes = new ArrayList<Reminder>();
		for(Reminder reminder : reminders) {
			Issue issue = issueManager.getIssueObject(reminder.getIssueId());
			if(issue == null || issue.getResolutionObject() != null) {
				toDeletes.add(reminder);
			}
		}
		for(Reminder toDelete : toDeletes) {
			reminders.remove(toDelete);
		}
		deleteReminders(toDeletes);
		return reminders;
	}

	@Override
	public void deleteReminder(Reminder reminder) {
		ao.delete(reminder);
	}
	
	@Override
	public void deleteReminders(List<Reminder> reminders) {
		Reminder[] reminderArr = new Reminder[reminders.size()];
		reminders.toArray(reminderArr);
		ao.delete(reminderArr);
	}

	@Override
	public void sendReminderNotifications(List<Reminder> reminders) {
		List<Reminder> toDelete = new ArrayList<Reminder>(reminders);
		SMTPMailServer mailServer = MailFactory.getServerManager().getDefaultSMTPMailServer();
		for(Reminder reminder : reminders) {
			User user = userManager.getUserObject(reminder.getAssigneeId());
			if(user != null) {
				Issue issue = issueManager.getIssueObject(reminder.getIssueId());
				User fromUser = new EmailUser(mailServer.getDefaultFrom());
				Map<String, Object> context = new HashMap<String, Object>();
				context.put("issue", issue);
				context.put("reminder", reminder);
				sendEmail(user, fromUser, context);
			}
		}
		deleteReminders(toDelete);
	}
	
	private void sendEmail(User toUser, User fromUser, Map<String, Object> context){
		NotificationRecipient recipient = new NotificationRecipient(toUser.getEmailAddress());
		mailService.sendRenderedMail(fromUser, recipient, SUBJECT_TEMPLATE, BODY_TEMPLATE, context);
	}
	
	private class EmailUser implements User 
	{
		private String email;
		
		public EmailUser(String email){
			this.email = email;
		}
		
		@Override
		public String getName() {
			return "";
		}

		@Override
		public int compareTo(User user) {
			return 0;
		}

		@Override
		public long getDirectoryId() {
			return 0;
		}

		@Override
		public String getDisplayName() {
			return "";
		}

		@Override
		public String getEmailAddress() {
			return email;
		}

		@Override
		public boolean isActive() {
			return true;
		}
	}
}
