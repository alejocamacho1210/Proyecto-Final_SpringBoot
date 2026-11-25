package com.coworkhub.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "additional_services")
@Getter @Setter @NoArgsConstructor
public class AdditionalService {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false, unique = true) private String name;
    @DecimalMin("0.00") @Column(name = "unit_price", nullable = false, precision = 10, scale = 2) private BigDecimal unitPrice;
    @OneToMany(mappedBy = "additionalService", fetch = FetchType.LAZY) private List<BookingItem> bookingItems = new ArrayList<>();
}
