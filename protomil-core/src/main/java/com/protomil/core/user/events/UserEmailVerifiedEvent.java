package com.protomil.core.user.events;

import com.protomil.core.user.domain.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserEmailVerifiedEvent extends ApplicationEvent {

    private final User user;

    public UserEmailVerifiedEvent(User user) {
        super(user);
        this.user = user;
    }
}