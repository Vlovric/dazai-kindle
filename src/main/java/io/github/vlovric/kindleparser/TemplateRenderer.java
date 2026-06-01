package io.github.vlovric.kindleparser;

import freemarker.cache.FileTemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.github.vlovric.kindleparser.models.Clipping;
import io.github.vlovric.kindleparser.models.Heading;
import io.github.vlovric.kindleparser.models.HeadingGroup;
import io.github.vlovric.kindleparser.template.TemplateClipping;
import io.github.vlovric.kindleparser.template.TemplateGroup;
import io.github.vlovric.kindleparser.template.TemplateHeading;

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
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(false);
            cfg.setWrapUncheckedExceptions(true);
            cfg.setFallbackOnNullLoopVariable(false);

            // Restrict dangerous template features (keep it as a pure renderer).
            cfg.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
            cfg.setAPIBuiltinEnabled(false);
            cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_33).build());

            // Enable relative includes/macros from the same folder as the template.
            cfg.setTemplateLoader(new FileTemplateLoader(templatePath.toAbsolutePath().getParent().toFile()));

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
     * Overloaded convenience method to write directly to standard out if no specific Writer is requested.
     */
    public void renderToStdout(List<HeadingGroup> groups, String title) throws IOException {
        render(groups, title, new PrintWriter(System.out));
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