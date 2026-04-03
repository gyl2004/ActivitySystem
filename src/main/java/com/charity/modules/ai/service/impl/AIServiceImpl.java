package com.charity.modules.ai.service.impl;

import cn.hutool.dfa.WordTree;
import com.charity.modules.ai.service.AIService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * AI 服务实现类 (示例实现，实际需集成通义千问等模型)
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    private final WordTree wordTree = new WordTree();

    @PostConstruct
    public void init() {
        log.info("开始加载敏感词库...");
        try {
            ClassPathResource resource = new ClassPathResource("sensitive_words.txt");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        wordTree.addWord(line.trim());
                    }
                }
            }
            log.info("敏感词库加载完成");
        } catch (Exception e) {
            log.error("加载敏感词库失败", e);
        }
    }

    @Override
    public String analyzeSentiment(String text) {
        log.info("开始对文本进行情感分析: {}", text);

        if (text.contains("好") || text.contains("棒") || text.contains("赞")) {
            return "positive";
        } else if (text.contains("差") || text.contains("烂") || text.contains("难")) {
            return "negative";
        }
        return "neutral";
    }

    @Override
    public String filterSensitiveWords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        log.info("开始进行敏感词过滤: {}", text);
        // 使用 Hutool 的 WordTree 进行过滤
        return wordTree.matchAll(text).stream()
                .reduce(text, (t, word) -> t.replace(word, "***"), (t1, t2) -> t1);
    }
}
