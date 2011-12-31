package sbt.jira.plugins.entities;

import java.sql.Timestamp;

import net.java.ao.Entity;
import net.java.ao.Preload;

@Preload
public interface Reminder extends Entity {
	Long getIssueId();
	
	void setIssueId(Long issueId);
	
	String getAssigneeId();
	 
    void setAssigneeId(String assigneeId);
 
    Timestamp getReminderDate();
 
    void setReminderDate(Timestamp reminderDate);
    
    String getComment();
    
    void setComment(String comment);
}
