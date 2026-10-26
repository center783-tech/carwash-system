package com.carwash.controller;

import com.carwash.controller.dto.VehicleRequest;
import com.carwash.controller.dto.VehicleResponse;
import com.carwash.entity.Vehicle;
import com.carwash.response.ApiResponse;
import com.carwash.security.AccessGuard;
import com.carwash.security.UserPrincipal;
import com.carwash.service.VehicleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "車輛", description = "會員的車輛管理")
@RestController
@RequestMapping("/api/members/{memberId}/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ApiResponse<VehicleResponse> add(@PathVariable Long memberId, @Valid @RequestBody VehicleRequest req,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        AccessGuard.requireSelf(memberId, principal);
        Vehicle vehicle = Vehicle.builder()
                .plateNumber(req.getPlateNumber())
                .brand(req.getBrand())
                .model(req.getModel())
                .color(req.getColor())
                .carType(req.getCarType())
                .build();
        return ApiResponse.success("新增車輛成功", VehicleResponse.from(vehicleService.addVehicle(memberId, vehicle)));
    }

    @GetMapping
    public ApiResponse<List<VehicleResponse>> list(@PathVariable Long memberId,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        AccessGuard.requireSelfOrStaff(memberId, principal);
        List<VehicleResponse> result = vehicleService.listByMember(memberId).stream()
                .map(VehicleResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    @PutMapping("/{vehicleId}")
    public ApiResponse<VehicleResponse> update(@PathVariable Long memberId, @PathVariable Long vehicleId,
                                                @Valid @RequestBody VehicleRequest req,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        AccessGuard.requireSelf(memberId, principal);
        Vehicle updates = Vehicle.builder()
                .plateNumber(req.getPlateNumber())
                .brand(req.getBrand())
                .model(req.getModel())
                .color(req.getColor())
                .carType(req.getCarType())
                .build();
        return ApiResponse.success("更新成功", VehicleResponse.from(vehicleService.update(vehicleId, updates)));
    }

    @DeleteMapping("/{vehicleId}")
    public ApiResponse<Void> delete(@PathVariable Long memberId, @PathVariable Long vehicleId,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        AccessGuard.requireSelf(memberId, principal);
        vehicleService.delete(vehicleId);
        return ApiResponse.success("刪除成功", null);
    }
}
