package com.clean.it.service;

import com.clean.it.dto.JobDtos.CreateJobRequest;
import com.clean.it.dto.JobDtos.JobResponse;

import java.util.List;

public interface JobService {
    JobResponse createJob(String clientEmail, CreateJobRequest req);

    List<JobResponse> listOpenJobs();

    JobResponse acceptJob(String cleanerEmail, Long jobId);
}

