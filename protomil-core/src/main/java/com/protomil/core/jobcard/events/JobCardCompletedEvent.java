package com.protomil.core.jobcard.events;

import com.protomil.core.jobcard.domain.JobCard;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class JobCardCompletedEvent extends ApplicationEvent {

    private final JobCard jobCard;

    public JobCardCompletedEvent(Object source, JobCard jobCard) {
        super(source);
        this.jobCard = jobCard;
    }
}