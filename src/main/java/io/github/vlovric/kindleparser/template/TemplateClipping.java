package io.github.vlovric.kindleparser.template;

public final class TemplateClipping {
    private final String bookTitle;
    private final String author;
    private final String type;
    private final Integer location;
    private final String rawLocation;
    private final Integer page;
    private final String date;
    private final String content;

    public TemplateClipping(
            String bookTitle,
            String author,
            String type,
            Integer location,
            String rawLocation,
            Integer page,
            String date,
            String content
    ) {
        this.bookTitle = bookTitle;
        this.author = author;
        this.type = type;
        this.location = location;
        this.rawLocation = rawLocation;
        this.page = page;
        this.date = date;
        this.content = content;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getAuthor() {
        return author;
    }

    public String getType() {
        return type;
    }

    public Integer getLocation() {
        return location;
    }

    public String getRawLocation() {
        return rawLocation;
    }

    public Integer getPage() {
        return page;
    }

    public String getDate() {
        return date;
    }

    public String getContent() {
        return content;
    }
}
