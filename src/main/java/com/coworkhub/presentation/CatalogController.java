package com.coworkhub.presentation;

import java.util.List;

import com.coworkhub.business.CatalogService;
import com.coworkhub.presentation.ApiDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class CatalogController {
    private final CatalogService catalogs;
    public CatalogController(CatalogService catalogs) { this.catalogs = catalogs; }

    @GetMapping("/sedes") public List<LocationResponse> locations() { return catalogs.locations(); }
    @GetMapping("/sedes/{id}") public LocationResponse location(@PathVariable Long id) { return catalogs.location(id); }
    @PostMapping("/sedes") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public LocationResponse createLocation(@Valid @RequestBody LocationRequest request) { return catalogs.saveLocation(null, request); }
    @PutMapping("/sedes/{id}") @PreAuthorize("hasRole('ADMIN')")
    public LocationResponse updateLocation(@PathVariable Long id, @Valid @RequestBody LocationRequest request) { return catalogs.saveLocation(id, request); }
    @DeleteMapping("/sedes/{id}") @PreAuthorize("hasRole('ADMIN')")
    public void deleteLocation(@PathVariable Long id) { catalogs.deleteLocation(id); }

    @GetMapping("/salas") public List<RoomResponse> rooms(@RequestParam(required = false) Long sedeId) { return catalogs.rooms(sedeId); }
    @GetMapping("/salas/{id}") public RoomResponse room(@PathVariable Long id) { return catalogs.room(id); }
    @PostMapping("/salas") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse createRoom(@Valid @RequestBody RoomRequest request) { return catalogs.saveRoom(null, request); }
    @PutMapping("/salas/{id}") @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) { return catalogs.saveRoom(id, request); }
    @DeleteMapping("/salas/{id}") @PreAuthorize("hasRole('ADMIN')")
    public void deleteRoom(@PathVariable Long id) { catalogs.deleteRoom(id); }

    @GetMapping("/equipamientos") public List<EquipmentResponse> equipment() { return catalogs.equipment(); }
    @GetMapping("/equipamientos/{id}") public EquipmentResponse equipment(@PathVariable Long id) { return catalogs.equipment(id); }
    @PostMapping("/equipamientos") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public EquipmentResponse createEquipment(@Valid @RequestBody EquipmentRequest request) { return catalogs.saveEquipment(null, request); }
    @PutMapping("/equipamientos/{id}") @PreAuthorize("hasRole('ADMIN')")
    public EquipmentResponse updateEquipment(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) { return catalogs.saveEquipment(id, request); }
    @DeleteMapping("/equipamientos/{id}") @PreAuthorize("hasRole('ADMIN')")
    public void deleteEquipment(@PathVariable Long id) { catalogs.deleteEquipment(id); }

    @GetMapping("/servicios-adicionales") public List<ServiceResponse> services() { return catalogs.services(); }
    @GetMapping("/servicios-adicionales/{id}") public ServiceResponse service(@PathVariable Long id) { return catalogs.service(id); }
    @PostMapping("/servicios-adicionales") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse createService(@Valid @RequestBody ServiceRequest request) { return catalogs.saveService(null, request); }
    @PutMapping("/servicios-adicionales/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse updateService(@PathVariable Long id, @Valid @RequestBody ServiceRequest request) { return catalogs.saveService(id, request); }
    @DeleteMapping("/servicios-adicionales/{id}") @PreAuthorize("hasRole('ADMIN')")
    public void deleteService(@PathVariable Long id) { catalogs.deleteService(id); }

    @GetMapping("/planes") public List<PlanResponse> plans() { return catalogs.plans(); }
    @GetMapping("/planes/{id}") public PlanResponse plan(@PathVariable Long id) { return catalogs.plan(id); }
    @PostMapping("/planes") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public PlanResponse createPlan(@Valid @RequestBody PlanRequest request) { return catalogs.savePlan(null, request); }
    @PutMapping("/planes/{id}") @PreAuthorize("hasRole('ADMIN')")
    public PlanResponse updatePlan(@PathVariable Long id, @Valid @RequestBody PlanRequest request) { return catalogs.savePlan(id, request); }
    @DeleteMapping("/planes/{id}") @PreAuthorize("hasRole('ADMIN')")
    public void deletePlan(@PathVariable Long id) { catalogs.deletePlan(id); }
}
