package com.coworkhub.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "membership_plans")
@Getter @Setter @NoArgsConstructor
public class MembershipPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false, unique = true) private String name;
    @Min(0) @Column(nullable = false) private int includedHoursPerMonth;
    @Min(0) @Column(nullable = false) private int overageDiscount;
    @Min(1) @Column(nullable = false) private int maxActiveBookings;
    @OneToMany(mappedBy = "membershipPlan", fetch = FetchType.LAZY) private List<Member> members = new ArrayList<>();
}
