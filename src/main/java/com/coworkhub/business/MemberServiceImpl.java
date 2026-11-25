package com.coworkhub.business;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

import com.coworkhub.domain.*;
import com.coworkhub.persistence.*;
import com.coworkhub.presentation.ApiDtos.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberServiceImpl implements MemberService {
    private final MemberRepository members;
    private final MembershipPlanRepository plans;
    private final UserAccountRepository accounts;
    private final BookingRepository bookings;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public MemberServiceImpl(MemberRepository members, MembershipPlanRepository plans, UserAccountRepository accounts,
                            BookingRepository bookings, PasswordEncoder passwordEncoder, Clock clock) {
        this.members = members; this.plans = plans; this.accounts = accounts; this.bookings = bookings;
        this.passwordEncoder = passwordEncoder; this.clock = clock;
    }

    @Override @Transactional(readOnly = true)
    public List<MemberResponse> list(String name, MemberStatus status) {
        String filter = name == null ? "" : name.trim();
        List<Member> result;
        if (status != null) result = members.findAllByFullNameContainingIgnoreCaseAndStatus(filter, status);
        else result = members.findAllByFullNameContainingIgnoreCase(filter);
        return result.stream().map(this::map).toList();
    }
    @Override @Transactional(readOnly = true)
    public MemberResponse get(Long id) { return map(findMember(id)); }

    @Override
    public MemberResponse create(MemberRequest request) {
        if (members.existsByDocumentNumberIgnoreCase(request.documentNumber().trim()))
            throw Errors.conflict("El documento ya está registrado", "RN-11");
        if (members.existsByEmailIgnoreCase(request.email().trim()))
            throw Errors.conflict("El email ya está registrado", "RN-11");
        if (accounts.existsByUsernameIgnoreCase(request.username().trim()))
            throw Errors.conflict("El usuario ya está registrado", "RN-11");
        MembershipPlan plan = plans.findById(request.planId()).orElseThrow(() -> Errors.notFound("Plan"));
        Member member = new Member();
        member.setDocumentNumber(request.documentNumber().trim()); member.setFullName(request.fullName().trim());
        member.setEmail(request.email().trim().toLowerCase()); member.setStatus(MemberStatus.ACTIVE);
        member.setMembershipPlan(plan);
        member = members.save(member);
        UserAccount account = new UserAccount();
        account.setUsername(request.username().trim()); account.setPassword(passwordEncoder.encode(request.password()));
        account.setRole(Role.MEMBER); account.setMember(member); member.setUserAccount(account);
        accounts.save(account);
        return map(member);
    }

    @Override
    public MemberResponse update(Long id, MemberUpdateRequest request) {
        Member member = findMember(id);
        if (members.existsByEmailIgnoreCaseAndIdNot(request.email().trim(), id))
            throw Errors.conflict("El email ya está registrado", "RN-11");
        member.setFullName(request.fullName().trim()); member.setEmail(request.email().trim().toLowerCase());
        member.setMembershipPlan(plans.findById(request.planId()).orElseThrow(() -> Errors.notFound("Plan")));
        return map(member);
    }

    @Override
    public MemberResponse setStatus(Long id, MemberStatus status) {
        Member member = findMember(id);
        member.setStatus(status);
        return map(member);
    }

    @Override
    public void delete(Long id) {
        Member member = findMember(id);
        if (!member.getBookings().isEmpty()) throw Errors.conflict("No se puede eliminar un miembro con reservas", "RN-12");
        UserAccount account = member.getUserAccount();
        if (account != null) accounts.delete(account);
        members.delete(member);
    }

    @Override @Transactional(readOnly = true)
    public ConsumptionResponse consumption(Long id, YearMonth month) {
        Member member = findMember(id);
        var start = month.atDay(1).atStartOfDay();
        var end = month.plusMonths(1).atDay(1).atStartOfDay();
        List<Booking> monthly = bookings.findAllByMemberIdAndStartTimeGreaterThanEqualAndStartTimeLessThanAndStatusNotOrderByStartTime(
            id, start, end, BookingStatus.CANCELLED);
        BigDecimal used = monthly.stream().map(b -> BigDecimal.valueOf(java.time.Duration.between(b.getStartTime(), b.getEndTime()).toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal included = BigDecimal.valueOf(member.getMembershipPlan().getIncludedHoursPerMonth());
        BigDecimal remaining = included.subtract(used).max(BigDecimal.ZERO);
        BigDecimal excess = used.subtract(included).max(BigDecimal.ZERO);
        return new ConsumptionResponse(id, month.toString(), included.intValue(), used, remaining, excess);
    }

    private Member findMember(Long id) { return members.findById(id).orElseThrow(() -> Errors.notFound("Miembro")); }
    private MemberResponse map(Member v) {
        return new MemberResponse(v.getId(), v.getDocumentNumber(), v.getFullName(), v.getEmail(), v.getStatus(),
            v.getMembershipPlan().getId(), v.getMembershipPlan().getName(), v.getUserAccount() == null ? null : v.getUserAccount().getUsername());
    }
}
