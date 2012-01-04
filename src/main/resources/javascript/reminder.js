AJS.$(function () {
    var getIssueKey = function(){
        if (JIRA.IssueNavigator.isNavigator()){
            return JIRA.IssueNavigator.getSelectedIssueKey();
        } else {
            return AJS.$.trim(AJS.$("#key-val").text());
        }
    };
    var updateReminderCount = function(startCount){
    	var $remindersContainer = AJS.$("#issuerow" + JIRA.IssueNavigator.getSelectedIssueId() + " td p.reminders, #reminders-val" );
        $remindersContainer.html("");
        var reminderCount = startCount;
        var existingReminderRows = this.getContentArea().find("table#existing_reminders tr");
        if(existingReminderRows){
        	existingReminderRows.each(function(){
            	reminderCount++;
            });
        }
        $remindersContainer.html(reminderCount);
    };
    JIRA.Dialogs.remindIssue = new JIRA.FormDialog({
        id: "reminder-dialog",
        trigger: "a.issueaction-remind-issue",
        ajaxOptions: JIRA.Dialogs.getDefaultAjaxOptions,
        width: 625,
        onSuccessfulSubmit : function(){ 
        	updateReminderCount(1);
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
    AJS.$(document).delegate("a.remindaction-delete-reminder", "click", function(event){
    	event.preventDefault();
    	if(confirm('Are you sure you want to delete this reminder?')){
    		var url = AJS.$(event.target).attr('href');
    		var reminderIdToDelete = AJS.$(event.target).attr('id').replace("existing_reminder_link_", "");
    		AJS.$.post(url, {reminderId: reminderIdToDelete}, function() {
    			var $reminderTr = AJS.$("tr#existing_reminder_" +reminderIdToDelete);
	            $reminderTr.remove();
	            updateReminderCount(0);
			});
    	}
    });
});
