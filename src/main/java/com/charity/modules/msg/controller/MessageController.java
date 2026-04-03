package com.charity.modules.msg.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charity.common.Result;
import com.charity.modules.msg.entity.SysMessage;
import com.charity.modules.msg.service.MessageService;
import com.charity.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 消息控制器
 */
@Tag(name = "站内信消息管理", description = "消息的查询、已读与删除")
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Operation(summary = "分页获取我的消息")
    @GetMapping
    public Result<IPage<SysMessage>> findMyMessages(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        Long userId = SecurityUtils.getUserId();
        Page<SysMessage> page = new Page<>(current, size);
        return Result.success(messageService.page(page, new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .orderByDesc(SysMessage::getCreateTime)));
    }

    @Operation(summary = "标记消息为已读")
    @PutMapping("/{id}/read")
    public Result<Void> readMessage(@PathVariable Long id) {
        SysMessage msg = messageService.getById(id);
        if (msg != null && msg.getUserId().equals(SecurityUtils.getUserId())) {
            msg.setStatus(1);
            messageService.updateById(msg);
        }
        return Result.success();
    }
}
