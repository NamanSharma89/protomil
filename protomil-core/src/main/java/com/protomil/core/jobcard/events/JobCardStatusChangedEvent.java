package com.protomil.core.jobcard.events;

import com.protomil.core.jobcard.domain.JobCard;
import com.protomil.core.jobcard.domain.enums.JobStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class JobCardStatusChangedEvent extends ApplicationEvent {

    private final JobCard jobCard;
    private final JobStatus previousStatus;
    private final JobStatus newStatus;

    public JobCardStatusChangedEvent(Object source, JobCard jobCard, JobStatus previousStatus, JobStatus newStatus) {
        super(source);
        this.jobCard = jobCard;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }
}