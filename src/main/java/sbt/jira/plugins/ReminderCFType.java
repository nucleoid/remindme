package sbt.jira.plugins;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.impl.CalculatedCFType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.fields.CustomField;

public class ReminderCFType extends CalculatedCFType implements SortableCustomField 
{
	private ReminderService reminderService;
	
	public ReminderCFType(ReminderService reminderService){
		this.reminderService = reminderService;
	}
	
	@Override
	public Object getSingularObjectFromString(String arg0) throws FieldValidationException {
		return -2;
	}

	@Override
	public String getStringFromSingularObject(Object arg0) {
		return "-3";
	}

	@Override
	public Object getValueFromIssue(CustomField cf, Issue issue) {
		return reminderService.findByIssueId(issue.getId()).size();
	}
}
