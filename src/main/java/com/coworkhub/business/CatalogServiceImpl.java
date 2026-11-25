package com.coworkhub.business;

import java.util.List;

import com.coworkhub.domain.*;
import com.coworkhub.persistence.*;
import com.coworkhub.presentation.ApiDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogServiceImpl implements CatalogService {
    private final LocationRepository locations;
    private final RoomRepository rooms;
    private final EquipmentRepository equipment;
    private final MembershipPlanRepository plans;
    private final AdditionalServiceRepository services;
    private final BookingRepository bookings;
    private final BookingItemRepository items;

    public CatalogServiceImpl(LocationRepository locations, RoomRepository rooms, EquipmentRepository equipment,
                              MembershipPlanRepository plans, AdditionalServiceRepository services,
                              BookingRepository bookings, BookingItemRepository items) {
        this.locations = locations; this.rooms = rooms; this.equipment = equipment; this.plans = plans;
        this.services = services; this.bookings = bookings; this.items = items;
    }

    @Override @Transactional(readOnly = true)
    public List<LocationResponse> locations() { return locations.findAll().stream().map(this::mapLocation).toList(); }
    @Override @Transactional(readOnly = true)
    public LocationResponse location(Long id) { return mapLocation(findLocation(id)); }
    @Override
    public LocationResponse saveLocation(Long id, LocationRequest request) {
        if (!request.openingTime().isBefore(request.closingTime())) throw Errors.invalid("El horario de apertura debe ser anterior al cierre", "VALIDATION_ERROR");
        Location entity = id == null ? new Location() : findLocation(id);
        entity.setName(request.name().trim()); entity.setCity(request.city().trim()); entity.setAddress(request.address().trim());
        entity.setOpeningTime(request.openingTime()); entity.setClosingTime(request.closingTime());
        return mapLocation(locations.save(entity));
    }
    @Override
    public void deleteLocation(Long id) {
        Location entity = findLocation(id);
        if (!entity.getRooms().isEmpty()) throw Errors.conflict("No se puede eliminar una sede con salas", "RN-12");
        locations.delete(entity);
    }

    @Override @Transactional(readOnly = true)
    public List<RoomResponse> rooms(Long locationId) {
        return (locationId == null ? rooms.findAll() : rooms.findAllByLocationId(locationId)).stream().map(this::mapRoom).toList();
    }
    @Override @Transactional(readOnly = true)
    public RoomResponse room(Long id) { return mapRoom(findRoom(id)); }
    @Override
    public RoomResponse saveRoom(Long id, RoomRequest request) {
        Location location = locations.findById(request.locationId()).orElseThrow(() -> Errors.notFound("Sede"));
        Room room = id == null ? new Room() : findRoom(id);
        boolean duplicate = id == null ? rooms.existsByLocationIdAndNameIgnoreCase(location.getId(), request.name())
            : rooms.existsByLocationIdAndNameIgnoreCaseAndIdNot(location.getId(), request.name(), id);
        if (duplicate) throw Errors.conflict("Ya existe una sala con ese nombre en la sede", "RN-11");
        room.setName(request.name().trim()); room.setType(request.type()); room.setCapacity(request.capacity());
        room.setHourlyRate(request.hourlyRate()); room.setActive(request.active()); room.setLocation(location);
        var ids = request.equipmentIds() == null ? List.<Long>of() : request.equipmentIds().stream().distinct().toList();
        if (ids.size() != (request.equipmentIds() == null ? 0 : request.equipmentIds().size()))
            throw Errors.invalid("No se permiten equipamientos repetidos", "VALIDATION_ERROR");
        var selectedEquipment = equipment.findAllById(ids);
        if (selectedEquipment.size() != ids.size()) throw Errors.notFound("Equipamiento");
        room.setEquipment(new java.util.HashSet<>(selectedEquipment));
        return mapRoom(rooms.save(room));
    }
    @Override
    public void deleteRoom(Long id) {
        Room room = findRoom(id);
        if (bookings.existsByRoomId(id)) throw Errors.conflict("No se puede eliminar una sala con reservas", "RN-12");
        rooms.delete(room);
    }

    @Override @Transactional(readOnly = true)
    public List<EquipmentResponse> equipment() { return equipment.findAll().stream().map(this::mapEquipment).toList(); }
    @Override @Transactional(readOnly = true)
    public EquipmentResponse equipment(Long id) { return mapEquipment(findEquipment(id)); }
    @Override
    public EquipmentResponse saveEquipment(Long id, EquipmentRequest request) {
        String name = request.name().trim();
        if (id == null ? equipment.existsByNameIgnoreCase(name) : equipment.existsByNameIgnoreCaseAndIdNot(name, id))
            throw Errors.conflict("El equipamiento ya existe", "RN-11");
        Equipment entity = id == null ? new Equipment() : findEquipment(id);
        entity.setName(name);
        return mapEquipment(equipment.save(entity));
    }
    @Override
    public void deleteEquipment(Long id) {
        Equipment entity = findEquipment(id);
        if (!entity.getRooms().isEmpty()) throw Errors.conflict("No se puede eliminar equipamiento asignado a salas", "RN-12");
        equipment.delete(entity);
    }

    @Override @Transactional(readOnly = true)
    public List<ServiceResponse> services() { return services.findAll().stream().map(this::mapService).toList(); }
    @Override @Transactional(readOnly = true)
    public ServiceResponse service(Long id) { return mapService(findService(id)); }
    @Override
    public ServiceResponse saveService(Long id, ServiceRequest request) {
        String name = request.name().trim();
        if (id == null ? services.existsByNameIgnoreCase(name) : services.existsByNameIgnoreCaseAndIdNot(name, id))
            throw Errors.conflict("El servicio adicional ya existe", "RN-11");
        AdditionalService entity = id == null ? new AdditionalService() : findService(id);
        entity.setName(name); entity.setUnitPrice(request.unitPrice());
        return mapService(services.save(entity));
    }
    @Override
    public void deleteService(Long id) {
        AdditionalService entity = findService(id);
        if (items.existsByAdditionalServiceId(id)) throw Errors.conflict("No se puede eliminar un servicio utilizado", "RN-12");
        services.delete(entity);
    }

    @Override @Transactional(readOnly = true)
    public List<PlanResponse> plans() { return plans.findAll().stream().map(this::mapPlan).toList(); }
    @Override @Transactional(readOnly = true)
    public PlanResponse plan(Long id) { return mapPlan(findPlan(id)); }
    @Override
    public PlanResponse savePlan(Long id, PlanRequest request) {
        String name = request.name().trim();
        if (id == null ? plans.existsByNameIgnoreCase(name) : plans.existsByNameIgnoreCaseAndIdNot(name, id))
            throw Errors.conflict("El plan ya existe", "RN-11");
        MembershipPlan entity = id == null ? new MembershipPlan() : findPlan(id);
        entity.setName(name); entity.setIncludedHoursPerMonth(request.includedHoursPerMonth());
        entity.setOverageDiscount(request.overageDiscount()); entity.setMaxActiveBookings(request.maxActiveBookings());
        return mapPlan(plans.save(entity));
    }
    @Override
    public void deletePlan(Long id) {
        MembershipPlan entity = findPlan(id);
        if (!entity.getMembers().isEmpty()) throw Errors.conflict("No se puede eliminar un plan con miembros", "RN-12");
        plans.delete(entity);
    }

    private Location findLocation(Long id) { return locations.findById(id).orElseThrow(() -> Errors.notFound("Sede")); }
    private Room findRoom(Long id) { return rooms.findById(id).orElseThrow(() -> Errors.notFound("Sala")); }
    private Equipment findEquipment(Long id) { return equipment.findById(id).orElseThrow(() -> Errors.notFound("Equipamiento")); }
    private AdditionalService findService(Long id) { return services.findById(id).orElseThrow(() -> Errors.notFound("Servicio adicional")); }
    private MembershipPlan findPlan(Long id) { return plans.findById(id).orElseThrow(() -> Errors.notFound("Plan")); }

    private LocationResponse mapLocation(Location v) {
        return new LocationResponse(v.getId(), v.getName(), v.getCity(), v.getAddress(), v.getOpeningTime(), v.getClosingTime());
    }
    private EquipmentResponse mapEquipment(Equipment v) { return new EquipmentResponse(v.getId(), v.getName()); }
    private ServiceResponse mapService(AdditionalService v) { return new ServiceResponse(v.getId(), v.getName(), v.getUnitPrice()); }
    private PlanResponse mapPlan(MembershipPlan v) {
        return new PlanResponse(v.getId(), v.getName(), v.getIncludedHoursPerMonth(), v.getOverageDiscount(), v.getMaxActiveBookings());
    }
    private RoomResponse mapRoom(Room v) {
        return new RoomResponse(v.getId(), v.getName(), v.getType(), v.getCapacity(), v.getHourlyRate(), v.isActive(),
            v.getLocation().getId(), v.getLocation().getName(), v.getEquipment().stream().map(this::mapEquipment).toList());
    }
}
