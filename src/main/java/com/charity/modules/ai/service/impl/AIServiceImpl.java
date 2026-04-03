package com.charity.modules.ai.service.impl;

import com.charity.modules.ai.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 服务实现类 (示例实现，实际需集成通义千问等模型)
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

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
        log.info("开始进行敏感词过滤: {}", text);
        // 简单模拟敏感词库过滤
        String[] sensitiveWords = {"敏感词1", "不当言论"};
        String filteredText = text;
        for (String word : sensitiveWords) {
            filteredText = filteredText.replace(word, "***");
        }
        return filteredText;
    }
}
