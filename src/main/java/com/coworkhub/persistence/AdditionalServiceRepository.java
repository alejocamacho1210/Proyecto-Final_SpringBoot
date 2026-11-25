package com.coworkhub.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.coworkhub.domain.AdditionalService;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
