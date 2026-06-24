package io.github.vlovric.dazaikindle;

import freemarker.cache.FileTemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.github.vlovric.dazaikindle.models.Clipping;
import io.github.vlovric.dazaikindle.models.Heading;
import io.github.vlovric.dazaikindle.models.HeadingGroup;
import io.github.vlovric.dazaikindle.template.TemplateClipping;
import io.github.vlovric.dazaikindle.template.TemplateGroup;
import io.github.vlovric.dazaikindle.template.TemplateHeading;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders a list of HeadingGroups to an output stream (console or file)
 * by applying them to a user-provided FreeMarker template file.
 */
public class TemplateRenderer {

    private final Path templatePath;

    /**
     * @param templatePath the location of the user-provided template file (e.g. .ftl)
     */
    public TemplateRenderer(Path templatePath) {
        this.templatePath = templatePath;
    }

    /**
     * Loads the template from disk and injects a minimal, explicit view-model.
     *
     * Exposed context keys:
     * - title: String
     * - groups: List<TemplateGroup>
     *
     * @param groups the populated groupings
     * @param title  the book title string
     * @param writer the destination writer to stream the output into
     * @throws IOException if reading the template or writing the output fails
     */
    public void render(List<HeadingGroup> groups, String title, Writer writer) throws IOException {
        try {
            Configuration cfg = buildConfiguration();
            Template template = cfg.getTemplate(templatePath.getFileName().toString());

            Map<String, Object> context = new HashMap<>();
            context.put("title", title);
            context.put("groups", toTemplateGroups(groups));

            template.process(context, writer);
            writer.flush();
        } catch (TemplateException e) {
            throw new IOException("Template rendering failed: " + e.getMessage(), e);
        }
    }

    /**
     * Renders a headings-only view (no clippings).
     * Exposes: title (String), headings (List<TemplateHeading>)
     */
    public void renderHeadings(List<Heading> headings, String title, Writer writer) throws IOException {
        try {
            Configuration cfg = buildConfiguration();
            Template template = cfg.getTemplate(templatePath.getFileName().toString());

            Map<String, Object> context = new HashMap<>();
            context.put("title", title);
            context.put("headings", toTemplateHeadings(headings));

            template.process(context, writer);
            writer.flush();
        } catch (TemplateException e) {
            throw new IOException("Template rendering failed: " + e.getMessage(), e);
        }
    }

    /**
     * Overloaded convenience method to write directly to standard out if no specific Writer is requested.
     */
    public void renderToStdout(List<HeadingGroup> groups, String title) throws IOException {
        render(groups, title, new PrintWriter(System.out));
    }

    private Configuration buildConfiguration() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        cfg.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        cfg.setAPIBuiltinEnabled(false);
        cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_33).build());
        cfg.setTemplateLoader(new FileTemplateLoader(templatePath.toAbsolutePath().getParent().toFile()));
        return cfg;
    }

    private static List<TemplateHeading> toTemplateHeadings(List<Heading> headings) {
        return headings.stream()
            .map(h -> new TemplateHeading(h.title(), h.level(), h.location(), h.charOffset(), h.file(), h.anchor()))
            .toList();
    }

    private static List<TemplateGroup> toTemplateGroups(List<HeadingGroup> groups) {
        return groups.stream().map(g -> {
            Heading h = g.heading();
            TemplateHeading th = new TemplateHeading(
                    h.title(),
                    h.level(),
                    h.location(),
                    h.charOffset(),
                    h.file(),
                    h.anchor()
            );

            List<TemplateClipping> clippings = g.clippings().stream()
                    .map(TemplateRenderer::toTemplateClipping)
                    .collect(Collectors.toList());

            return new TemplateGroup(th, clippings);
        }).collect(Collectors.toList());
    }

    private static TemplateClipping toTemplateClipping(Clipping c) {
        return new TemplateClipping(
                c.bookTitle(),
                c.author(),
                c.type(),
                c.location(),
                c.rawLocation(),
                c.page(),
                c.date(),
                c.content()
        );
    }
}