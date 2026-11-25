package com.coworkhub.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.coworkhub.domain.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
    @Override @EntityGraph(attributePaths = {"location", "equipment"})
    List<Room> findAll();
    @EntityGraph(attributePaths = {"location", "equipment"})
    List<Room> findAllByLocationId(Long locationId);
    boolean existsByLocationIdAndNameIgnoreCase(Long locationId, String name);
    boolean existsByLocationIdAndNameIgnoreCaseAndIdNot(Long locationId, String name, Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"location", "equipment"})
    @Query("""
        select r from Room r where r.location.id = :locationId and r.active = true
        and r.capacity >= :capacity
        and not exists (select b.id from Booking b where b.room = r
            and b.status in (com.coworkhub.domain.BookingStatus.PENDING, com.coworkhub.domain.BookingStatus.CONFIRMED)
            and b.startTime < :endTime and b.endTime > :startTime)
        """)
    List<Room> findAvailable(@Param("locationId") Long locationId, @Param("capacity") int capacity,
                             @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
