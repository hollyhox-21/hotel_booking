package org.example.common;

import java.time.Instant;

public record ErrorDTO(
    Instant timestamp, int status, String message, String path, String traceId) {}
