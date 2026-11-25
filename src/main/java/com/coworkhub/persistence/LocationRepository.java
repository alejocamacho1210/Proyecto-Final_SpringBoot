package com.coworkhub.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.coworkhub.domain.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
    boolean existsByNameIgnoreCaseAndCityIgnoreCaseAndIdNot(String name, String city, Long id);
}
