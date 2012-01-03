AJS.$(function () {
    var getIssueKey = function(){
        if (JIRA.IssueNavigator.isNavigator()){
            return JIRA.IssueNavigator.getSelectedIssueKey();
        } else {
            return AJS.$.trim(AJS.$("#key-val").text());
        }
    };
    JIRA.Dialogs.remindIssue = new JIRA.FormDialog({
        id: "reminder-dialog",
        trigger: "a.issueaction-remind-issue",
        ajaxOptions: JIRA.Dialogs.getDefaultAjaxOptions,
        width: 625,
        onSuccessfulSubmit : function(){ 
            var $remindersContainer = AJS.$("#issuerow" + JIRA.IssueNavigator.getSelectedIssueId() + " td p.reminders, #reminders-val" );
            $remindersContainer.html("");
            var reminderCount = 1;
            this.getContentArea().find("table#existing_reminders tr").each(function(){
            	reminderCount++;
            });
            $remindersContainer.html(reminderCount);
        },
        onDialogFinished : function(){ 
            if (JIRA.IssueNavigator.isNavigator()){
                JIRA.Messages.showSuccessMsg(AJS.I18n.getText("reminder.success.issue", getIssueKey()));
            } else {
                JIRA.Messages.showSuccessMsg(AJS.I18n.getText("reminder.success"));
            }
        },
        autoClose : true
    });
});