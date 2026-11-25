package com.coworkhub.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_room_time", columnList = "room_id,start_time,end_time,status"),
    @Index(name = "idx_booking_member_start", columnList = "member_id,start_time,status")
})
@Getter @Setter @NoArgsConstructor
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "start_time", nullable = false) private LocalDateTime startTime;
    @Column(name = "end_time", nullable = false) private LocalDateTime endTime;
    @Column(nullable = false) private int attendees;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BookingStatus status;
    @Column(name = "room_cost", nullable = false, precision = 10, scale = 2) private BigDecimal roomCost = BigDecimal.ZERO;
    @Column(name = "services_cost", nullable = false, precision = 10, scale = 2) private BigDecimal servicesCost = BigDecimal.ZERO;
    @Column(name = "total_cost", nullable = false, precision = 10, scale = 2) private BigDecimal totalCost = BigDecimal.ZERO;
    @Column(name = "cancellation_fee", nullable = false, precision = 10, scale = 2) private BigDecimal cancellationFee = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_id", nullable = false) private Room room;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "member_id", nullable = false) private Member member;
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookingItem> items = new ArrayList<>();
}
