package sbt.jira.plugins.webwork;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.ofbiz.core.entity.GenericValue;

import sbt.jira.plugins.ReminderService;
import sbt.jira.plugins.entities.Reminder;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.webresource.WebResourceManager;

public class ReminderWebworkModuleAction extends JiraWebActionSupport
{	
	private final IssueService issueService;
	private final ReminderService reminderService;
    private final JiraAuthenticationContext authenticationContext;
    private final WebResourceManager webResourceManager;
    private UserManager userManager;
    
    private Long id;
    private String assigneeId;
    private String reminderDate;
    private String comment;
    private int redminderId;
    
    private List<Reminder> currentReminders;
    private static final String DATE_FORMAT = "MM/dd/yyyy";
    private SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

    public ReminderWebworkModuleAction(IssueService issueService, JiraAuthenticationContext authenticationContext, WebResourceManager webResourceManager, ReminderService reminderService, UserManager userManager)
    {
        this.issueService = issueService;
        this.authenticationContext = authenticationContext;
        this.webResourceManager = webResourceManager;
        this.reminderService = reminderService;
        this.userManager = userManager;
    }

    @Override
    protected void doValidation()
    {
    	if (assigneeId == null || assigneeId.length() == 0) 
    		addError("assigneeId", "Please enter username.");
        if (reminderDate == null || reminderDate.length() < 1) 
        	addError("reminderDate", "Please enter a date.");
        if (assigneeId != null && assigneeId.length() > 0 && userManager.getUserObject(assigneeId) == null) 
    		addError("assigneeId", "Invalid username.");
        if (reminderDate != null && reminderDate.length() > 0 && !dateParses(reminderDate)) 
        	addError("reminderDate", String.format("Invalid date.  (%1s)", DATE_FORMAT));
        super.doValidation();
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
    	java.util.Date date;
		try {
			date = sdf.parse(reminderDate);
		} catch (ParseException e) {
			date = Calendar.getInstance().getTime();
		}
        Reminder savedReminder = reminderService.add(getIssue().getId(), getAssigneeId(), new Timestamp(date.getTime()), getComment());

        if (savedReminder == null || savedReminder.getID() == 0){
        	addErrorMessage("Something went wrong! Reminder not saved.");
        	return ERROR;
        }
            
        
    	return returnComplete("/browse/" + getIssue().getKey());
    }

    @Override
    public String doDefault() throws Exception
    {
    	if(redminderId > 0){
    		Reminder reminderToDelete = reminderService.findById(redminderId);
    		reminderService.deleteReminder(reminderToDelete);
    	}
    	else{
    		final Issue issue = getIssueObject();
            if (issue == null)
                return INPUT;
    	}
        includeResources();
        return INPUT;
    }
    
    private boolean dateParses(String date){
    	try {
			sdf.parse(date);
		} catch (ParseException e) {
			return false;
		}
		return true;
    }

    private void includeResources() {
        webResourceManager.requireResource("jira.webresources:jira-fields");
        webResourceManager.requireResource("jira.webresources:autocomplete");
        webResourceManager.requireResource("jira.webresources:calendar");
        webResourceManager.requireResource("jira.webresources:calendar-en");
    }

    public GenericValue getProject()
    {
        return getIssue().getProject();
    }

    public Issue getIssue()
    {
        return getIssueObject();
    }

    public Issue getIssueObject()
    {
        final IssueService.IssueResult issueResult = issueService.getIssue(authenticationContext.getLoggedInUser(), id);
        if (!issueResult.isValid())
        {
            this.addErrorCollection(issueResult.getErrorCollection());
            return null;
        }
        return  issueResult.getIssue();
    }

    public int getReminderId() {
        return redminderId;
    }

    public void setReminderId(int redminderId) {
        this.redminderId = redminderId;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }
    
    public String getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(String reminderDate) {
    	this.reminderDate = reminderDate;
    }
    
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    } 
    
    public List<Reminder> getCurrentReminders(){
    	if(currentReminders == null)
    		currentReminders = reminderService.findByIssueId(getIssue().getId());
    	return currentReminders;
    }
    
    public boolean isDisplayCurrentReminders() {
    	return getCurrentReminders().size() > 0;
    }
    
    public String formatTimestamp(Timestamp date){
    	if(date != null)
    		return new SimpleDateFormat("MM/dd/yyyy").format(date);
    	return "N/A";
    }
    
    public String fullNameFromUsername(String username){
    	User user = userManager.getUserObject(username);
    	if(user != null)
    		return user.getDisplayName();
    	return username;
    }
}
