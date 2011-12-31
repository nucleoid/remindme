package it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.List;

import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import sbt.jira.plugins.ReminderServiceImpl;
import sbt.jira.plugins.entities.Reminder;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(ReminderServiceImplTest.ReminderServiceImplTestDatabaseUpdater.class)
public class ReminderServiceImplTest 
{
	private static final Long ISSUE_ID = 84l;
	private static final String ASSIGNEE_ID = "mstatz";
	private static final String ASSIGNEE_ID2 = "statzm";
	private static final Timestamp REMINDER_DATE = new Timestamp(Timestamp.UTC(2011, 12, 30, 19, 28, 5));
	private static final String COMMENT = "This is a reminder";
	
	private EntityManager entityManager;
	private ActiveObjects ao;
	private ReminderServiceImpl reminderService;
	
	@Before
	public void setUp() throws Exception {
		assertNotNull(entityManager);
		ao = new TestActiveObjects(entityManager);
		reminderService = new ReminderServiceImpl(ao);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAdd() {
		final String assigneeId = "kitty";
 
        assertEquals(2, ao.find(Reminder.class).length);
 
        final Reminder add = reminderService.add(ISSUE_ID, assigneeId, REMINDER_DATE, COMMENT);
        assertFalse(add.getID() == 0);
 
        ao.flushAll();
 
        final Reminder[] reminders = ao.find(Reminder.class);
        assertEquals(3, reminders.length);
        assertEquals(ISSUE_ID, reminders[2].getIssueId());
        assertEquals(assigneeId, reminders[2].getAssigneeId());
        assertEquals(REMINDER_DATE, reminders[2].getReminderDate());
        assertEquals(COMMENT, reminders[2].getComment());
	}

	@Test
	public void testFindByIssueId() { 
        ao.migrate(Reminder.class);
 
        assertEquals(2, ao.find(Reminder.class).length);
 
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
        ao.migrate(Reminder.class);
 
        assertEquals(2, ao.find(Reminder.class).length);

        ao.flushAll();
        
        final int reminderCount = reminderService.countByIssueId(ISSUE_ID);
        assertEquals(2, reminderCount);
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
            reminder2.setReminderDate(REMINDER_DATE);
            reminder2.setComment(COMMENT);
            reminder2.save();
        }
    }
}
