package com.ebv12.backend.api.dto;

import java.util.List;

public record PagedApiResponse<T>(List<T> data, int page, int size, long totalElements) {
}
