package com.coworkhub.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import com.coworkhub.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class ApiDtos {
    private ApiDtos() {}

    public record ErrorResponse(String error, String codigo, Instant timestamp) {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, String tipo, long venceEnMinutos) {}
    public record ProfileResponse(String usuario, Role rol, Long miembroId) {}

    public record LocationRequest(@NotBlank String name, @NotBlank String city, @NotBlank String address,
                                  @NotNull LocalTime openingTime, @NotNull LocalTime closingTime) {}
    public record LocationResponse(Long id, String name, String city, String address, LocalTime openingTime, LocalTime closingTime) {}
    public record EquipmentRequest(@NotBlank String name) {}
    public record EquipmentResponse(Long id, String name) {}
    public record ServiceRequest(@NotBlank String name, @NotNull @DecimalMin("0.00") BigDecimal unitPrice) {}
    public record ServiceResponse(Long id, String name, BigDecimal unitPrice) {}
    public record PlanRequest(@NotBlank String name, @Min(0) int includedHoursPerMonth,
                              @Min(0) @Max(100) int overageDiscount, @Min(1) int maxActiveBookings) {}
    public record PlanResponse(Long id, String name, int includedHoursPerMonth, int overageDiscount, int maxActiveBookings) {}
    public record RoomRequest(@NotBlank String name, @NotNull RoomType type, @Min(1) int capacity,
                              @NotNull @DecimalMin("0.00") BigDecimal hourlyRate, @NotNull Boolean active,
                              @NotNull Long locationId, List<Long> equipmentIds) {}
    public record RoomResponse(Long id, String name, RoomType type, int capacity, BigDecimal hourlyRate,
                               boolean active, Long locationId, String locationName, List<EquipmentResponse> equipment) {}
    public record MemberRequest(@NotBlank String documentNumber, @NotBlank String fullName, @Email @NotBlank String email,
                                @NotNull Long planId, @NotBlank String username, @NotBlank @Size(min = 8) String password) {}
    public record MemberUpdateRequest(@NotBlank String fullName, @Email @NotBlank String email, @NotNull Long planId) {}
    public record MemberResponse(Long id, String documentNumber, String fullName, String email,
                                 MemberStatus status, Long planId, String planName, String username) {}
    public record ConsumptionResponse(Long miembroId, String mes, int horasIncluidas, BigDecimal horasUsadas,
                                      BigDecimal horasRestantes, BigDecimal horasExcedentes) {}
    public record BookingItemRequest(@NotNull Long servicioId, @Min(1) int cantidad) {}
    public record BookingRequest(@NotNull Long salaId, @NotNull Long miembroId, @NotNull LocalDateTime inicio,
                                 @NotNull LocalDateTime fin, @Min(1) int asistentes, List<@Valid BookingItemRequest> servicios) {}
    public record BookingUpdateRequest(@NotNull Long salaId, @NotNull LocalDateTime inicio,
                                       @NotNull LocalDateTime fin, @Min(1) int asistentes) {}
    public record BookingResponse(Long id, LocalDateTime startTime, LocalDateTime endTime, int attendees,
                                  BookingStatus status, BigDecimal roomCost, BigDecimal servicesCost,
                                  BigDecimal totalCost, BigDecimal cancellationFee, Long roomId, String roomName,
                                  Long memberId, String memberName, List<BookingItemResponse> items) {}
    public record BookingItemResponse(Long serviceId, String serviceName, int quantity, BigDecimal appliedUnitPrice) {}
    public record CancellationResponse(Long bookingId, BookingStatus status, BigDecimal cancellationFee) {}
    public record AvailabilityResponse(Long id, String name, RoomType type, int capacity, BigDecimal hourlyRate,
                                       Long locationId, List<EquipmentResponse> equipment) {}
}
