package com.coworkhub.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(columnNames = {"location_id", "name"}))
@Getter @Setter @NoArgsConstructor
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RoomType type;
    @Min(1) @Column(nullable = false) private int capacity;
    @DecimalMin("0.00") @Column(nullable = false, precision = 10, scale = 2) private BigDecimal hourlyRate;
    @Column(nullable = false) private boolean active = true;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "location_id", nullable = false) private Location location;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "room_equipment", joinColumns = @JoinColumn(name = "room_id"), inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private Set<Equipment> equipment = new HashSet<>();
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY) private List<Booking> bookings = new ArrayList<>();
}
