package com.charity.modules.review.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charity.common.Result;
import com.charity.modules.review.dto.ReviewAuditDTO;
import com.charity.modules.review.dto.ReviewReplyDTO;
import com.charity.modules.review.dto.ReviewSubmitDTO;
import com.charity.modules.review.entity.ActivityReview;
import com.charity.modules.review.entity.ActivityReviewReply;
import com.charity.modules.review.service.ReviewService;
import com.charity.modules.review.vo.ReviewVO;
import com.charity.util.SecurityUtils;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 评价控制器
 */
@Tag(name = "评价管理", description = "活动评价与审核管理")
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Operation(summary = "获取评价统计 (管理端)")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('review:audit')")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(reviewService.getStatistics());
    }

    @Operation(summary = "提交评价")
    @PostMapping
    public Result<Void> submitReview(@Valid @RequestBody ReviewSubmitDTO submitDTO) {
        Long userId = SecurityUtils.getUserId();
        reviewService.submitReview(submitDTO, userId);
        return Result.success();
    }

    @Operation(summary = "回复评价")
    @PostMapping("/reply")
    public Result<Void> reply(@Valid @RequestBody ReviewReplyDTO replyDTO) {
        Long userId = SecurityUtils.getUserId();
        reviewService.replyReview(replyDTO, userId);
        return Result.success();
    }

    @Operation(summary = "点赞评价")
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        reviewService.likeReview(id, userId, 1);
        return Result.success();
    }

    @Operation(summary = "点踩评价")
    @PostMapping("/{id}/dislike")
    public Result<Void> dislike(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        reviewService.likeReview(id, userId, 2);
        return Result.success();
    }

    @Operation(summary = "获取活动评价列表")
    @GetMapping("/activity/{activityId}")
    public Result<IPage<ReviewVO>> findPageByActivityId(
            @PathVariable Long activityId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        Page<ActivityReview> page = new Page<>(current, size);
        return Result.success(reviewService.findPageByActivityId(page, activityId));
    }

    @Operation(summary = "获取活动评论总数(评价+回复)")
    @GetMapping("/activity/{activityId}/count")
    public Result<Map<String, Long>> getActivityCommentCount(@PathVariable Long activityId) {
        return Result.success(reviewService.getActivityCommentCount(activityId));
    }

    @Operation(summary = "获取评价回复列表")
    @GetMapping("/{reviewId}/replies")
    public Result<IPage<ActivityReviewReply>> findRepliesByReviewId(
            @PathVariable Long reviewId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        Page<ActivityReviewReply> page = new Page<>(current, size);
        return Result.success(reviewService.findRepliesByReviewId(page, reviewId));
    }

    @Operation(summary = "分页查询所有评价 (管理端)")
    @GetMapping
    @PreAuthorize("hasAuthority('review:audit')")
    public Result<IPage<ReviewVO>> findPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String sentiment) {
        Page<ActivityReview> page = new Page<>(current, size);
        return Result.success(reviewService.findPage(page, status, sentiment));
    }

    @Operation(summary = "审核评价")
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('review:audit')")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody ReviewAuditDTO auditDTO) {
        reviewService.auditReview(id, auditDTO);
        return Result.success();
    }

    @Operation(summary = "撤回评价 (管理端)")
    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('review:audit')")
    public Result<Void> revoke(@PathVariable Long id) {
        reviewService.revokeReview(id);
        return Result.success();
    }
}
