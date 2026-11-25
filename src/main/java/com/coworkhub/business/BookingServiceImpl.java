package com.coworkhub.business;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.coworkhub.domain.*;
import com.coworkhub.persistence.*;
import com.coworkhub.presentation.ApiDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final MemberRepository members;
    private final AdditionalServiceRepository services;
    private final EquipmentRepository equipment;
    private final Clock clock;

    public BookingServiceImpl(BookingRepository bookings, RoomRepository rooms, MemberRepository members,
                              AdditionalServiceRepository services, EquipmentRepository equipment, Clock clock) {
        this.bookings = bookings; this.rooms = rooms; this.members = members; this.services = services;
        this.equipment = equipment; this.clock = clock;
    }

    @Override
    public BookingResponse create(BookingRequest request) {
        Room room = rooms.findByIdForUpdate(request.salaId()).orElseThrow(() -> Errors.notFound("Sala"));
        Member member = members.findByIdForUpdate(request.miembroId()).orElseThrow(() -> Errors.notFound("Miembro"));
        validateBooking(room, member, request.inicio(), request.fin(), request.asistentes());
        LocalDateTime now = LocalDateTime.now(clock);
        if (bookings.countActiveFutureByMember(member.getId(), now) >= member.getMembershipPlan().getMaxActiveBookings())
            throw Errors.conflict("El miembro alcanzó el máximo de reservas activas de su plan", "RN-06");
        if (!bookings.findOverlapping(room.getId(), request.inicio(), request.fin()).isEmpty())
            throw Errors.conflict("La sala ya está reservada en ese horario", "RN-01");

        Booking booking = new Booking();
        booking.setRoom(room); booking.setMember(member); booking.setStartTime(request.inicio()); booking.setEndTime(request.fin());
        booking.setAttendees(request.asistentes()); booking.setStatus(BookingStatus.PENDING);
        appendRequestedServices(booking, request.servicios());
        calculateServicesCost(booking);
        booking = bookings.saveAndFlush(booking);
        recalculateMonthlyRoomCosts(member, YearMonth.from(request.inicio()));
        return map(bookings.findDetailedById(booking.getId()).orElseThrow());
    }

    @Override
    public BookingResponse update(Long id, BookingUpdateRequest request) {
        Booking booking = findBooking(id);
        requirePending(booking);
        Room room = rooms.findByIdForUpdate(request.salaId()).orElseThrow(() -> Errors.notFound("Sala"));
        Member member = members.findByIdForUpdate(booking.getMember().getId())
            .orElseThrow(() -> Errors.notFound("Miembro"));
        validateBooking(room, member, request.inicio(), request.fin(), request.asistentes());
        boolean overlapsAnotherBooking = bookings.findOverlapping(room.getId(), request.inicio(), request.fin())
            .stream().anyMatch(overlap -> !overlap.getId().equals(id));
        if (overlapsAnotherBooking) throw Errors.conflict("La sala ya está reservada en ese horario", "RN-01");

        YearMonth previousMonth = YearMonth.from(booking.getStartTime());
        YearMonth updatedMonth = YearMonth.from(request.inicio());
        booking.setRoom(room);
        booking.setStartTime(request.inicio());
        booking.setEndTime(request.fin());
        booking.setAttendees(request.asistentes());
        bookings.saveAndFlush(booking);

        recalculateMonthlyRoomCosts(member, previousMonth);
        if (!updatedMonth.equals(previousMonth)) recalculateMonthlyRoomCosts(member, updatedMonth);
        return map(bookings.findDetailedById(id).orElseThrow());
    }

    private void validateBooking(Room room, Member member, LocalDateTime start, LocalDateTime end, int attendees) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (member.getStatus() != MemberStatus.ACTIVE) throw Errors.conflict("El miembro está suspendido", "RN-05");
        if (!room.isActive()) throw Errors.conflict("La sala no está activa", "RN-04");
        if (attendees > room.getCapacity()) throw Errors.conflict("Los asistentes superan la capacidad de la sala", "RN-04");
        if (!start.isBefore(end) || !start.toLocalDate().equals(end.toLocalDate()))
            throw Errors.conflict("La reserva debe empezar y terminar el mismo día", "RN-02");
        Duration duration = Duration.between(start, end);
        long durationMillis = duration.toMillis();
        if (durationMillis < 30 * 60 * 1000L || durationMillis > 8 * 60 * 60 * 1000L
                || durationMillis % (30 * 60 * 1000L) != 0)
            throw Errors.conflict("La duración debe ser de 30 minutos a 8 horas, en múltiplos de 30 minutos", "RN-02");
        if (!start.isAfter(now) || start.isBefore(now.plusHours(1)))
            throw Errors.conflict("La reserva debe ser futura y solicitarse con al menos 1 hora de anticipación", "RN-03");
        LocalTime opening = room.getLocation().getOpeningTime();
        LocalTime closing = room.getLocation().getClosingTime();
        if (start.toLocalTime().isBefore(opening) || end.toLocalTime().isAfter(closing))
            throw Errors.conflict("La reserva debe estar dentro del horario de la sede", "RN-03");
    }

    @Override @Transactional(readOnly = true)
    public BookingResponse get(Long id) {
        return map(bookings.findDetailedById(id).orElseThrow(() -> Errors.notFound("Reserva")));
    }

    @Override @Transactional(readOnly = true)
    public List<BookingResponse> list(Long memberId, Long roomId, LocalDate date, BookingStatus status) {
        List<Booking> result;
        if (memberId != null) result = bookings.findAllByMemberId(memberId);
        else if (roomId != null && date != null) result = bookings.findAllByRoomIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
            roomId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        else if (status != null) result = bookings.findAllByStatus(status);
        else result = bookings.findAll();
        return result.stream().map(this::map).toList();
    }

    @Override
    public BookingResponse addService(Long id, BookingItemRequest request) {
        Booking booking = findBooking(id);
        requirePending(booking);
        AdditionalService service = services.findById(request.servicioId()).orElseThrow(() -> Errors.notFound("Servicio adicional"));
        BookingItem existing = booking.getItems().stream()
            .filter(item -> item.getAdditionalService().getId().equals(service.getId())).findFirst().orElse(null);
        if (existing == null) {
            BookingItem item = new BookingItem();
            item.setBooking(booking); item.setAdditionalService(service); item.setQuantity(request.cantidad());
            item.setAppliedUnitPrice(service.getUnitPrice()); booking.getItems().add(item);
        } else {
            if (existing.getQuantity() > Integer.MAX_VALUE - request.cantidad())
                throw Errors.invalid("La cantidad total del servicio excede el máximo permitido", "VALIDATION_ERROR");
            existing.setQuantity(existing.getQuantity() + request.cantidad());
        }
        calculateServicesCost(booking);
        return map(bookings.saveAndFlush(booking));
    }

    @Override
    public BookingResponse removeService(Long id, Long serviceId) {
        Booking booking = findBooking(id);
        requirePending(booking);
        boolean removed = booking.getItems().removeIf(item -> {
            if (item.getAdditionalService().getId().equals(serviceId)) { item.setBooking(null); return true; }
            return false;
        });
        if (!removed) throw Errors.notFound("Servicio de la reserva");
        calculateServicesCost(booking);
        return map(bookings.saveAndFlush(booking));
    }

    @Override
    public BookingResponse confirm(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.PENDING) throw Errors.conflict("Solo se puede confirmar una reserva pendiente", "RN-09");
        booking.setStatus(BookingStatus.CONFIRMED);
        return map(booking);
    }

    @Override
    public CancellationResponse cancel(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED)
            throw Errors.conflict("La reserva no se puede cancelar desde su estado actual", "RN-09");
        LocalDateTime now = LocalDateTime.now(clock);
        long minutesUntilStart = Duration.between(now, booking.getStartTime()).toMinutes();
        if (minutesUntilStart < 120) throw Errors.conflict("No se puede cancelar con menos de 2 horas de anticipación o una vez iniciada", "RN-08");
        BigDecimal fee = minutesUntilStart >= 24 * 60 ? BigDecimal.ZERO : booking.getRoomCost().multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
        booking.setStatus(BookingStatus.CANCELLED); booking.setCancellationFee(fee);
        recalculateMonthlyRoomCosts(booking.getMember(), YearMonth.from(booking.getStartTime()));
        return new CancellationResponse(booking.getId(), booking.getStatus(), fee);
    }

    @Override
    public BookingResponse complete(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.CONFIRMED) throw Errors.conflict("Solo se puede completar una reserva confirmada", "RN-09");
        booking.setStatus(BookingStatus.COMPLETED);
        return map(booking);
    }

    @Override @Transactional(readOnly = true)
    public List<AvailabilityResponse> availability(Long locationId, LocalDate date, LocalTime start, LocalTime end,
                                                    Integer minimumCapacity, List<Long> equipmentIds) {
        int capacity = minimumCapacity == null ? 1 : minimumCapacity;
        if (capacity < 1) throw Errors.invalid("La capacidad mínima debe ser al menos 1", "VALIDATION_ERROR");
        List<Long> requestedEquipment = equipmentIds == null ? List.of() : equipmentIds.stream().distinct().toList();
        if (requestedEquipment.size() != (equipmentIds == null ? 0 : equipmentIds.size()))
            throw Errors.invalid("No se permiten equipamientos repetidos", "VALIDATION_ERROR");
        if (equipment.findAllById(requestedEquipment).size() != requestedEquipment.size())
            throw Errors.notFound("Equipamiento");
        LocalDateTime startTime = date.atTime(start), endTime = date.atTime(end);
        if (!startTime.isBefore(endTime)) throw Errors.invalid("La hora de inicio debe ser anterior a la hora de fin", "VALIDATION_ERROR");
        return rooms.findAvailable(locationId, capacity, startTime, endTime).stream()
            .filter(room -> !start.isBefore(room.getLocation().getOpeningTime())
                && !end.isAfter(room.getLocation().getClosingTime()))
            .filter(room -> room.getEquipment().stream().map(Equipment::getId).collect(java.util.stream.Collectors.toSet())
                .containsAll(requestedEquipment))
            .map(room -> new AvailabilityResponse(room.getId(), room.getName(), room.getType(), room.getCapacity(),
                room.getHourlyRate(), room.getLocation().getId(), room.getEquipment().stream()
                .map(e -> new EquipmentResponse(e.getId(), e.getName())).toList())).toList();
    }

    private void appendRequestedServices(Booking booking, List<BookingItemRequest> requested) {
        if (requested == null) return;
        for (BookingItemRequest input : requested) {
            AdditionalService service = services.findById(input.servicioId()).orElseThrow(() -> Errors.notFound("Servicio adicional"));
            BookingItem item = new BookingItem();
            item.setBooking(booking); item.setAdditionalService(service); item.setQuantity(input.cantidad());
            item.setAppliedUnitPrice(service.getUnitPrice()); booking.getItems().add(item);
        }
        long distinct = booking.getItems().stream().map(item -> item.getAdditionalService().getId()).distinct().count();
        if (distinct != booking.getItems().size()) throw Errors.invalid("Cada servicio debe aparecer una sola vez", "VALIDATION_ERROR");
    }

    private void calculateServicesCost(Booking booking) {
        BigDecimal cost = booking.getItems().stream()
            .map(item -> item.getAppliedUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        booking.setServicesCost(cost);
        booking.setTotalCost(booking.getRoomCost().add(cost).setScale(2, RoundingMode.HALF_UP));
    }

    private void recalculateMonthlyRoomCosts(Member member, YearMonth month) {
        List<Booking> monthBookings = bookings.findAllByMemberIdAndStartTimeGreaterThanEqualAndStartTimeLessThanAndStatusNotOrderByStartTime(
            member.getId(), month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay(), BookingStatus.CANCELLED);
        monthBookings = new ArrayList<>(monthBookings);
        monthBookings.sort(Comparator.comparing(Booking::getStartTime).thenComparing(Booking::getId));
        BigDecimal unusedIncludedHours = BigDecimal.valueOf(member.getMembershipPlan().getIncludedHoursPerMonth());
        BigDecimal discountFactor = BigDecimal.ONE.subtract(BigDecimal.valueOf(member.getMembershipPlan().getOverageDiscount())
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        for (Booking current : monthBookings) {
            BigDecimal duration = BigDecimal.valueOf(Duration.between(current.getStartTime(), current.getEndTime()).toMinutes())
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            BigDecimal includedForBooking = duration.min(unusedIncludedHours);
            unusedIncludedHours = unusedIncludedHours.subtract(includedForBooking);
            BigDecimal chargeable = duration.subtract(includedForBooking);
            BigDecimal roomCost = chargeable.multiply(current.getRoom().getHourlyRate()).multiply(discountFactor)
                .setScale(2, RoundingMode.HALF_UP);
            current.setRoomCost(roomCost);
            current.setTotalCost(roomCost.add(current.getServicesCost()).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private void requirePending(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) throw Errors.conflict("Los cambios solo se permiten en reservas pendientes", "RN-10");
    }
    private Booking findBooking(Long id) {
        return bookings.findDetailedByIdForUpdate(id).orElseThrow(() -> Errors.notFound("Reserva"));
    }
    private BookingResponse map(Booking b) {
        List<BookingItemResponse> mappedItems = b.getItems().stream().map(item ->
            new BookingItemResponse(item.getAdditionalService().getId(), item.getAdditionalService().getName(),
                item.getQuantity(), item.getAppliedUnitPrice())).toList();
        return new BookingResponse(b.getId(), b.getStartTime(), b.getEndTime(), b.getAttendees(), b.getStatus(),
            b.getRoomCost(), b.getServicesCost(), b.getTotalCost(), b.getCancellationFee(), b.getRoom().getId(),
            b.getRoom().getName(), b.getMember().getId(), b.getMember().getFullName(), mappedItems);
    }
}
