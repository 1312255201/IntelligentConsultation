package cn.gugufish.service;

import cn.gugufish.ai.AiTriageAdvice;
import cn.gugufish.ai.AiTriageContext;

public interface AiTriageService {
    boolean isAvailable();
    AiTriageAdvice generateInitialAdvice(AiTriageContext context);
    AiTriageAdvice continueConversation(AiTriageContext context);
}
