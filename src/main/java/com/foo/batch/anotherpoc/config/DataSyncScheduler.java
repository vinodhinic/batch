package com.foo.batch.anotherpoc.config;


import com.foo.batch.anotherpoc.dao.EtlJobDao;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.*;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.foo.batch.anotherpoc.config.AppConfiguration.DATA_SYNC_JOB_NAME;

@Component
@DependsOn("dataSync")
public class DataSyncScheduler {


    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private EtlJobDao etlJobDao;

    @PostConstruct
    public void cleanUpAbandonedJobs() throws JobParametersInvalidException,
            JobRestartException, JobInstanceAlreadyCompleteException, NoSuchJobExecutionException, NoSuchJobException {

        System.out.println("postconstruct invoked");
        // cleanup previous executions if any
        Set<JobExecution> jobExecutions = jobExplorer.findRunningJobExecutions(DATA_SYNC_JOB_NAME);
        System.out.println("Found running executions "+jobExecutions.size());
        for (JobExecution jobExecution : jobExecutions) {
            Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
            for (StepExecution stepExecution : stepExecutions) {
                BatchStatus status = stepExecution.getStatus();
                if (status.isRunning() || status == BatchStatus.STOPPING) {
                    stepExecution.setStatus(BatchStatus.STOPPED);
                    stepExecution.setEndTime(new Date());
                    jobRepository.update(stepExecution);
                }
            }

            jobExecution.setStatus(BatchStatus.STOPPED);
            jobExecution.setEndTime(new Date());
            jobRepository.update(jobExecution);

            Long jobExecutionId = jobExecution.getId();

            this.jobOperator.restart(jobExecutionId);
        }
    }

    @Bean
    public String getFixedDelayForDataSync() {
        return etlJobDao.getEtlConfiguration(DATA_SYNC_JOB_NAME).getSchedule();
    }

    //    https://stackoverflow.com/questions/24033208/how-to-prevent-overlapping-schedules-in-spring
    @Scheduled(fixedDelayString = "#{@getFixedDelayForDataSync}")
    public void runJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
            JobParametersInvalidException, JobRestartException, JobParametersNotFoundException, NoSuchJobException,
            JobInstanceAlreadyExistsException, InterruptedException {

        System.out.println("scheduler invoked");

        // if the instance was stopped abruptly and you reboot again, this will indefinitely wait
        // if the previous instance just started a job execution. so you might have to do a cleanup during bootstrap
        // to stop all the executions : I have added one cleanup logic here.
        // Hence @DependsOn the job itself. because you cannot restart the job before the job bean is initialized.
        while (isLastExecutionAtNonOverrideModeStillRunning(DATA_SYNC_JOB_NAME)) {
            System.out.println("Previous execution for job is still running.. waiting.");
            TimeUnit.MILLISECONDS.sleep(1000);
        }

        if(!this.etlJobDao.isActive(DATA_SYNC_JOB_NAME)) {
            return;
        }
        this.jobOperator.startNextInstance(DATA_SYNC_JOB_NAME);
    }

    public void pause() {
        this.etlJobDao.setIsActive(DATA_SYNC_JOB_NAME, false);
    }

    public void unpause() {
        this.etlJobDao.setIsActive(DATA_SYNC_JOB_NAME, true);
    }

    private JobInstance getTheLastJobInstance(String job) {
        // this returns the job instances in decreasing order of created time. this way, we are asking for the last instance triggered.
        // so what happens for retries?
        List<JobInstance> previousInstances = this.jobExplorer.findJobInstancesByJobName(job, 0, 1);
        if(previousInstances.isEmpty()) {
            return null;
        }
        JobInstance jobInstance = previousInstances.get(0);
        return jobInstance;
    }

    public boolean isLastExecutionAtNonOverrideModeStillRunning(String jobName) {
        JobInstance jobInstance = getTheLastJobInstance(jobName);
        if(jobInstance == null) {
            return false;
        }
        List<JobExecution> jobExecutions = this.jobExplorer.getJobExecutions(jobInstance);
        return jobExecutions.stream().anyMatch(e->e.isRunning());
    }

}
