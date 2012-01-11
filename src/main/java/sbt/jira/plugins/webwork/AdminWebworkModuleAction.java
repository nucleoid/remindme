package sbt.jira.plugins.webwork;

import sbt.jira.plugins.ReminderMonitor;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class AdminWebworkModuleAction  extends JiraWebActionSupport {

	private final WebResourceManager webResourceManager;
	private final PluginSettingsFactory pluginSettingsFactory;
	private final ReminderMonitor reminderMonitor;
	
	private int interval;
	private boolean updated;
	
	public AdminWebworkModuleAction(WebResourceManager webResourceManager, PluginSettingsFactory pluginSettingsFactory, ReminderMonitor reminderMonitor){
		this.webResourceManager = webResourceManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.reminderMonitor = reminderMonitor;
	}
	
	@Override
	protected void doValidation()
    {
		if (interval == 0){
			addError("interval", "Please enter a valid whole number greater than zero");
			setUpdated(false);
		}
		super.doValidation();
    }
	
	@Override
	@RequiresXsrfCheck
	public String doExecute() throws Exception
	{
		PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        pluginSettings.put(AdminWebworkModuleAction.class.getName()  +".interval", Integer.toString(getInterval()));
        setUpdated(true);
        reminderMonitor.reschedule(getInterval(), true);
    	return returnComplete();
	}
	
	@Override
	public String doDefault() throws Exception
    {
		if(!isSystemAdministrator())
			return LOGIN;

        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        String interval = (String) settings.get(AdminWebworkModuleAction.class.getName() + ".interval");
        if (interval != null)
            setInterval(Integer.parseInt(interval));
        setUpdated(false);
		return INPUT;
    }
	
	public String getBaseUrl(){
		return getApplicationProperties().getString(APKeys.JIRA_BASEURL);
	}
	
	public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
    
    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
}
