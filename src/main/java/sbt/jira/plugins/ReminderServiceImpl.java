package sbt.jira.plugins;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.java.ao.Query;
import sbt.jira.plugins.entities.Reminder;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.mail.Email;
import com.atlassian.mail.MailException;
import com.atlassian.mail.MailFactory;
import com.atlassian.mail.server.SMTPMailServer;

public class ReminderServiceImpl implements ReminderService 
{
	private static final String RESOLVED = "Resolved";
	private static final String CLOSED = "Closed";
	
	private final ActiveObjects ao;
	private final IssueManager issueManager;
	private UserManager userManager;
	private JiraAuthenticationContext authenticationContext;
	
	public ReminderServiceImpl(ActiveObjects ao, IssueManager issueManager, UserManager userManager, 
			JiraAuthenticationContext authenticationContext){
		this.ao = ao;
		this.issueManager = issueManager;
		this.userManager = userManager;
		this.authenticationContext = authenticationContext;
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
			if(issue == null || issue.getStatusObject().getName().equals(CLOSED) || issue.getStatusObject().getName().equals(RESOLVED)) {
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
		I18nHelper helper = authenticationContext.getI18nHelper();
		
		for(Reminder reminder : reminders) {
			User user = userManager.getUserObject(reminder.getAssigneeId());
			if(user != null) {
				Email email = new Email(user.getEmailAddress());
				email.setSubject(helper.getText("reminder.email.subject"));
				Issue issue = issueManager.getIssueObject(reminder.getIssueId());
				String comment = reminder.getComment() != null ? reminder.getComment() : "N/A";
				email.setBody(helper.getText("reminder.email.body", issue.getKey(), comment));
				try {
					mailServer.send(email);
				} catch (MailException e) {
					toDelete.remove(reminder);
				}
			}
				
		}
		deleteReminders(toDelete);
	}
}
