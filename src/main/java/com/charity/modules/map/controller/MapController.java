package com.charity.modules.map.controller;

import com.charity.common.Result;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.map.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 地图控制器
 */
@Tag(name = "活动地图管理", description = "附近活动搜索、地理编码、围栏校验")
@RestController
@RequestMapping("/api/map")
public class MapController {

    @Autowired
    private MapService mapService;

    @Operation(summary = "搜索附近活动")
    @GetMapping("/nearby")
    public Result<List<Activity>> searchNearby(
            @RequestParam BigDecimal longitude,
            @RequestParam BigDecimal latitude,
            @RequestParam(defaultValue = "10.0") Double radius) {
        return Result.success(mapService.searchNearby(longitude, latitude, radius));
    }

    @Operation(summary = "地理编码")
    @GetMapping("/geocoding")
    public Result<BigDecimal[]> geocoding(@RequestParam String address) {
        return Result.success(mapService.geocoding(address));
    }

    @Operation(summary = "逆地理编码")
    @GetMapping("/reverse-geocoding")
    public Result<String> reverseGeocoding(@RequestParam BigDecimal longitude, @RequestParam BigDecimal latitude) {
        return Result.success(mapService.reverseGeocoding(longitude, latitude));
    }
}
