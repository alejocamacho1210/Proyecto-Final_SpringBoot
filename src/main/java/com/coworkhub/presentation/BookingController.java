package com.coworkhub.presentation;

import java.time.LocalDate;
import java.util.List;

import com.coworkhub.business.BookingService;
import com.coworkhub.business.Errors;
import com.coworkhub.domain.BookingStatus;
import com.coworkhub.presentation.ApiDtos.*;
import com.coworkhub.security.AccountUserDetails;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/reservas")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {
    private final BookingService bookings;
    public BookingController(BookingService bookings) { this.bookings = bookings; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','MEMBER')")
    public BookingResponse create(@Valid @RequestBody BookingRequest request, @AuthenticationPrincipal AccountUserDetails user) {
        if (user.account().getRole() == com.coworkhub.domain.Role.MEMBER &&
            (user.memberId() == null || !user.memberId().equals(request.miembroId())))
            throw Errors.forbidden("Un miembro solo puede reservar a su propio nombre");
        return bookings.create(request);
    }

    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') or @authz.isBookingOwner(#id, authentication)")
    public BookingResponse update(@PathVariable Long id, @Valid @RequestBody BookingUpdateRequest request) {
        return bookings.update(id, request);
    }

    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') or @authz.isBookingOwner(#id, authentication)")
    public BookingResponse get(@PathVariable Long id) { return bookings.get(id); }

    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','MEMBER')")
    public List<BookingResponse> list(@RequestParam(required = false) Long miembroId,
        @RequestParam(required = false) Long salaId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        @RequestParam(required = false) BookingStatus estado,
        @AuthenticationPrincipal AccountUserDetails user) {
        if ((salaId == null) != (fecha == null)) throw Errors.invalid("salaId y fecha deben enviarse juntos", "VALIDATION_ERROR");
        if (user.account().getRole() == com.coworkhub.domain.Role.MEMBER) {
            if (miembroId == null || !miembroId.equals(user.memberId()) || salaId != null)
                throw Errors.forbidden("Un miembro solo puede listar sus reservas usando su propio miembroId");
        }
        return bookings.list(miembroId, salaId, fecha, estado);
    }

    @PostMapping("/{id}/servicios") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') or @authz.isBookingOwner(#id, authentication)")
    public BookingResponse addService(@PathVariable Long id, @Valid @RequestBody BookingItemRequest request) {
        return bookings.addService(id, request);
    }
    @DeleteMapping("/{id}/servicios/{servicioId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') or @authz.isBookingOwner(#id, authentication)")
    public BookingResponse removeService(@PathVariable Long id, @PathVariable Long servicioId) {
        return bookings.removeService(id, servicioId);
    }
    @PostMapping("/{id}/confirmar") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public BookingResponse confirm(@PathVariable Long id) { return bookings.confirm(id); }
    @PostMapping("/{id}/cancelar") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') or @authz.isBookingOwner(#id, authentication)")
    public CancellationResponse cancel(@PathVariable Long id) { return bookings.cancel(id); }
    @PostMapping("/{id}/completar") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public BookingResponse complete(@PathVariable Long id) { return bookings.complete(id); }
}
