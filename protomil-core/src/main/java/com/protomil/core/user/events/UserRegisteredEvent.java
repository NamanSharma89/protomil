// com/protomil/core/user/events/UserRegisteredEvent.java
package com.protomil.core.user.events;

import com.protomil.core.user.domain.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserRegisteredEvent extends ApplicationEvent {
    private final User user;

    public UserRegisteredEvent(User user) {
        super(user);
        this.user = user;
    }
}