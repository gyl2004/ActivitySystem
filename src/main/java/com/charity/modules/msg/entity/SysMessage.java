package com.charity.modules.msg.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内信消息表
 */
@Data
@TableName("sys_message")
public class SysMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId; // 接收人ID, 0 为系统广播
    private String title;
    private String content;
    private Integer type; // 1-系统通知, 2-报名结果, 3-签到提醒, 4-活动评价
    private Integer status; // 0-未读, 1-已读
    private LocalDateTime createTime;
}
