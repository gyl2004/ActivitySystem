package com.charity.modules.sys.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.charity.common.Result;
import com.charity.common.AppException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@Tag(name = "文件上传", description = "统一文件上传接口")
public class FileController {

    @Value("${file.upload-path:uploads/}")
    private String uploadPath;

    @Operation(summary = "上传图片")
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new AppException("上传文件不能为空");
        }

        // 检查类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException("只能上传图片文件");
        }

        try {
            // 获取文件扩展名
            String originalFilename = file.getOriginalFilename();
            String extName = FileUtil.extName(originalFilename);
            if (cn.hutool.core.util.StrUtil.isBlank(extName)) {
                extName = "png"; // 默认扩展名
            }

            // 生成新文件名
            String newFileName = IdUtil.fastSimpleUUID() + "." + extName;

            // 确保目录存在
            File directory = new File(uploadPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 保存文件
            File dest = new File(directory.getAbsolutePath() + File.separator + newFileName);
            file.transferTo(dest);

            // 构建可访问的URL
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String fileUrl = baseUrl + "/uploads/" + newFileName;

            return Result.success(fileUrl);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new AppException("文件上传失败: " + e.getMessage());
        }
    }
}
