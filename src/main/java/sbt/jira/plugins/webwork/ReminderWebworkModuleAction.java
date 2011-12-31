package sbt.jira.plugins.webwork;

import java.sql.Timestamp;

import org.ofbiz.core.entity.GenericValue;

import sbt.jira.plugins.ReminderService;
import sbt.jira.plugins.entities.Reminder;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.webresource.WebResourceManager;

public class ReminderWebworkModuleAction extends JiraWebActionSupport
{
	private final IssueService issueService;
	private final ReminderService reminderService;
    private final JiraAuthenticationContext authenticationContext;

    private Long id;
    private String assigneeId;
    private Timestamp reminderDate;
    private String comment;
    
    private final WebResourceManager webResourceManager;

    public ReminderWebworkModuleAction(IssueService issueService, JiraAuthenticationContext authenticationContext, WebResourceManager webResourceManager, ReminderService reminderService)
    {
        this.issueService = issueService;
        this.authenticationContext = authenticationContext;
        this.webResourceManager = webResourceManager;
        this.reminderService = reminderService;

    }

    protected void doValidation()
    {
    }

    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        Reminder savedReminder = reminderService.add(getIssue().getId(), getAssigneeId(), getReminderDate(), getComment());

        if (savedReminder == null || savedReminder.getID() == 0)
            return ERROR;

        return returnComplete("/browse/" + getIssue().getKey());
    }

    public String doDefault() throws Exception
    {
        final Issue issue = getIssueObject();
        if (issue == null)
        {
            return INPUT;
        }

        includeResources();

        return INPUT;
    }

    private void includeResources() {
        webResourceManager.requireResource("jira.webresources:jira-fields");
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
    
    public Timestamp getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(Timestamp reminderDate) {
        this.reminderDate = reminderDate;
    }
    
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
