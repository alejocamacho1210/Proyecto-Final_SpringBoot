package com.coworkhub.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.coworkhub.domain.Equipment;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsByIdIn(java.util.Collection<Long> ids);
}
