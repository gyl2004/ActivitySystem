package com.charity.modules.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.charity.modules.review.entity.ActivityReviewReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivityReviewReplyMapper extends BaseMapper<ActivityReviewReply> {

    @Select("SELECT COUNT(1) " +
            "FROM activity_review_reply r " +
            "INNER JOIN activity_review v ON r.review_id = v.id " +
            "WHERE v.activity_id = #{activityId} AND v.status = 1")
    Long countRepliesByActivityId(Long activityId);
}
