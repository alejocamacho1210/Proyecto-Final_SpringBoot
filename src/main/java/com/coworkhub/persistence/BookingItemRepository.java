package com.coworkhub.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.coworkhub.domain.BookingItem;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {
    boolean existsByAdditionalServiceId(Long serviceId);
}
