package com.protomil.core.jobcard.listeners;

import com.protomil.core.jobcard.events.JobCardAssignedEvent;
import com.protomil.core.jobcard.events.JobCardCompletedEvent;
import com.protomil.core.jobcard.events.JobCardCreatedEvent;
import com.protomil.core.jobcard.events.JobCardStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobCardEventListener {

    @EventListener
    @Async
    public void handleJobCardCreated(JobCardCreatedEvent event) {
        log.info("Job card created: {} - {}",
                event.getJobCard().getJobNumber(),
                event.getJobCard().getTitle());

        // Add notification logic here
        // sendNotificationToSupervisors(event.getJobCard());
    }

    @EventListener
    @Async
    public void handleJobCardAssigned(JobCardAssignedEvent event) {
        log.info("Job card assigned: {} assigned to user ID: {}",
                event.getJobCard().getJobNumber(),
                event.getAssignedUserId());

        // Add notification logic here
        // sendNotificationToAssignee(event.getJobCard(), event.getAssignedUserId());
    }

    @EventListener
    @Async
    public void handleJobCardStatusChanged(JobCardStatusChangedEvent event) {
        log.info("Job card status changed: {} from {} to {}",
                event.getJobCard().getJobNumber(),
                event.getPreviousStatus(),
                event.getNewStatus());

        // Add notification and business logic here
        // updateRelatedSystems(event.getJobCard(), event.getNewStatus());
    }

    @EventListener
    @Async
    public void handleJobCardCompleted(JobCardCompletedEvent event) {
        log.info("Job card completed: {} - Duration: {} minutes",
                event.getJobCard().getJobNumber(),
                event.getJobCard().getActualDurationMinutes());

        // Add completion processing logic here
        // processCompletionMetrics(event.getJobCard());
        // triggerProductionDataCapture(event.getJobCard());
    }
}