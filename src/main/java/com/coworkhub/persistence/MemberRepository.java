package com.coworkhub.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import com.coworkhub.domain.Member;
import com.coworkhub.domain.MemberStatus;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    java.util.Optional<Member> findByIdForUpdate(@Param("id") Long id);
    boolean existsByDocumentNumberIgnoreCase(String documentNumber);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByDocumentNumberIgnoreCaseAndIdNot(String documentNumber, Long id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    @EntityGraph(attributePaths = {"membershipPlan", "userAccount"})
    List<Member> findAllByFullNameContainingIgnoreCaseAndStatus(String name, MemberStatus status);
    @EntityGraph(attributePaths = {"membershipPlan", "userAccount"})
    List<Member> findAllByFullNameContainingIgnoreCase(String name);
    @EntityGraph(attributePaths = {"membershipPlan", "userAccount"})
    List<Member> findAllByStatus(MemberStatus status);
    @EntityGraph(attributePaths = {"membershipPlan", "userAccount"})
    List<Member> findAll();
}
