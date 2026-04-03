package com.charity.modules.recommendation.aspect;

import com.charity.modules.recommendation.service.UserBehaviorService;
import com.charity.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 行为追踪切面
 */
@Slf4j
@Aspect
@Component
public class ActivityBehaviorAspect {

    @Autowired
    private UserBehaviorService userBehaviorService;

    // 1. 浏览切点 (ActivityController.getById)
    @Pointcut("execution(* com.charity.modules.activity.controller.ActivityController.getById(..))")
    public void viewPointcut() {}

    // 2. 报名切点 (RegistrationController.register)
    @Pointcut("execution(* com.charity.modules.registration.controller.RegistrationController.register(..))")
    public void registerPointcut() {}

    // 3. 签到切点 (CheckinController.checkin)
    @Pointcut("execution(* com.charity.modules.checkin.controller.CheckinController.checkin(..))")
    public void checkinPointcut() {}

    // 4. 评价切点 (ReviewController.submit)
    @Pointcut("execution(* com.charity.modules.review.controller.ReviewController.submit(..))")
    public void reviewPointcut() {}

    @AfterReturning(pointcut = "viewPointcut()", returning = "result")
    public void afterView(JoinPoint joinPoint, Object result) {
        log.info("触发浏览行为追踪");
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long activityId) {
            Long userId = SecurityUtils.getUserId();
            if (userId != null) {
                userBehaviorService.logBehavior(userId, activityId, "view", new BigDecimal("1.0"));
            }
        }
    }

    @AfterReturning(pointcut = "registerPointcut()", returning = "result")
    public void afterRegister(JoinPoint joinPoint, Object result) {
        log.info("触发报名行为追踪");
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof com.charity.modules.registration.dto.RegistrationDTO registrationDTO) {
            Long userId = SecurityUtils.getUserId();
            if (userId != null) {
                userBehaviorService.logBehavior(userId, registrationDTO.getActivityId(), "register", new BigDecimal("5.0"));
            }
        }
    }

    @AfterReturning(pointcut = "checkinPointcut()", returning = "result")
    public void afterCheckin(JoinPoint joinPoint, Object result) {
        log.info("触发签到行为追踪");
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof com.charity.modules.checkin.dto.CheckinDTO checkinDTO) {
            Long userId = SecurityUtils.getUserId();
            if (userId != null) {
                userBehaviorService.logBehavior(userId, checkinDTO.getActivityId(), "checkin", new BigDecimal("10.0"));
            }
        }
    }

    @AfterReturning(pointcut = "reviewPointcut()", returning = "result")
    public void afterReview(JoinPoint joinPoint, Object result) {
        log.info("触发评价行为追踪");
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof com.charity.modules.review.dto.ReviewSubmitDTO submitDTO) {
            Long userId = SecurityUtils.getUserId();
            if (userId != null) {
                userBehaviorService.logBehavior(userId, submitDTO.getActivityId(), "review", new BigDecimal("8.0"));
            }
        }
    }
}
