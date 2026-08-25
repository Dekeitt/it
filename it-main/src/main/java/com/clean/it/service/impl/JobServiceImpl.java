package com.clean.it.service.impl;

import com.clean.it.domain.Job;
import com.clean.it.domain.JobStatus;
import com.clean.it.dto.JobDtos.CreateJobRequest;
import com.clean.it.dto.JobDtos.JobResponse;
import com.clean.it.repository.JobRepository;
import com.clean.it.service.JobService;
import com.clean.it.util.RedisLockService;
import com.clean.it.websocket.JobNotificationService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobServiceImpl implements JobService {

    private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);

    private final JobRepository jobRepository;
    private final RedisLockService lockService;
    private final JobNotificationService notificationService;

    public JobServiceImpl(JobRepository jobRepository, RedisLockService lockService, JobNotificationService notificationService) {
        this.jobRepository = jobRepository;
        this.lockService = lockService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public JobResponse createJob(Long clientId, String clientEmail, CreateJobRequest req) {
        try {
            Job job = new Job();
            job.setClientId(clientId);
            job.setClientEmail(clientEmail);
            job.setTitle(req.getTitle() != null && !req.getTitle().isBlank() ? req.getTitle() : null);
            job.setDescription(req.getDescription());
            job.setPriceCents(req.getPriceCents());
            job.setStatus(JobStatus.OPEN);
            Job saved = jobRepository.save(job);
            JobResponse resp = toDto(saved);
            try {
                notificationService.publishJobUpdate(resp);
            } catch (Exception e) {
                log.error("Failed to publish job update for job id={}", resp.getId(), e);
            }
            return resp;
        } catch (Exception ex) {
            log.error("Error creating job for clientId={}: {}", clientId, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public List<JobResponse> listOpenJobs() {
        return jobRepository.findByStatus(JobStatus.OPEN).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JobResponse acceptJob(Long cleanerId, String cleanerEmail, Long jobId) {
        String lockKey = "job:lock:" + jobId;
        String token = null;
        try {
            token = lockService.tryLock(lockKey, Duration.ofSeconds(5));
            if (token == null) {
                throw new IllegalStateException("Could not acquire lock, try again");
            }

            Job job = jobRepository.findByIdForUpdate(jobId).orElseThrow(() -> new IllegalArgumentException("Job not found"));
            if (job.getStatus() != JobStatus.OPEN) {
                throw new IllegalStateException("Job is not open");
            }
            boolean hasActive = jobRepository.existsByCleanerIdAndStatusIn(cleanerId, List.of(JobStatus.ACCEPTED, JobStatus.IN_PROGRESS));
            if (hasActive) {
                throw new IllegalStateException("Cleaner already has an active job");
            }

            job.setCleanerId(cleanerId);
            job.setCleanerEmail(cleanerEmail);
            job.setStatus(JobStatus.ACCEPTED);
            Job saved = jobRepository.save(job);
            JobResponse resp = toDto(saved);
            try {
                notificationService.publishJobUpdate(resp);
            } catch (Exception e) {
                log.error("Failed to publish job update for accepted job id={}", resp.getId(), e);
            }
            return resp;
        } catch (Exception ex) {
            log.error("Error accepting job id={} by cleanerId={}: {}", jobId, cleanerId, ex.getMessage(), ex);
            throw ex;
        } finally {
            try {
                lockService.releaseLock(lockKey, token);
            } catch (Exception e) {
                log.warn("Failed to release lock {} with token={}", lockKey, token, e);
            }
        }
    }

    private JobResponse toDto(Job job) {
        JobResponse dto = new JobResponse();
        dto.setId(job.getId());
        dto.setClientEmail(job.getClientEmail());
        dto.setCleanerEmail(job.getCleanerEmail());
        dto.setStatus(job.getStatus().name());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setPriceCents(job.getPriceCents());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());
        return dto;
    }
}
