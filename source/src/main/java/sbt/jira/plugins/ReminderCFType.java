package sbt.jira.plugins;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.impl.CalculatedCFType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.fields.CustomField;

public class ReminderCFType extends CalculatedCFType<Integer, Integer> implements SortableCustomField<Integer>
{
	private ReminderService reminderService;
	
	public ReminderCFType(ReminderService reminderService){
		this.reminderService = reminderService;
	}

	@Override
	public Integer getSingularObjectFromString(String size) throws FieldValidationException {
		Integer parsed = null;
		try{
			parsed = Integer.parseInt(size);
		}catch (NumberFormatException nfe) {
			parsed = 0;
		}
		return parsed;
	}

	@Override
	public String getStringFromSingularObject(Integer size) {
		return size.toString();
	}

	@Override
	public Integer getValueFromIssue(CustomField cf, Issue issue) {
		return reminderService.findByIssueId(issue.getId()).size();
	}
}
