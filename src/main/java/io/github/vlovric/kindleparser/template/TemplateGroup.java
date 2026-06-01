package io.github.vlovric.kindleparser.template;

import java.util.List;

public final class TemplateGroup {
    private final TemplateHeading heading;
    private final List<TemplateClipping> clippings;

    public TemplateGroup(TemplateHeading heading, List<TemplateClipping> clippings) {
        this.heading = heading;
        this.clippings = clippings;
    }

    public TemplateHeading getHeading() {
        return heading;
    }

    public List<TemplateClipping> getClippings() {
        return clippings;
    }
}
