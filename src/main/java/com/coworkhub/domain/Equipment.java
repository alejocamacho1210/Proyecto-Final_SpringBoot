package com.coworkhub.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipment")
@Getter @Setter @NoArgsConstructor
public class Equipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false, unique = true) private String name;
    @ManyToMany(mappedBy = "equipment", fetch = FetchType.LAZY) private Set<Room> rooms = new HashSet<>();
}
