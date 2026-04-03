package com.charity.modules.activity.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.common.AppException;
import com.charity.modules.activity.doc.ActivityDoc;
import com.charity.modules.activity.dto.ActivityDTO;
import com.charity.modules.activity.dto.ActivityQueryDTO;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.mapper.ActivityMapper;
import com.charity.modules.activity.service.ActivityService;
import com.charity.modules.activity.repository.ActivityRepository;
import com.charity.modules.sys.service.SysConfigService;
import com.charity.websocket.NotificationServer;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.GeoDistanceOrder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.domain.Sort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动服务实现类
 */
@Slf4j
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    @Autowired
    private SysConfigService configService;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public IPage<Activity> findPage(Page<Activity> page, ActivityQueryDTO queryDTO) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            wrapper.like(StringUtils.hasText(queryDTO.getTitle()), Activity::getTitle, queryDTO.getTitle())
                    .eq(queryDTO.getCategoryId() != null, Activity::getCategoryId, queryDTO.getCategoryId())
                    .eq(queryDTO.getStatus() != null, Activity::getStatus, queryDTO.getStatus())
                    .ge(queryDTO.getStartTimeBegin() != null, Activity::getStartTime, queryDTO.getStartTimeBegin())
                    .le(queryDTO.getStartTimeEnd() != null, Activity::getStartTime, queryDTO.getStartTimeEnd())
                    .eq(queryDTO.getCreateUserId() != null, Activity::getCreateUserId, queryDTO.getCreateUserId());
        }
        wrapper.orderByDesc(Activity::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Cacheable(value = "activity", key = "#id", unless = "#result == null")
    public Activity getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    @CacheEvict(value = "activity", key = "#entity.id")
    public boolean updateById(Activity entity) {
        return super.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createActivity(ActivityDTO activityDTO, Long userId) {
        Activity activity = BeanUtil.copyProperties(activityDTO, Activity.class);
        activity.setCreateUserId(userId);
        
        // 检查系统配置是否自动发布
        String autoPublish = configService.getValueByKey("activity.auto_publish");
        if ("true".equalsIgnoreCase(autoPublish)) {
            activity.setStatus(2); // 已发布
        } else {
            activity.setStatus(0); // 默认为草稿
        }
        
        activity.setRegisteredCount(0);
        activity.setViewCount(0);
        activity.setShareCount(0);
        this.save(activity);

        if (activity.getStatus() == 2) {
            syncToEs(activity);
            NotificationServer.sendInfo(JSONUtil.createObj()
                    .set("type", "new_activity")
                    .set("activityId", activity.getId())
                    .set("title", activity.getTitle())
                    .toString());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "activity", key = "#id")
    public void updateActivity(Long id, ActivityDTO activityDTO) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        if (activity.getStatus() > 1) {
            throw new AppException("已发布的活动不能直接编辑");
        }
        BeanUtil.copyProperties(activityDTO, activity);
        this.updateById(activity);
        if (activity.getStatus() == 2) {
            syncToEs(activity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "activity", key = "#id")
    public void publishActivity(Long id) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        if (activity.getStatus() != 0 && activity.getStatus() != 1) {
            throw new AppException("当前状态不允许发布");
        }
        activity.setStatus(2); // 已发布
        this.updateById(activity);
        syncToEs(activity);

        NotificationServer.sendInfo(JSONUtil.createObj()
                .set("type", "new_activity")
                .set("activityId", activity.getId())
                .set("title", activity.getTitle())
                .toString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "activity", key = "#id")
    public void cancelActivity(Long id) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        activity.setStatus(5); // 已取消
        this.updateById(activity);
        deleteFromEs(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "activity", key = "#id")
    public void revertToDraft(Long id) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        // 允许从“已发布(2)”或“已取消(5)”退回到“草稿(0)”
        activity.setStatus(0);
        this.updateById(activity);
        deleteFromEs(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "activity", key = "#id")
    public boolean removeById(Serializable id) {
        boolean result = super.removeById(id);
        if (result) {
            deleteFromEs((Long) id);
        }
        return result;
    }

    @Override
    public IPage<Activity> search(Page<Activity> page, String keyword) {
        List<ActivityDoc> docs;
        try {
            docs = activityRepository.findByTitleOrSummaryOrContent(keyword, keyword, keyword);
        } catch (Exception e) {
            log.error("ES 搜索异常: {}", e.getMessage());
            return new Page<>(); // 降级处理，返回空数据
        }
        List<Long> ids = docs.stream().map(doc -> Long.valueOf(doc.getId())).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new Page<>();
        }
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Activity::getId, ids)
                .eq(Activity::getStatus, 2) // 只搜索已发布的
                .orderByDesc(Activity::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copyActivity(Long id, Long userId) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        Activity newActivity = BeanUtil.copyProperties(activity, Activity.class, "id", "createTime", "updateTime", "registeredCount", "viewCount", "shareCount");
        newActivity.setTitle(newActivity.getTitle() + " (副本)");
        newActivity.setStatus(0); // 默认草稿
        newActivity.setCreateUserId(userId);
        this.save(newActivity);
    }

    @Override
    public void exportActivities(HttpServletResponse response, ActivityQueryDTO queryDTO) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            wrapper.like(StringUtils.hasText(queryDTO.getTitle()), Activity::getTitle, queryDTO.getTitle())
                    .eq(queryDTO.getCategoryId() != null, Activity::getCategoryId, queryDTO.getCategoryId())
                    .eq(queryDTO.getStatus() != null, Activity::getStatus, queryDTO.getStatus());
        }
        List<Activity> list = this.list(wrapper);

        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("id", "活动ID");
        writer.addHeaderAlias("title", "标题");
        writer.addHeaderAlias("summary", "摘要");
        writer.addHeaderAlias("locationName", "地点名称");
        writer.addHeaderAlias("address", "详细地址");
        writer.addHeaderAlias("startTime", "开始时间");
        writer.addHeaderAlias("endTime", "结束时间");
        writer.addHeaderAlias("status", "状态");
        writer.addHeaderAlias("registeredCount", "已报名人数");
        writer.addHeaderAlias("points", "积分");
        writer.addHeaderAlias("volunteerDuration", "志愿时长");
        writer.setOnlyAlias(true);

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<java.util.Map<String, Object>> exportList = list.stream().map(activity -> {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", activity.getId());
            map.put("title", activity.getTitle());
            map.put("summary", activity.getSummary());
            map.put("locationName", activity.getLocationName());
            map.put("address", activity.getAddress());
            map.put("startTime", activity.getStartTime() != null ? activity.getStartTime().format(formatter) : null);
            map.put("endTime", activity.getEndTime() != null ? activity.getEndTime().format(formatter) : null);
            map.put("status", activity.getStatus());
            map.put("registeredCount", activity.getRegisteredCount());
            map.put("points", activity.getPoints());
            map.put("volunteerDuration", activity.getVolunteerDuration());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        writer.write(exportList, true);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        String fileName = URLEncoder.encode("活动数据", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
            writer.flush(out, true);
        } catch (IOException e) {
            throw new AppException("导出失败");
        } finally {
            writer.close();
        }
    }

    @Override
    public List<Activity> findNearby(Double longitude, Double latitude, Double distance) {
        GeoPoint point = new GeoPoint(latitude, longitude);
        
        // 构造 ES 地理位置查询
        Criteria criteria = new Criteria("location").within(point, distance + "km");
        Query query = new CriteriaQuery(criteria);
        
        // 按距离排序
        query.addSort(Sort.by(new GeoDistanceOrder("location", point)));
        
        SearchHits<ActivityDoc> hits;
        try {
            hits = elasticsearchOperations.search(query, ActivityDoc.class);
        } catch (Exception e) {
            log.error("ES 附近搜索异常: {}", e.getMessage());
            return List.of(); // 降级处理，返回空数据
        }
        
        List<Long> ids = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(doc -> Long.valueOf(doc.getId()))
                .collect(Collectors.toList());
        
        if (ids.isEmpty()) {
            return List.of();
        }
        
        // 从数据库中获取详细信息并保持顺序
        return this.list(new LambdaQueryWrapper<Activity>()
                .in(Activity::getId, ids)
                .eq(Activity::getStatus, 2)
                .last("ORDER BY FIELD(id, " + ids.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")"));
    }

    private void syncToEs(Activity activity) {
        try {
            ActivityDoc doc = BeanUtil.copyProperties(activity, ActivityDoc.class, "id");
            doc.setId(activity.getId().toString());
            if (activity.getLongitude() != null && activity.getLatitude() != null) {
                doc.setLocation(new GeoPoint(activity.getLatitude().doubleValue(), activity.getLongitude().doubleValue()));
            }
            activityRepository.save(doc);
        } catch (Exception e) {
            log.error("同步活动到 ES 失败: {}", e.getMessage());
        }
    }

    private void deleteFromEs(Long id) {
        try {
            activityRepository.deleteById(id.toString());
        } catch (Exception e) {
            log.error("从 ES 删除活动失败: {}", e.getMessage());
        }
    }
}
