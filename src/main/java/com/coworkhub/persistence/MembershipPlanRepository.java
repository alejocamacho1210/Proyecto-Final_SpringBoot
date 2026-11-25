package com.coworkhub.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.coworkhub.domain.MembershipPlan;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
