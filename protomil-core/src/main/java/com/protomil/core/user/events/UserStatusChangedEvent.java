// src/main/java/com/protomil/core/user/events/UserStatusChangedEvent.java
package com.protomil.core.user.events;

import com.protomil.core.shared.domain.enums.UserStatus;
import com.protomil.core.user.domain.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class UserStatusChangedEvent extends ApplicationEvent {

    private final User user;
    private final UserStatus previousStatus;
    private final UserStatus newStatus;
    private final UUID changedBy;
    private final String reason;

    public UserStatusChangedEvent(User user, UserStatus previousStatus, UserStatus newStatus,
                                  UUID changedBy, String reason) {
        super(user);
        this.user = user;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.reason = reason;
    }
}