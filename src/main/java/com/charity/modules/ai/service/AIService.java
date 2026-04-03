package com.charity.modules.ai.service;

/**
 * AI 服务接口
 */
public interface AIService {
    

    String analyzeSentiment(String text);
    

    String filterSensitiveWords(String text);
}
