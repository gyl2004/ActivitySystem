package com.charity.modules.recommendation.controller;

import com.charity.common.Result;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.recommendation.service.RecommendationService;
import com.charity.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "推荐管理", description = "个性化推荐与相似活动推荐")
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Operation(summary = "获取用户个性化推荐")
    @GetMapping("/user")
    public Result<List<Activity>> getPersonalized(@RequestParam(defaultValue = "4") int limit) {
        Long userId = SecurityUtils.getUserId();
        // 如果未登录，则返回热门活动作为推荐
        if (userId == null) {
            return Result.success(recommendationService.recommendPopular(limit));
        }
        return Result.success(recommendationService.recommendForUser(userId, limit));
    }

    @Operation(summary = "获取相似活动推荐")
    @GetMapping("/similar/{activityId}")
    public Result<List<Activity>> getSimilar(@PathVariable Long activityId, @RequestParam(defaultValue = "3") int limit) {
        return Result.success(recommendationService.recommendSimilar(activityId, limit));
    }

    @Operation(summary = "获取热门活动")
    @GetMapping("/popular")
    public Result<List<Activity>> getPopular(@RequestParam(defaultValue = "4") int limit) {
        return Result.success(recommendationService.recommendPopular(limit));
    }
}
