package com.charity.modules.statistics.controller;

import com.charity.common.Result;
import com.charity.modules.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 统计控制器
 */
@Tag(name = "统计分析", description = "活动数据统计与报表")
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @Operation(summary = "获取总体统计数据")
    @GetMapping("/overall")
    public Result<Map<String, Object>> getOverallStats() {
        return Result.success(statisticsService.getOverallStats());
    }

    @Operation(summary = "获取活动详情统计")
    @GetMapping("/activity/{activityId}")
    @PreAuthorize("hasAuthority('statistics:view')")
    public Result<Map<String, Object>> getActivityStats(@PathVariable Long activityId) {
        return Result.success(statisticsService.getActivityStats(activityId));
    }
}
