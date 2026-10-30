package org.azdev.barber_book.dtos;

import java.util.List;

public record ClientFrequencyReportResponse(
        String month,
        int page,
        int size,
        long totalClients,
        List<ClientFrequencyItemResponse> clients
) {
}
