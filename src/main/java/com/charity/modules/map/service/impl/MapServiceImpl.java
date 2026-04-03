package com.charity.modules.map.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.service.ActivityService;
import com.charity.modules.map.service.MapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 地图服务实现类 (简版实现，实际可集成高德地图或 Elasticsearch GeoQuery)
 */
@Slf4j
@Service
public class MapServiceImpl implements MapService {

    @Autowired
    private ActivityService activityService;

    @Override
    public List<Activity> searchNearby(BigDecimal longitude, BigDecimal latitude, double radius) {
        log.info("开始搜索附近 {}km 的活动: lon={}, lat={}", radius, longitude, latitude);
        // 简单逻辑：查询出所有已发布的活动，然后在内存中进行距离过滤
        // 生产环境应使用 Elasticsearch 或 MySQL 的空间索引 (Spatial Index)
        List<Activity> activities = activityService.list(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, 2)
                .isNotNull(Activity::getLongitude)
                .isNotNull(Activity::getLatitude));

        return activities.stream()
                .filter(a -> calculateDistance(longitude, latitude, a.getLongitude(), a.getLatitude()) <= radius)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal[] geocoding(String address) {
        log.info("地址转经纬度: {}", address);
        // 调用外部 API (AMap / Baidu)
        // 模拟返回
        return new BigDecimal[]{new BigDecimal("116.40"), new BigDecimal("39.90")};
    }

    @Override
    public String reverseGeocoding(BigDecimal longitude, BigDecimal latitude) {
        log.info("经纬度转地址: {},{}", longitude, latitude);
        // 调用外部 API
        return "北京市东城区某街道";
    }

    @Override
    public boolean checkInFence(Long activityId, BigDecimal userLon, BigDecimal userLat, double radiusCheckin) {
        Activity activity = activityService.getById(activityId);
        if (activity == null || activity.getLongitude() == null || activity.getLatitude() == null) {
            return false;
        }
        double distance = calculateDistance(userLon, userLat, activity.getLongitude(), activity.getLatitude());
        return distance * 1000 <= radiusCheckin; // 公里转米进行校验
    }

    /**
     * 计算两点间距离 (Haversine 公式)
     */
    private double calculateDistance(BigDecimal lon1, BigDecimal lat1, BigDecimal lon2, BigDecimal lat2) {
        double r = 6371; // 地球半径 km
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue())) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }
}
