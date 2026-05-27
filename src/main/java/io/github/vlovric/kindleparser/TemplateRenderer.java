package io.github.vlovric.kindleparser;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.github.vlovric.kindleparser.models.HeadingGroup;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a list of HeadingGroups to an output stream (console or file)
 * by applying them securely to an external JMustache template file.
 */
public class TemplateRenderer {

    private final Path templatePath;

    /**
     * @param templatePath the location of the user-provided .mustache template file
     */
    public TemplateRenderer(Path templatePath) {
        this.templatePath = templatePath;
    }

    /**
     * Compiles the mustache template from the filesystem and injects the title and groupings.
     * Rendering avoids loading entire documents into memory.
     *
     * @param groups the populated groupings
     * @param title  the book title string
     * @param writer the destination writer to stream the output into
     * @throws IOException if reading the template or writing the output fails
     */
    public void render(List<HeadingGroup> groups, String title, Writer writer) throws IOException {
        String templateString = Files.readString(templatePath);
        Template compiledTemplate = Mustache.compiler().compile(templateString);

        Map<String, Object> context = new HashMap<>();
        context.put("title", title);
        context.put("groups", groups);

        compiledTemplate.execute(context, writer);
        writer.flush();
    }

    /**
     * Overloaded convenience method to write directly to standard out if no specific Writer is requested.
     */
    public void renderToStdout(List<HeadingGroup> groups, String title) throws IOException {
        render(groups, title, new PrintWriter(System.out));
    }
}