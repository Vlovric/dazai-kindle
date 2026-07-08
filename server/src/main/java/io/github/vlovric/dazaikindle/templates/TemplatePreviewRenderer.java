package io.github.vlovric.dazaikindle.templates;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import freemarker.cache.StringTemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.github.vlovric.dazaikindle.template.TemplateClipping;
import io.github.vlovric.dazaikindle.template.TemplateGroup;
import io.github.vlovric.dazaikindle.template.TemplateHeading;

/**
 * Renders arbitrary (not-yet-saved-to-disk) template content against a fixed
 * set of hardcoded example entries, for the Template library's live preview
 * (FR07_01-HP_02). Exposes both "groups" (output templates) and "headings"
 * (heading templates) in the same context - the preview endpoint isn't told
 * which kind of template it's rendering, so both view-models are always
 * available; a template only ever references the one it cares about.
 */
@Component
public class TemplatePreviewRenderer {

    public String render(String content) {
        try {
            Configuration cfg = buildConfiguration();
            StringTemplateLoader loader = new StringTemplateLoader();
            loader.putTemplate("preview", content);
            cfg.setTemplateLoader(loader);

            Template template = cfg.getTemplate("preview");
            StringWriter writer = new StringWriter();
            template.process(exampleContext(), writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private Configuration buildConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        cfg.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        cfg.setAPIBuiltinEnabled(false);
        cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_33).build());
        return cfg;
    }

    private Map<String, Object> exampleContext() {
        TemplateHeading heading = new TemplateHeading("Chapter 1: The Beginning", 1, 120, 0, "chapter1.xhtml", "heading-1");
        TemplateClipping highlight = new TemplateClipping(
            "Example Book", "Example Author", "Highlight", 120, "120-124", 12, "2026-01-01", "This is an example highlighted passage."
        );
        TemplateClipping note = new TemplateClipping(
            "Example Book", "Example Author", "Note", 121, "121", 12, "2026-01-01", "This is an example note."
        );
        TemplateGroup group = new TemplateGroup(heading, List.of(highlight, note));

        Map<String, Object> context = new HashMap<>();
        context.put("title", "Example Book");
        context.put("groups", List.of(group));
        context.put("headings", List.of(heading));
        return context;
    }
}
