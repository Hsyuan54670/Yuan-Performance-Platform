package com.yuan.analysis.report;

import com.yuan.analysis.vo.AnalysisResultVO;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

@Service
public class AnalysisReportHtmlRenderer {

    private final TemplateEngine templateEngine;

    public AnalysisReportHtmlRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(AnalysisResultVO report) {
        Context context = new Context(Locale.SIMPLIFIED_CHINESE);
        context.setVariable("report", report);
        context.setVariable("bottleneckCount", report.getBottlenecks().size());
        context.setVariable("suggestionCount", report.getSuggestions().size());
        return templateEngine.process("analysis-report", context);
    }
}
