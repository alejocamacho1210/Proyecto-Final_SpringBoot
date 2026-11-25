package com.coworkhub.presentation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.coworkhub.business.BookingService;
import com.coworkhub.presentation.ApiDtos.AvailabilityResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/disponibilidad")
@SecurityRequirement(name = "bearerAuth")
public class AvailabilityController {
    private final BookingService bookings;
    public AvailabilityController(BookingService bookings) { this.bookings = bookings; }

    @GetMapping
    public List<AvailabilityResponse> availability(
        @RequestParam Long sedeId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
        @RequestParam(required = false) Integer capacidadMinima,
        @RequestParam(required = false) List<Long> equipamientoIds) {
        return bookings.availability(sedeId, fecha, horaInicio, horaFin, capacidadMinima, equipamientoIds);
    }
}
