// com/protomil/core/user/events/UserRoleAssignedEvent.java
package com.protomil.core.user.events;

import com.protomil.core.user.domain.Role;
import com.protomil.core.user.domain.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class UserRoleAssignedEvent extends ApplicationEvent {
    private final User user;
    private final Role role;
    private final UUID assignedBy;

    public UserRoleAssignedEvent(User user, Role role, UUID assignedBy) {
        super(user);
        this.user = user;
        this.role = role;
        this.assignedBy = assignedBy;
    }
}