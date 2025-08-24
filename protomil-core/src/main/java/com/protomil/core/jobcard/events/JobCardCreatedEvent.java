package com.protomil.core.jobcard.events;

import com.protomil.core.jobcard.domain.JobCard;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class JobCardCreatedEvent extends ApplicationEvent {

    private final JobCard jobCard;

    public JobCardCreatedEvent(Object source, JobCard jobCard) {
        super(source);
        this.jobCard = jobCard;
    }
}