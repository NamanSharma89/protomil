package com.protomil.core.jobcard.events;

import com.protomil.core.jobcard.domain.JobCard;
import com.protomil.core.jobcard.domain.Machine;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class JobCardAssignedEvent extends ApplicationEvent {

    private final JobCard jobCard;
    private final UUID assignedUserId;
    private final Machine machine;

    public JobCardAssignedEvent(Object source, JobCard jobCard, UUID assignedUserId, Machine machine) {
        super(source);
        this.jobCard = jobCard;
        this.assignedUserId = assignedUserId;
        this.machine = machine;
    }
}