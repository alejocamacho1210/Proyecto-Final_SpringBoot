package com.coworkhub.presentation;

import java.time.YearMonth;
import java.time.Clock;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.coworkhub.business.Errors;
import com.coworkhub.business.MemberService;
import com.coworkhub.domain.MemberStatus;
import com.coworkhub.presentation.ApiDtos.*;
import com.coworkhub.security.AccountUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/miembros")
@SecurityRequirement(name = "bearerAuth")
public class MemberController {
    private final MemberService members;
    private final Clock clock;
    public MemberController(MemberService members, Clock clock) { this.members = members; this.clock = clock; }

    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public List<MemberResponse> list(@RequestParam(required = false) String nombre,
                                     @RequestParam(required = false) MemberStatus estado) {
        return members.list(nombre, estado);
    }
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') or @authz.isMemberOwner(#id, authentication)")
    public MemberResponse get(@PathVariable Long id) { return members.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public MemberResponse create(@Valid @RequestBody MemberRequest request) { return members.create(request); }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberUpdateRequest request) { return members.update(id, request); }
    @PatchMapping("/{id}/suspender") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public MemberResponse suspend(@PathVariable Long id) { return members.setStatus(id, MemberStatus.SUSPENDED); }
    @PatchMapping("/{id}/activar") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public MemberResponse activate(@PathVariable Long id) { return members.setStatus(id, MemberStatus.ACTIVE); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public void delete(@PathVariable Long id) { members.delete(id); }

    @GetMapping("/{id}/consumo") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') or @authz.isMemberOwner(#id, authentication)")
    public ConsumptionResponse consumption(@PathVariable Long id, @RequestParam(required = false) String mes) {
        YearMonth month;
        try { month = mes == null ? YearMonth.now(clock) : YearMonth.parse(mes); }
        catch (DateTimeParseException ex) { throw Errors.invalid("El mes debe tener formato yyyy-MM", "VALIDATION_ERROR"); }
        return members.consumption(id, month);
    }
}
