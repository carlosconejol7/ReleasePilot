package com.releasepilot.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables Spring's asynchronous method execution support, required for {@code @Async}-annotated
 * event listeners (such as {@code com.releasepilot.infrastructure.audit.AuditLogConsumer}) to
 * actually run on a separate thread rather than synchronously on the publishing thread.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
