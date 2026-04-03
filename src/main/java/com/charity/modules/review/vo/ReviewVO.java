package com.charity.modules.review.vo;

import com.charity.modules.review.entity.ActivityReview;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewVO extends ActivityReview {
    private String nickname;
    private String avatar;
    private String activityTitle;
}
