package com.charity.modules.review.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.common.AppException;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.service.ActivityService;
import com.charity.modules.ai.service.AIService;
import com.charity.modules.checkin.entity.ActivityCheckin;
import com.charity.modules.checkin.service.CheckinService;
import com.charity.modules.review.dto.ReviewAuditDTO;
import com.charity.modules.review.dto.ReviewReplyDTO;
import com.charity.modules.review.dto.ReviewSubmitDTO;
import com.charity.modules.review.entity.ActivityReview;
import com.charity.modules.review.entity.ActivityReviewLike;
import com.charity.modules.review.entity.ActivityReviewReply;
import com.charity.modules.review.mapper.ActivityReviewLikeMapper;
import com.charity.modules.review.mapper.ActivityReviewMapper;
import com.charity.modules.review.mapper.ActivityReviewReplyMapper;
import com.charity.modules.review.service.ReviewService;
import com.charity.modules.review.vo.ReviewVO;
import com.charity.modules.sys.entity.SysUser;
import com.charity.modules.sys.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评价服务实现类
 */
@Service
public class ReviewServiceImpl extends ServiceImpl<ActivityReviewMapper, ActivityReview> implements ReviewService {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private SysUserService userService;

    @Autowired
    private ActivityReviewReplyMapper replyMapper;

    @Autowired
    private ActivityReviewLikeMapper likeMapper;

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private AIService aiService;

    @Override
    public Map<String, Object> getStatistics() {
        List<ActivityReview> reviews = this.list();
        Map<String, Object> stats = new HashMap<>();
        
        // 状态统计
        stats.put("status", reviews.stream()
                .collect(Collectors.groupingBy(ActivityReview::getStatus, Collectors.counting())));
        
        // 情感统计
        stats.put("sentiment", reviews.stream()
                .collect(Collectors.groupingBy(r -> r.getSentiment() == null ? "neutral" : r.getSentiment(), Collectors.counting())));
        
        stats.put("total", reviews.size());
        
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReview(ReviewSubmitDTO submitDTO, Long userId) {
        // 1. 检查是否签到过 (只有签到过的用户才能评价)
        ActivityCheckin checkin = checkinService.getOne(new LambdaQueryWrapper<ActivityCheckin>()
                .eq(ActivityCheckin::getActivityId, submitDTO.getActivityId())
                .eq(ActivityCheckin::getUserId, userId));
        if (checkin == null) {
            throw new AppException("未签到用户无法进行评价");
        }

        // 2. AI 情感分析与敏感词过滤
        String filteredContent = aiService.filterSensitiveWords(submitDTO.getContent());
        String sentiment = aiService.analyzeSentiment(filteredContent);

        // 3. 保存评价
        ActivityReview review = new ActivityReview();
        review.setActivityId(submitDTO.getActivityId());
        review.setUserId(userId);
        review.setRating(submitDTO.getRating());
        review.setContent(filteredContent);
        review.setImages(JSONUtil.toJsonStr(submitDTO.getImages()));
        review.setTags(JSONUtil.toJsonStr(submitDTO.getTags()));
        review.setLikeCount(0);
        review.setReplyCount(0);
        review.setStatus(1);
        review.setSentiment(sentiment);
        review.setCreateTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        this.save(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyReview(ReviewReplyDTO replyDTO, Long userId) {
        ActivityReview review = this.getById(replyDTO.getReviewId());
        if (review == null) {
            throw new AppException("评价不存在");
        }

        // 敏感词过滤
        String filteredContent = aiService.filterSensitiveWords(replyDTO.getContent());

        ActivityReviewReply reply = new ActivityReviewReply();
        reply.setReviewId(replyDTO.getReviewId());
        reply.setUserId(userId);
        reply.setContent(filteredContent);
        reply.setParentId(replyDTO.getParentId() != null ? replyDTO.getParentId() : 0L);
        reply.setCreateTime(LocalDateTime.now());
        replyMapper.insert(reply);

        // 更新回复数
        review.setReplyCount(review.getReplyCount() + 1);
        this.updateById(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeReview(Long reviewId, Long userId, Integer type) {
        ActivityReview review = this.getById(reviewId);
        if (review == null) {
            throw new AppException("评价不存在");
        }

        ActivityReviewLike like = likeMapper.selectOne(new LambdaQueryWrapper<ActivityReviewLike>()
                .eq(ActivityReviewLike::getReviewId, reviewId)
                .eq(ActivityReviewLike::getUserId, userId));

        if (like != null) {
            if (like.getType().equals(type)) {
                // 取消点赞/点踩
                likeMapper.deleteById(like.getId());
                if (type == 1) {
                    review.setLikeCount(review.getLikeCount() - 1);
                }
            } else {
                // 切换点赞/点踩
                like.setType(type);
                likeMapper.updateById(like);
                if (type == 1) {
                    review.setLikeCount(review.getLikeCount() + 1);
                } else {
                    review.setLikeCount(review.getLikeCount() - 1);
                }
            }
        } else {
            // 新增点赞/点踩
            like = new ActivityReviewLike();
            like.setReviewId(reviewId);
            like.setUserId(userId);
            like.setType(type);
            like.setCreateTime(LocalDateTime.now());
            likeMapper.insert(like);
            if (type == 1) {
                review.setLikeCount(review.getLikeCount() + 1);
            }
        }
        this.updateById(review);
    }

    @Override
    public void auditReview(Long reviewId, ReviewAuditDTO auditDTO) {
        ActivityReview review = this.getById(reviewId);
        if (review == null) {
            throw new AppException("评价不存在");
        }
        review.setStatus(auditDTO.getStatus());
        this.updateById(review);
    }

    @Override
    public void revokeReview(Long reviewId) {
        ActivityReview review = this.getById(reviewId);
        if (review == null) {
            throw new AppException("评价不存在");
        }
        review.setStatus(2);
        review.setUpdateTime(LocalDateTime.now());
        this.updateById(review);
    }

    @Override
    public IPage<ReviewVO> findPage(Page<ActivityReview> page, Integer status, String sentiment) {
        LambdaQueryWrapper<ActivityReview> queryWrapper = new LambdaQueryWrapper<ActivityReview>()
                .orderByDesc(ActivityReview::getCreateTime);
        if (status != null) {
            queryWrapper.eq(ActivityReview::getStatus, status);
        }
        if (sentiment != null && !sentiment.isEmpty()) {
            queryWrapper.eq(ActivityReview::getSentiment, sentiment);
        }
        
        IPage<ActivityReview> reviewPage = this.page(page, queryWrapper);
        Page<ReviewVO> voPage = new Page<>(reviewPage.getCurrent(), reviewPage.getSize(), reviewPage.getTotal());
        
        List<ReviewVO> voList = reviewPage.getRecords().stream().map(review -> {
            ReviewVO vo = BeanUtil.copyProperties(review, ReviewVO.class);
            
            // 补充用户信息
            SysUser user = userService.getById(review.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            
            // 补充活动信息
            Activity activity = activityService.getById(review.getActivityId());
            if (activity != null) {
                vo.setActivityTitle(activity.getTitle());
            }
            
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public IPage<ReviewVO> findPageByActivityId(Page<ActivityReview> page, Long activityId) {
        IPage<ActivityReview> reviewPage = this.page(page, new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getActivityId, activityId)
                .eq(ActivityReview::getStatus, 1) // 只显示审核通过的
                .orderByDesc(ActivityReview::getCreateTime));

        Page<ReviewVO> voPage = new Page<>(reviewPage.getCurrent(), reviewPage.getSize(), reviewPage.getTotal());
        List<ReviewVO> voList = reviewPage.getRecords().stream().map(review -> {
            ReviewVO vo = BeanUtil.copyProperties(review, ReviewVO.class);
            // 补充用户信息
            SysUser user = userService.getById(review.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public IPage<ActivityReviewReply> findRepliesByReviewId(Page<ActivityReviewReply> page, Long reviewId) {
        return replyMapper.selectPage(page, new LambdaQueryWrapper<ActivityReviewReply>()
                .eq(ActivityReviewReply::getReviewId, reviewId)
                .orderByAsc(ActivityReviewReply::getCreateTime));
    }

    @Override
    public Map<String, Long> getActivityCommentCount(Long activityId) {
        Long reviewCount = this.count(new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getActivityId, activityId)
                .eq(ActivityReview::getStatus, 1));
        Long replyCount = replyMapper.countRepliesByActivityId(activityId);
        if (replyCount == null) {
            replyCount = 0L;
        }
        Map<String, Long> map = new HashMap<>();
        long reviews = reviewCount == null ? 0L : reviewCount;
        map.put("reviews", reviews);
        map.put("replies", replyCount);
        map.put("total", reviews + replyCount);
        return map;
    }
}
