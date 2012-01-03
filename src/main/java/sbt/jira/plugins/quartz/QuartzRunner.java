package sbt.jira.plugins.quartz;

import javax.annotation.PreDestroy;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;

import com.atlassian.jira.scheduler.JiraSchedulerFactory;
import com.atlassian.sal.api.lifecycle.LifecycleAware;

public class QuartzRunner implements LifecycleAware {
	
	private static final String REMINDER_JOB_NAME = "sbt.jira.plugins.quartz.ReminderJob Job";
	private static final String REMINDER_TRIGGER_NAME = "sbt.jira.plugins.quartz.ReminderJob Trigger";
	
	private JiraSchedulerFactory schedulerFactory;

	public QuartzRunner(JiraSchedulerFactory schedulerFactory) {
		this.schedulerFactory = schedulerFactory;
	}
	
	@Override
	public void onStart() {		
		JobDetail jobDetail = new JobDetail(REMINDER_JOB_NAME, null, ReminderJob.class);
//		Trigger trigger = TriggerUtils.makeDailyTrigger(8, 0);
		Trigger trigger = TriggerUtils.makeMinutelyTrigger(3);
		trigger.setName(REMINDER_TRIGGER_NAME);
		
		try {
			Scheduler scheduler = schedulerFactory.getScheduler();
			scheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}
	
	@PreDestroy
	public void Destory(){
		try {
			schedulerFactory.getScheduler().unscheduleJob(REMINDER_TRIGGER_NAME, null);
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}

}
