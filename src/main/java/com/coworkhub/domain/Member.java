package com.coworkhub.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "members")
@Getter @Setter @NoArgsConstructor
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(name = "document_number", nullable = false, unique = true) private String documentNumber;
    @NotBlank @Column(name = "full_name", nullable = false) private String fullName;
    @Email @Column(nullable = false, unique = true) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MemberStatus status = MemberStatus.ACTIVE;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "membership_plan_id", nullable = false) private MembershipPlan membershipPlan;
    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY) private UserAccount userAccount;
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY) private List<Booking> bookings = new ArrayList<>();
}
