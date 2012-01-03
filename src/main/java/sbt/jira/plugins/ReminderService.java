package sbt.jira.plugins;

import java.sql.Timestamp;
import java.util.List;

import sbt.jira.plugins.entities.Reminder;

import com.atlassian.activeobjects.tx.Transactional;

@Transactional
public interface ReminderService {
	Reminder add(Long issueId, String assigneeId, Timestamp reminderDate, String comment);
	List<Reminder> findByIssueId(Long issueId);
	int countByIssueId(Long issueId);
	List<Reminder> findNeededReminders(Timestamp today);
	void deleteReminders(List<Reminder> reminders);
	void sendReminderNotifications(List<Reminder> reminders);
}
