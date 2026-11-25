package com.coworkhub.business;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import com.coworkhub.domain.BookingStatus;
import com.coworkhub.presentation.ApiDtos.*;

public interface BookingService {
    BookingResponse create(BookingRequest request);
    BookingResponse update(Long id, BookingUpdateRequest request);
    BookingResponse get(Long id);
    List<BookingResponse> list(Long memberId, Long roomId, LocalDate date, BookingStatus status);
    BookingResponse addService(Long id, BookingItemRequest request);
    BookingResponse removeService(Long id, Long serviceId);
    BookingResponse confirm(Long id);
    CancellationResponse cancel(Long id);
    BookingResponse complete(Long id);
    List<AvailabilityResponse> availability(Long locationId, LocalDate date, LocalTime start, LocalTime end,
                                             Integer minimumCapacity, List<Long> equipmentIds);
}
