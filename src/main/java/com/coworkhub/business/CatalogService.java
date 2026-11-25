package com.coworkhub.business;

import java.util.List;

import com.coworkhub.presentation.ApiDtos.*;
import com.coworkhub.domain.*;

public interface CatalogService {
    List<LocationResponse> locations();
    LocationResponse location(Long id);
    LocationResponse saveLocation(Long id, LocationRequest request);
    void deleteLocation(Long id);
    List<RoomResponse> rooms(Long locationId);
    RoomResponse room(Long id);
    RoomResponse saveRoom(Long id, RoomRequest request);
    void deleteRoom(Long id);
    List<EquipmentResponse> equipment();
    EquipmentResponse equipment(Long id);
    EquipmentResponse saveEquipment(Long id, EquipmentRequest request);
    void deleteEquipment(Long id);
    List<ServiceResponse> services();
    ServiceResponse service(Long id);
    ServiceResponse saveService(Long id, ServiceRequest request);
    void deleteService(Long id);
    List<PlanResponse> plans();
    PlanResponse plan(Long id);
    PlanResponse savePlan(Long id, PlanRequest request);
    void deletePlan(Long id);
}
