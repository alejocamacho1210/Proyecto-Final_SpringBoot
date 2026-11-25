package com.coworkhub.business;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.coworkhub.domain.*;
import com.coworkhub.persistence.*;
import com.coworkhub.presentation.ApiDtos.BookingRequest;
import com.coworkhub.presentation.ApiDtos.BookingUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingServiceImplTest {
    private BookingRepository bookings;
    private RoomRepository rooms;
    private MemberRepository members;
    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        bookings = mock(BookingRepository.class);
        rooms = mock(RoomRepository.class);
        members = mock(MemberRepository.class);
        service = new BookingServiceImpl(bookings, rooms, members, mock(AdditionalServiceRepository.class),
            mock(EquipmentRepository.class), Clock.fixed(Instant.parse("2030-06-03T08:00:00Z"), ZoneId.of("UTC")));
    }

    @Test
    void rejectsDurationsThatAreNotThirtyMinuteMultiples() {
        Room room = room();
        Member member = member(0, 0, 3);
        when(rooms.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
        BookingRequest request = new BookingRequest(1L, 1L,
            LocalDateTime.parse("2030-06-03T10:00:00"), LocalDateTime.parse("2030-06-03T10:45:00"), 2, List.of());

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals("RN-02", error.getCode());
        verify(bookings, never()).saveAndFlush(any());
    }

    @Test
    void rejectsOverlappingBookingsWithRn01() {
        Room room = room();
        Member member = member(0, 0, 3);
        when(rooms.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
        when(bookings.countActiveFutureByMember(eq(1L), any())).thenReturn(0L);
        when(bookings.findOverlapping(eq(1L), any(), any())).thenReturn(List.of(new Booking()));
        BookingRequest request = new BookingRequest(1L, 1L,
            LocalDateTime.parse("2030-06-03T10:00:00"), LocalDateTime.parse("2030-06-03T11:00:00"), 2, List.of());

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals("RN-01", error.getCode());
        verify(bookings, never()).saveAndFlush(any());
    }

    @Test
    void updatesPendingBookingAndDoesNotTreatItselfAsAnOverlap() {
        Room room = room();
        Member member = member(10, 0, 3);
        Booking booking = booking(LocalDateTime.parse("2030-06-04T10:00:00"), BigDecimal.ZERO);
        when(bookings.findDetailedByIdForUpdate(8L)).thenReturn(Optional.of(booking));
        when(rooms.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
        when(bookings.findOverlapping(1L, LocalDateTime.parse("2030-06-05T10:00:00"),
            LocalDateTime.parse("2030-06-05T11:00:00"))).thenReturn(List.of(booking));
        when(bookings.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookings.findAllByMemberIdAndStartTimeGreaterThanEqualAndStartTimeLessThanAndStatusNotOrderByStartTime(
            eq(1L), any(), any(), eq(BookingStatus.CANCELLED))).thenAnswer(invocation -> List.of(booking));
        when(bookings.findDetailedById(8L)).thenReturn(Optional.of(booking));

        var response = service.update(8L, new BookingUpdateRequest(1L,
            LocalDateTime.parse("2030-06-05T10:00:00"), LocalDateTime.parse("2030-06-05T11:00:00"), 3));

        assertEquals(LocalDateTime.parse("2030-06-05T10:00:00"), response.startTime());
        assertEquals(3, response.attendees());
        assertEquals(BookingStatus.PENDING, response.status());
        verify(bookings).saveAndFlush(booking);
    }

    @Test
    void rejectsUpdateWhenAnotherBookingOccupiesTheRequestedTime() {
        Room room = room();
        Member member = member(10, 0, 3);
        Booking booking = booking(LocalDateTime.parse("2030-06-04T10:00:00"), BigDecimal.ZERO);
        Booking overlapping = new Booking();
        overlapping.setId(9L);
        when(bookings.findDetailedByIdForUpdate(8L)).thenReturn(Optional.of(booking));
        when(rooms.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
        when(bookings.findOverlapping(1L, LocalDateTime.parse("2030-06-05T10:00:00"),
            LocalDateTime.parse("2030-06-05T11:00:00"))).thenReturn(List.of(booking, overlapping));

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(8L,
            new BookingUpdateRequest(1L, LocalDateTime.parse("2030-06-05T10:00:00"),
                LocalDateTime.parse("2030-06-05T11:00:00"), 3)));

        assertEquals("RN-01", error.getCode());
        verify(bookings, never()).saveAndFlush(any());
    }

    @Test
    void onlyAllowsEditingPendingBookings() {
        Booking booking = booking(LocalDateTime.parse("2030-06-04T10:00:00"), BigDecimal.ZERO);
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookings.findDetailedByIdForUpdate(8L)).thenReturn(Optional.of(booking));

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(8L,
            new BookingUpdateRequest(1L, LocalDateTime.parse("2030-06-05T10:00:00"),
                LocalDateTime.parse("2030-06-05T11:00:00"), 3)));

        assertEquals("RN-10", error.getCode());
        verifyNoInteractions(rooms, members);
        verify(bookings, never()).saveAndFlush(any());
    }

    @Test
    void calculatesRoomOverageAndReturnsTotalCost() {
        Room room = room();
        Member member = member(0, 0, 3);
        when(rooms.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
        when(bookings.countActiveFutureByMember(eq(1L), any())).thenReturn(0L);
        when(bookings.findOverlapping(eq(1L), any(), any())).thenReturn(List.of());
        AtomicReference<Booking> savedBooking = new AtomicReference<>();
        when(bookings.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(50L);
            savedBooking.set(saved);
            return saved;
        });
        when(bookings.findAllByMemberIdAndStartTimeGreaterThanEqualAndStartTimeLessThanAndStatusNotOrderByStartTime(
            eq(1L), any(), any(), eq(BookingStatus.CANCELLED))).thenAnswer(invocation -> List.of(savedBooking.get()));
        when(bookings.findDetailedById(50L)).thenAnswer(invocation -> Optional.of(savedBooking.get()));

        var response = service.create(new BookingRequest(1L, 1L, LocalDateTime.parse("2030-06-03T10:00:00"),
            LocalDateTime.parse("2030-06-03T11:00:00"), 2, List.of()));

        assertEquals(new BigDecimal("60.00"), response.roomCost());
        assertEquals(new BigDecimal("0.00"), response.servicesCost());
        assertEquals(new BigDecimal("60.00"), response.totalCost());
    }

    @Test
    void rejectsDurationsWithSecondsOutsideThirtyMinuteMultiples() {
        Room room = room();
        Member member = member(10, 0, 3);
        when(rooms.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
        BookingRequest request = new BookingRequest(1L, 1L,
            LocalDateTime.parse("2030-06-03T10:00:00"),
            LocalDateTime.parse("2030-06-03T10:30:59"), 2, List.of());

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals("RN-02", error.getCode());
        verify(bookings, never()).saveAndFlush(any());
    }

    @Test
    void chargesHalfOfRoomCostWhenCancelledBetweenTwoAndTwentyFourHoursBeforeStart() {
        Booking booking = booking(LocalDateTime.parse("2030-06-03T13:00:00"), new BigDecimal("80.00"));
        when(bookings.findDetailedByIdForUpdate(8L)).thenReturn(Optional.of(booking));
        when(bookings.findAllByMemberIdAndStartTimeGreaterThanEqualAndStartTimeLessThanAndStatusNotOrderByStartTime(
            anyLong(), any(), any(), any())).thenReturn(List.of());

        var response = service.cancel(8L);

        assertEquals(BookingStatus.CANCELLED, response.status());
        assertEquals(new BigDecimal("40.00"), response.cancellationFee());
    }

    private Room room() {
        Location location = new Location();
        location.setOpeningTime(java.time.LocalTime.of(8, 0));
        location.setClosingTime(java.time.LocalTime.of(20, 0));
        Room room = new Room();
        room.setId(1L); room.setName("Sala de pruebas"); room.setLocation(location); room.setCapacity(4);
        room.setActive(true); room.setHourlyRate(new BigDecimal("60.00"));
        return room;
    }

    private Member member(int includedHours, int discount, int maxBookings) {
        MembershipPlan plan = new MembershipPlan();
        plan.setIncludedHoursPerMonth(includedHours); plan.setOverageDiscount(discount); plan.setMaxActiveBookings(maxBookings);
        Member member = new Member();
        member.setId(1L); member.setStatus(MemberStatus.ACTIVE); member.setMembershipPlan(plan); member.setFullName("Test");
        return member;
    }

    private Booking booking(LocalDateTime start, BigDecimal roomCost) {
        Booking booking = new Booking();
        booking.setId(8L); booking.setStatus(BookingStatus.PENDING); booking.setStartTime(start);
        booking.setEndTime(start.plusHours(1)); booking.setRoomCost(roomCost); booking.setServicesCost(BigDecimal.ZERO);
        booking.setTotalCost(roomCost); booking.setMember(member(10, 0, 3));
        booking.setRoom(room());
        return booking;
    }
}
