package com.coworkhub.security;

import com.coworkhub.persistence.BookingRepository;
import com.coworkhub.persistence.MemberRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("authz")
public class AuthorizationService {
    private final BookingRepository bookings;
    private final MemberRepository members;
    public AuthorizationService(BookingRepository bookings, MemberRepository members) {
        this.bookings = bookings;
        this.members = members;
    }
    public boolean isBookingOwner(Long bookingId, Authentication authentication) {
        AccountUserDetails user = (AccountUserDetails) authentication.getPrincipal();
        return user.memberId() != null && bookings.findDetailedById(bookingId)
            .map(booking -> booking.getMember().getId().equals(user.memberId())).orElse(false);
    }
    public boolean isMemberOwner(Long memberId, Authentication authentication) {
        AccountUserDetails user = (AccountUserDetails) authentication.getPrincipal();
        return user.memberId() != null && user.memberId().equals(memberId);
    }
}
