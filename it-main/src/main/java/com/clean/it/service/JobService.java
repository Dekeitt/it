package com.clean.it.service;

import com.clean.it.dto.JobDtos.CreateJobRequest;
import com.clean.it.dto.JobDtos.JobResponse;

import java.util.List;

public interface JobService {
    JobResponse createJob(Long clientId, String clientEmail, CreateJobRequest req);

    List<JobResponse> listOpenJobs();

    JobResponse acceptJob(Long cleanerId, String cleanerEmail, Long jobId);
}
