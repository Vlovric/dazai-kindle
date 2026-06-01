package io.github.vlovric.kindleparser.template;

public final class TemplateHeading {
    private final String title;
    private final int level;
    private final int location;
    private final int charOffset;
    private final String file;
    private final String anchor;

    public TemplateHeading(String title, int level, int location, int charOffset, String file, String anchor) {
        this.title = title;
        this.level = level;
        this.location = location;
        this.charOffset = charOffset;
        this.file = file;
        this.anchor = anchor;
    }

    public String getTitle() {
        return title;
    }

    public int getLevel() {
        return level;
    }

    public int getLocation() {
        return location;
    }

    public int getCharOffset() {
        return charOffset;
    }

    public String getFile() {
        return file;
    }

    public String getAnchor() {
        return anchor;
    }
}
