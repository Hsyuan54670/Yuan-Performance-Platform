package com.yuan.analysis.ai.service;

import com.yuan.analysis.model.AnalysisComputationResult;
import com.yuan.analysis.model.AnalysisSnapshot;

public interface AiAnalysisEnhancer {

    AnalysisComputationResult enhance(Long userId, AnalysisSnapshot snapshot, AnalysisComputationResult baseResult);
}
