package com.coworkhub.domain;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_items", uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id", "additional_service_id"}))
@Getter @Setter @NoArgsConstructor
public class BookingItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private int quantity;
    @Column(name = "applied_unit_price", nullable = false, precision = 10, scale = 2) private BigDecimal appliedUnitPrice;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id", nullable = false) private Booking booking;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "additional_service_id", nullable = false) private AdditionalService additionalService;
}
