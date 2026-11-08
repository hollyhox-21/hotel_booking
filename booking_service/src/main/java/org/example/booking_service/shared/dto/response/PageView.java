package org.example.booking_service.shared.dto.response;

import java.util.List;

public record PageView<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {}
