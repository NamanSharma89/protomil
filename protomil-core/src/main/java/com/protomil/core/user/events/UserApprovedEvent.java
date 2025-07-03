// com/protomil/core/user/events/UserApprovedEvent.java
package com.protomil.core.user.events;

import com.protomil.core.user.domain.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class UserApprovedEvent extends ApplicationEvent {
    private final User user;
    private final UUID approvedBy;

    public UserApprovedEvent(User user, UUID approvedBy) {
        super(user);
        this.user = user;
        this.approvedBy = approvedBy;
    }
}