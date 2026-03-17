package com.yuan.analysis.llm;

import com.yuan.analysis.ai.model.AiAnalysisInput;
import com.yuan.analysis.ai.model.AiAnalysisResult;

public interface LlmClient {

    AiAnalysisResult analyze(String prompt, AiAnalysisInput input);
}
