package it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import sbt.jira.plugins.ReminderServiceImpl;
import sbt.jira.plugins.entities.Reminder;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.mail.MailService;
import com.atlassian.jira.notification.NotificationRecipient;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.mail.Email;
import com.atlassian.mail.MailException;
import com.atlassian.mail.MailFactory;
import com.atlassian.mail.server.MailServerManager;
import com.atlassian.mail.server.SMTPMailServer;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(ReminderServiceImplTest.ReminderServiceImplTestDatabaseUpdater.class)
public class ReminderServiceImplTest 
{
	private static final Long ISSUE_ID = 84l;
	private static final Long ISSUE_ID2 = 85l;
	private static final String ASSIGNEE_ID = "mstatz";
	private static final String ASSIGNEE_ID2 = "statzm";
	private static final Timestamp REMINDER_DATE = new Timestamp(Timestamp.UTC(2011, 12, 30, 19, 28, 5));
	private static final Timestamp REMINDER_DATE2 = new Timestamp(Timestamp.UTC(2012, 1, 03, 19, 28, 5));
	private static final String COMMENT = "This is a reminder";
	
	private EntityManager entityManager;
	private ActiveObjects ao;
	private IssueManager issueManager;
	private UserManager userManager;
	private MailService mailService;
	private ReminderServiceImpl reminderService;
	
	@Before
	public void setUp() throws Exception {
		assertNotNull(entityManager);
		ao = new TestActiveObjects(entityManager);
		issueManager = mock(IssueManager.class);
		userManager = mock(UserManager.class);
		mailService = mock(MailService.class);
		reminderService = new ReminderServiceImpl(ao, issueManager, userManager, mailService);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAdd() {
		final String assigneeId = "kitty";
 
        assertEquals(3, ao.find(Reminder.class).length);
 
        final Reminder add = reminderService.add(ISSUE_ID, assigneeId, REMINDER_DATE, COMMENT);
        assertFalse(add.getID() == 0);
 
        ao.flushAll();
 
        final Reminder[] reminders = ao.find(Reminder.class);
        assertEquals(4, reminders.length);
        Reminder found = null;
        for(Reminder reminder : reminders){
        	if(reminder.getAssigneeId() == assigneeId)
        		found = reminder;
        }
        assertNotNull(found);
	}

	@Test
	public void testFindById() {
		Reminder[] preReminders = ao.find(Reminder.class);
		assertEquals(3, preReminders.length);
		int firstId = preReminders[0].getID();
        ao.flushAll();
        
        final Reminder reminder = reminderService.findById(firstId);
        assertNotNull(reminder);
        assertEquals(firstId, reminder.getID());
        assertEquals(COMMENT, reminder.getComment());
	}
	
	@Test
	public void testFindByIssueId() {       
        assertEquals(3, ao.find(Reminder.class).length);
 
        ao.flushAll();
        
        final List<Reminder> reminders = reminderService.findByIssueId(ISSUE_ID);
        assertEquals(2, reminders.size());
        assertEquals(ISSUE_ID, reminders.get(0).getIssueId());
        assertEquals(ISSUE_ID, reminders.get(1).getIssueId());
        assertEquals(ASSIGNEE_ID, reminders.get(0).getAssigneeId());
        assertEquals(ASSIGNEE_ID2, reminders.get(1).getAssigneeId());
	}

	@Test
	public void testCountByIssueId() {
        assertEquals(3, ao.find(Reminder.class).length);

        ao.flushAll();
        
        final int reminderCount = reminderService.countByIssueId(ISSUE_ID);
        assertEquals(2, reminderCount);
	}
	
	@Test
	public void testFindNeededRemindersWithNeeded() {
		assertEquals(3, ao.find(Reminder.class).length);
		MutableIssue issue = mock(MutableIssue.class);
		when(issue.getResolutionObject()).thenReturn(null);
		when(issueManager.getIssueObject(ISSUE_ID)).thenReturn(issue);
		
		MutableIssue issue2 = mock(MutableIssue.class);
		Resolution resolution = mock(Resolution.class);
		when(issue2.getResolutionObject()).thenReturn(resolution);
		when(issueManager.getIssueObject(ISSUE_ID2)).thenReturn(issue2);
		
        ao.flushAll();
        
        final List<Reminder> reminders = reminderService.findNeededReminders(REMINDER_DATE);
        assertEquals(1, reminders.size());
        assertEquals(ISSUE_ID, reminders.get(0).getIssueId());
        assertEquals(ASSIGNEE_ID, reminders.get(0).getAssigneeId());
        assertEquals(REMINDER_DATE, reminders.get(0).getReminderDate());
        assertEquals(2, ao.find(Reminder.class).length);
	}
	
	@Test
	public void testFindNeededRemindersWithResolved() {
		assertEquals(3, ao.find(Reminder.class).length);
		MutableIssue issue = mock(MutableIssue.class);
		Resolution resolution = mock(Resolution.class);
		when(issue.getResolutionObject()).thenReturn(resolution);
		when(issueManager.getIssueObject(ISSUE_ID)).thenReturn(issue);
        ao.flushAll();
        
        final List<Reminder> reminders = reminderService.findNeededReminders(REMINDER_DATE);
        assertEquals(0, reminders.size());
        assertEquals(1, ao.find(Reminder.class).length);
	}
	
	@Test
	public void testDeleteReminder() {
		List<Reminder> allReminders = new ArrayList<Reminder>(Arrays.asList(ao.find(Reminder.class))); 
		assertEquals(3, allReminders.size());
		 
        ao.flushAll();
        
        reminderService.deleteReminder(allReminders.get(0));
        assertEquals(2, ao.find(Reminder.class).length);
	}
	
	@Test
	public void testDeleteReminders() {
		List<Reminder> allReminders = new ArrayList<Reminder>(Arrays.asList(ao.find(Reminder.class))); 
		assertEquals(3, allReminders.size());
		 
        ao.flushAll();
        allReminders.remove(0);
        
        reminderService.deleteReminders(allReminders);
        assertEquals(1, ao.find(Reminder.class).length);
	}
	
	@Test
	public void testSendReminderNotifications() throws MailException {
		assertEquals(3, ao.find(Reminder.class).length);
		MailServerManager serverManager = mock(MailServerManager.class);
		SMTPMailServer smtpServer = mock(SMTPMailServer.class);
		final String fromEmail = "me@mail.com";
		when(smtpServer.getDefaultFrom()).thenReturn(fromEmail);
		when(serverManager.getDefaultSMTPMailServer()).thenReturn(smtpServer);
		MailFactory.setServerManager(serverManager);
		
		final String issueKey = "ASDF-1";
		List<Reminder> reminders = reminderService.findByIssueId(ISSUE_ID2);
		
		User user = mock(User.class);
		final String userEmail = "mitch@pragmaticcoder.com"; 
		when(user.getEmailAddress()).thenReturn(userEmail);
		when(userManager.getUserObject(ASSIGNEE_ID2)).thenReturn(user);
		
		MutableIssue issue = mock(MutableIssue.class);
		when(issue.getKey()).thenReturn(issueKey);
		when(issueManager.getIssueObject(ISSUE_ID2)).thenReturn(issue);
		
		ao.flushAll();
		reminderService.sendReminderNotifications(reminders);
		
		ArgumentCaptor<User> from = ArgumentCaptor.forClass(User.class);
		ArgumentCaptor<NotificationRecipient> recipient = ArgumentCaptor.forClass(NotificationRecipient.class);
		ArgumentCaptor<String> subjectTemplate = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> bodyTemplate = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Email> argument = ArgumentCaptor.forClass(Email.class);
		ArgumentCaptor<Map<String, Object>> context = new ArgumentCaptor<Map<String, Object>>();

		verify(mailService).sendRenderedMail(from.capture(), recipient.capture(), subjectTemplate.capture(), bodyTemplate.capture(), context.capture());
		
		assertEquals(fromEmail, from.getValue().getEmailAddress());
		assertEquals(userEmail, recipient.getValue().getEmail());
		assertEquals(ReminderServiceImpl.SUBJECT_TEMPLATE, subjectTemplate.getValue());
		assertEquals(ReminderServiceImpl.BODY_TEMPLATE, bodyTemplate.getValue());
		assertNotNull(context.getValue().get("issue"));
		assertNotNull(context.getValue().get("reminder"));
		assertEquals(2, ao.find(Reminder.class).length);
	}
	
	public static class ReminderServiceImplTestDatabaseUpdater implements DatabaseUpdater
    {
        @Override
        public void update(EntityManager em) throws Exception
        {
            em.migrate(Reminder.class);
            
            final Reminder reminder = em.create(Reminder.class);
    		reminder.setIssueId(ISSUE_ID);
    		reminder.setAssigneeId(ASSIGNEE_ID);
    		reminder.setReminderDate(REMINDER_DATE);
    		reminder.setComment(COMMENT);
            reminder.save();
            
            final Reminder reminder2 = em.create(Reminder.class);
            reminder2.setIssueId(ISSUE_ID);
            reminder2.setAssigneeId(ASSIGNEE_ID2);
            reminder2.setReminderDate(REMINDER_DATE2);
            reminder2.setComment(COMMENT);
            reminder2.save();
            
            final Reminder reminder3 = em.create(Reminder.class);
            reminder3.setIssueId(ISSUE_ID2);
            reminder3.setAssigneeId(ASSIGNEE_ID2);
            reminder3.setReminderDate(REMINDER_DATE);
            reminder3.setComment(COMMENT);
            reminder3.save();
        }
    }
}
