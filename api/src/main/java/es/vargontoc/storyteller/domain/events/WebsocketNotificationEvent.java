package es.vargontoc.storyteller.domain.events;

import es.vargontoc.storyteller.domain.enums.NotificationType;

public record WebsocketNotificationEvent(NotificationType type, String text) {
    
    public static WebsocketNotificationEvent  info(String text) {
        return new WebsocketNotificationEvent(NotificationType.INFO, text);
    }

    public static WebsocketNotificationEvent  error(String text) {
        return new WebsocketNotificationEvent(NotificationType.ERROR, text);
    }

    public static WebsocketNotificationEvent  success(String text) {
        return new WebsocketNotificationEvent(NotificationType.SUCCESS, text);
    }
}
