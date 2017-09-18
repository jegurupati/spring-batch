package com.medinvention.controller;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medinvention.controller.exception.BadRequestException;
import com.medinvention.controller.exception.NotFoundException;
import com.medinvention.job.report.ExecutionReport;

@RestController
public class PersonController {

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    JobExplorer jobExplorer;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    @Qualifier("importUserJob")
    Job importJob;

    @Autowired
    @Qualifier("exportUserJob")
    Job exportJob;

    @RequestMapping(path = "/job/status", method = RequestMethod.GET)
    public ExecutionReport jobStatus(@RequestParam(value = "id") Long id) throws Exception {

        JobExecution jobExecution = jobExplorer.getJobExecution(id);

        if (null == jobExecution) {
            throw new NotFoundException(String.format("Job with ID [%s] not found !", id));
        }

        return ExecutionReport.createFromJobExecution(jobExecution);
    }

    @RequestMapping(value = { "/person/import", "person/async/import" }, method = RequestMethod.GET)
    public ExecutionReport runImport(HttpServletRequest request, @RequestParam String filename) throws Exception {
        Boolean isAsync = request.getRequestURI().equals("/person/async/import");

        if (isAsync) {
            filename = "sample-data-big.csv";
        }
        else {
            filename = "sample-data.csv";
        }

        File file = new File(filename);

        if (!file.exists() || file.isDirectory()) {
            throw new BadRequestException(String.format("File with name [%s] not found !", filename));
        }

        if (filename.length() < 5 || 0 != filename.substring(filename.length() - 4).toLowerCase().compareTo(".csv")) {
            throw new BadRequestException(String.format("File with name [%s] not validate (not csv) !", filename));
        }

        JobParameters parameters = new JobParametersBuilder().addLong("timestamp", System.currentTimeMillis())
                .addString("filename", filename).toJobParameters();

        JobExecution jobResult;

        if (isAsync) {
            SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
            simpleJobLauncher.setJobRepository(jobRepository);

            SimpleAsyncTaskExecutor simpleAsyncTaskExecutor = new SimpleAsyncTaskExecutor();
            simpleJobLauncher.setTaskExecutor(simpleAsyncTaskExecutor);

            jobResult = simpleJobLauncher.run(importJob, parameters);
        }
        else {
            jobResult = jobLauncher.run(importJob, parameters);
        }

        return ExecutionReport.createFromJobExecution(jobResult);
    }

    @RequestMapping(value = { "/person/export", "/person/async/export" }, method = RequestMethod.GET)
    public ExecutionReport runExport(HttpServletRequest request, @RequestParam String filename) throws Exception {

        Boolean isAsync = request.getRequestURI().equals("/person/async/export");

        filename = "sample-data.json";

        if (filename.length() < 6 || 0 != filename.substring(filename.length() - 5).toLowerCase().compareTo(".json")) {
            throw new BadRequestException(String.format("File with name [%s] not validate (not json) !", filename));
        }

        JobParameters parameters = new JobParametersBuilder().addLong("timestamp", System.currentTimeMillis())
                .addString("filename", "sample-data.json").toJobParameters();

        JobExecution jobResult;

        if (isAsync) {
            SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
            simpleJobLauncher.setJobRepository(jobRepository);

            SimpleAsyncTaskExecutor simpleAsyncTaskExecutor = new SimpleAsyncTaskExecutor();
            simpleJobLauncher.setTaskExecutor(simpleAsyncTaskExecutor);

            jobResult = simpleJobLauncher.run(exportJob, parameters);
        }
        else {
            jobResult = jobLauncher.run(exportJob, parameters);
        }

        return ExecutionReport.createFromJobExecution(jobResult);
    }
}
