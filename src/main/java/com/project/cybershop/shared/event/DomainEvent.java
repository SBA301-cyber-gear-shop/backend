package com.project.cybershop.shared.event;

/**
 * Marker interface cho mọi domain event trong hệ thống.
 * Các module phát event bằng ApplicationEventPublisher.
 * Các module nhận event bằng @EventListener hoặc @TransactionalEventListener.
 */
public interface DomainEvent {
}
