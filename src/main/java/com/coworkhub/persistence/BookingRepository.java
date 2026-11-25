package com.coworkhub.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import com.coworkhub.domain.Booking;
import com.coworkhub.domain.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByRoomId(Long roomId);
    @Override
    @EntityGraph(attributePaths = {"room", "member", "member.membershipPlan", "items", "items.additionalService"})
    List<Booking> findAll();
    @Query("""
        select count(b) from Booking b where b.member.id = :memberId
        and b.status in (com.coworkhub.domain.BookingStatus.PENDING, com.coworkhub.domain.BookingStatus.CONFIRMED)
        and b.startTime > :now
        """)
    long countActiveFutureByMember(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);
    @Query("""
        select b from Booking b where b.room.id = :roomId
        and b.status in (com.coworkhub.domain.BookingStatus.PENDING, com.coworkhub.domain.BookingStatus.CONFIRMED)
        and b.startTime < :endTime and b.endTime > :startTime
        """)
    List<Booking> findOverlapping(@Param("roomId") Long roomId, @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);
    @EntityGraph(attributePaths = {"room", "member", "member.membershipPlan", "items", "items.additionalService"})
    @Query("select b from Booking b where b.id = :id")
    java.util.Optional<Booking> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"room", "member", "member.membershipPlan", "items", "items.additionalService"})
    @Query("select b from Booking b where b.id = :id")
    java.util.Optional<Booking> findDetailedByIdForUpdate(@Param("id") Long id);
    @EntityGraph(attributePaths = {"room", "member", "items", "items.additionalService"})
    List<Booking> findAllByMemberId(Long memberId);
    @EntityGraph(attributePaths = {"room", "member", "items", "items.additionalService"})
    List<Booking> findAllByRoomIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(Long roomId, LocalDateTime start, LocalDateTime end);
    @EntityGraph(attributePaths = {"room", "member", "items", "items.additionalService"})
    List<Booking> findAllByStatus(BookingStatus status);
    @EntityGraph(attributePaths = {"room", "member", "items", "items.additionalService"})
    List<Booking> findAllByMemberIdAndStartTimeGreaterThanEqualAndStartTimeLessThanAndStatusNotOrderByStartTime(
        Long memberId, LocalDateTime start, LocalDateTime end, BookingStatus excludedStatus);
}
