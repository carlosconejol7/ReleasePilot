package com.releasepilot.infrastructure.api.exception;

import java.time.Instant;

/**
 * Standardized error response body returned to API clients when a request fails.
 *
 * @param message   a human-readable description of the error
 * @param status    the HTTP status code returned
 * @param timestamp the instant at which the error occurred
 */
public record ErrorResponse(String message, int status, Instant timestamp) {
}
