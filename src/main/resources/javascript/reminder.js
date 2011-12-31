AJS.$(function () {
    // Function for getting the issue key of the issue being edited.
    var getIssueKey = function(){
        if (JIRA.IssueNavigator.isNavigator()){
            return JIRA.IssueNavigator.getSelectedIssueKey();
        } else {
            return AJS.$.trim(AJS.$("#key-val").text());
        }
    };
 
    JIRA.Dialogs.scheduleIssue = new JIRA.FormDialog({
        id: "reminder-dialog",
        trigger: "a.issueaction-remind-issue",
        ajaxOptions: JIRA.Dialogs.getDefaultAjaxOptions,
        onSuccessfulSubmit : function(){ 
        },
        onDialogFinished : function(){ 
            if (JIRA.IssueNavigator.isNavigator()){
                JIRA.Messages.showSuccessMsg(AJS.I18n.getText("reminder.success.issue", getIssueKey()));
            } else {
                JIRA.Messages.showSuccessMsg(AJS.I18n.getText("reminder.success"));
            }
        },
        autoClose : true // This tells the dialog to automatically close after a successful form submit.
    });
});