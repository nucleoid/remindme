AJS.$(function () {
    var getIssueKey = function(){
        if (JIRA.IssueNavigator.isNavigator()){
            return JIRA.IssueNavigator.getSelectedIssueKey();
        } else {
            return AJS.$.trim(AJS.$("#key-val").text());
        }
    };
    var updateReminderCount = function(startCount, dialog){
    	var $remindersContainer = AJS.$("#issuerow" + JIRA.IssueNavigator.getSelectedIssueId() + " td p.reminders, #reminders-val" );
        $remindersContainer.html("");
        var reminderCount = startCount;
        var existingReminderRows;
        try{
        	existingReminderRows = dialog.getContentArea().find("table#existing_reminders tr");
        } catch(err) {
        	existingReminderRows = null;
        }
        if(existingReminderRows){
        	existingReminderRows.each(function(){
        		if(AJS.$(this).attr("id"))
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
        	updateReminderCount(1, this);
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
	            var existingReminderRows = JIRA.Dialogs.remindIssue.getContentArea().find("table#existing_reminders tr");
	            if(existingReminderRows){
	            	var hasRows = false;
	            	existingReminderRows.each(function(){
	            		if(AJS.$(this).attr("id"))
	            			hasRows = true;
	                });
	            	if(!hasRows)
	            		AJS.$("table#existing_reminders").replaceWith("There are currently no reminders <br />associated with this issue.")
	            }
	            updateReminderCount(0, JIRA.Dialogs.remindIssue);
		});
    	}
    });
});
