package org.openthinclient.util;

import org.springframework.context.ApplicationEvent;

/**
 * {@link ApplicationEvent} indicating that a restart shall be executed.
 */
public class RestartApplicationEvent extends ApplicationEvent {
    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public RestartApplicationEvent(Object source) {
        super(source);
    }
}
