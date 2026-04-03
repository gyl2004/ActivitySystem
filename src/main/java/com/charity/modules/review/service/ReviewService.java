package com.charity.modules.review.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.charity.modules.review.dto.ReviewAuditDTO;
import com.charity.modules.review.dto.ReviewReplyDTO;
import com.charity.modules.review.dto.ReviewSubmitDTO;
import com.charity.modules.review.entity.ActivityReview;
import com.charity.modules.review.entity.ActivityReviewReply;

import java.util.Map;

import com.charity.modules.review.vo.ReviewVO;

/**
 * 评价服务接口
 */
public interface ReviewService extends IService<ActivityReview> {
    
    /**
     * 获取评价统计 (管理端)
     */
    Map<String, Object> getStatistics();

    /**
     * 提交评价
     */
    void submitReview(ReviewSubmitDTO submitDTO, Long userId);
    
    /**
     * 回复评价
     */
    void replyReview(ReviewReplyDTO replyDTO, Long userId);
    
    /**
     * 点赞评价
     */
    void likeReview(Long reviewId, Long userId, Integer type);
    
    /**
     * 审核评价
     */
    void auditReview(Long reviewId, ReviewAuditDTO auditDTO);

    /**
     * 撤回评价 (管理端)
     */
    void revokeReview(Long reviewId);
    
    /**
     * 获取所有评价 (管理端分页)
     */
    IPage<ReviewVO> findPage(Page<ActivityReview> page, Integer status, String sentiment);

    /**
     * 获取活动评价列表
     */
    IPage<ReviewVO> findPageByActivityId(Page<ActivityReview> page, Long activityId);
    
    /**
     * 获取回复列表
     */
    IPage<ActivityReviewReply> findRepliesByReviewId(Page<ActivityReviewReply> page, Long reviewId);

    /**
     * 获取活动评论数（评价 + 回复，均基于已展示评价）
     */
    Map<String, Long> getActivityCommentCount(Long activityId);
}
