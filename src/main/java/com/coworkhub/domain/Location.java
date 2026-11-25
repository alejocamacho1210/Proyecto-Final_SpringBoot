package com.coworkhub.domain;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "locations")
@Getter @Setter @NoArgsConstructor
public class Location {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false) private String name;
    @NotBlank @Column(nullable = false) private String city;
    @NotBlank @Column(nullable = false) private String address;
    @Column(nullable = false) private LocalTime openingTime;
    @Column(nullable = false) private LocalTime closingTime;
    @OneToMany(mappedBy = "location", fetch = FetchType.LAZY) private List<Room> rooms = new ArrayList<>();
}
