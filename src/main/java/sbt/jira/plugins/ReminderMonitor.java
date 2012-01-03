package sbt.jira.plugins;

public interface ReminderMonitor {
	public void reschedule(long interval);
}
