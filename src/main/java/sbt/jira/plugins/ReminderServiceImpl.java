package sbt.jira.plugins;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.java.ao.Query;
import sbt.jira.plugins.entities.Reminder;

import com.atlassian.activeobjects.external.ActiveObjects;

public class ReminderServiceImpl implements ReminderService 
{
	private final ActiveObjects ao;
	
	public ReminderServiceImpl(ActiveObjects ao){
		this.ao = ao;
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
	public List<Reminder> findByIssueId(Long issueId) {
		return new ArrayList<Reminder>(Arrays.asList(ao.find(Reminder.class, Query.select().where("issue_id = ?", issueId))));
	}

	@Override
	public int countByIssueId(Long issueId) {
		return ao.count(Reminder.class, Query.select().where("issue_id = ?", issueId));
	}
}
