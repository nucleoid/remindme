AJS.$(function () {
	if(AJS.$("form#reminder-admin input#updated").val() == "true"){
		JIRA.Messages.showSuccessMsg(AJS.I18n.getText("reminder.admin.success"));
	}
});