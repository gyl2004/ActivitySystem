package com.charity.modules.map.service;

import com.charity.modules.activity.entity.Activity;

import java.math.BigDecimal;
import java.util.List;

/**
 * 地图服务接口
 */
public interface MapService {
    
    /**
     * 根据地理位置搜索附近活动
     * @param longitude 经度
     * @param latitude 纬度
     * @param radius 半径 (km)
     * @return 活动列表
     */
    List<Activity> searchNearby(BigDecimal longitude, BigDecimal latitude, double radius);
    
    /**
     * 地理编码: 地址转经纬度
     * @param address 地址
     * @return 经纬度数组 [lon, lat]
     */
    BigDecimal[] geocoding(String address);
    
    /**
     * 逆地理编码: 经纬度转地址
     * @param longitude 经度
     * @param latitude 纬度
     * @return 地址名称
     */
    String reverseGeocoding(BigDecimal longitude, BigDecimal latitude);
    
    /**
     * 校验是否在围栏内
     * @param activityId 活动ID
     * @param userLon 用户经度
     * @param userLat 用户纬度
     * @param radiusCheckin 签到半径 (米)
     * @return 是否在围栏内
     */
    boolean checkInFence(Long activityId, BigDecimal userLon, BigDecimal userLat, double radiusCheckin);
}
