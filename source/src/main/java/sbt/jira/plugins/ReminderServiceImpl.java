package sbt.jira.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import net.java.ao.Query;

import org.apache.velocity.exception.VelocityException;

import sbt.jira.plugins.entities.Reminder;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.config.properties.LookAndFeelBean;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.attachment.Path;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.mail.Email;
import com.atlassian.mail.MailException;
import com.atlassian.mail.MailFactory;
import com.atlassian.mail.server.SMTPMailServer;
import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.velocity.VelocityManager;
import com.opensymphony.util.TextUtils;

public class ReminderServiceImpl implements ReminderService 
{
	public final static String SUBJECT_TEMPLATE = "templates/email/subject/reminder-email-subject.vm";
	public final static String HTML_BODY_TEMPLATE = "templates/email/html/reminder-email-body.vm";
	public final static String TEXT_BODY_TEMPLATE = "templates/email/text/reminder-email-body.vm";
	
	private final ActiveObjects ao;
	private final IssueManager issueManager;
	private UserManager userManager;
	private ApplicationProperties applicationProperties;
	private JiraAuthenticationContext authenticationContext;
	private VelocityManager velocityManager;
	private String baseUrl;
	
	public ReminderServiceImpl(ActiveObjects ao, IssueManager issueManager, UserManager userManager, ApplicationProperties applicationProperties, JiraAuthenticationContext authenticationContext, VelocityManager velocityManager){
		this.ao = ao;
		this.issueManager = issueManager;
		this.userManager = userManager;
		this.applicationProperties = applicationProperties;
		this.authenticationContext = authenticationContext;
		this.velocityManager = velocityManager;
		baseUrl = this.applicationProperties.getString(APKeys.JIRA_BASEURL);
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
		for(Reminder reminder : reminders) {
			User user = userManager.getUserObject(reminder.getAssigneeId());
			if(user != null) {
				Issue issue = issueManager.getIssueObject(reminder.getIssueId());
				Map<String, Object> context = new HashMap<String, Object>();
				context.put("issue", issue);
				context.put("reminder", reminder);
				LookAndFeelBean lookAndFeelBean = LookAndFeelBean.getInstance(applicationProperties);
				context.put("lfbean", lookAndFeelBean);
				context.put("textutils", new TextUtils());
				context.put("i18n", authenticationContext.getI18nHelper());
				String jiraBase = Path.join(baseUrl, "jira");
				context.put("jiraLogoUrl", Path.join(jiraBase, lookAndFeelBean.getLogoUrl()));
				if(!sendEmail(user, context))
					toDelete.remove(reminder);
			}
		}
		deleteReminders(toDelete);
	}
	
	private boolean sendEmail(User toUser, Map<String, Object> context){
		SMTPMailServer mailServer = MailFactory.getServerManager().getDefaultSMTPMailServer();
		User fromUser = new EmailUser(mailServer.getDefaultFrom());
		Email email = generateEmail(toUser, fromUser, context);
		try {
			mailServer.send(email);
		} catch (MailException e) {
			return false;
		}
		return true;
	}
	
	private Email generateEmail(User toUser, User fromUser, Map<String, Object> context) {
		InputStream subjectTemplate = ClassLoaderUtils. getResourceAsStream(SUBJECT_TEMPLATE, ReminderServiceImpl.class);
		String emailSubjectContent = getContent(subjectTemplate);
		InputStream bodyHtmlTemplate = ClassLoaderUtils. getResourceAsStream(HTML_BODY_TEMPLATE, ReminderServiceImpl.class);
		String emailBodyHtmlContent = getContent(bodyHtmlTemplate);
		InputStream bodyTextTemplate = ClassLoaderUtils. getResourceAsStream(TEXT_BODY_TEMPLATE, ReminderServiceImpl.class);
		String emailBodyTextContent = getContent(bodyTextTemplate);
		String emailSubject = null;
		String emailHtmlBody = null;
		String emailTextBody = null;
		try {
			emailSubject = velocityManager.getEncodedBodyForContent(emailSubjectContent, baseUrl, context);
			emailHtmlBody = velocityManager.getEncodedBodyForContent(emailBodyHtmlContent, baseUrl, context);
			emailTextBody = velocityManager.getEncodedBodyForContent(emailBodyTextContent, baseUrl, context);
		} catch (VelocityException e) {
			e.printStackTrace(); 
		}
		Email email = new Email(toUser.getEmailAddress());
		email.setFrom(fromUser.getEmailAddress());
		email.setSubject(emailSubject);
		Multipart body = generateEmailBody(emailHtmlBody, emailTextBody);
		email.setMultipart(body);
		return email;
	}
	
	private Multipart generateEmailBody(String emailHtmlBody, String emailTextBody){
		MimeMultipart rootMixedMultipart = new MimeMultipart("mixed");
	 
	    MimeMultipart nestedRelatedMultipart = new MimeMultipart("related");
	    MimeBodyPart relatedBodyPart = new MimeBodyPart();
	    try {
		    relatedBodyPart.setContent(nestedRelatedMultipart);
		    rootMixedMultipart.addBodyPart(relatedBodyPart);
		 
		    MimeMultipart messageBody = new MimeMultipart("alternative");
	        MimeBodyPart mimeBodyPart = new MimeBodyPart();
	        nestedRelatedMultipart.addBodyPart(mimeBodyPart);
	        MimeBodyPart bodyPart = mimeBodyPart;
		    bodyPart.setContent(messageBody, "text/alternative");
			
			BodyPart text = new MimeBodyPart();
			BodyPart html = new MimeBodyPart();
			text.setText(emailTextBody);
			messageBody.addBodyPart(text);
			html.setContent(emailHtmlBody, "text/html");
			messageBody.addBodyPart(html);
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return rootMixedMultipart;
	}
	
	private String getContent(InputStream inStream){
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
			StringWriter stringWriter = new StringWriter();
			char buf[] = new char[1024];
			int len;
			while ((len = reader.read(buf, 0, 1024)) != -1) {
				stringWriter.write(buf, 0, len);
			}
			return stringWriter.toString();

		} catch (IOException e) {
			throw new RuntimeException("Could not read template: "+e, e);
			
		} finally {
			try {if (reader != null) reader.close();} catch (IOException e) {}
		}
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
