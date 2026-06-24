package io.github.vlovric.dazaikindle.models;

import java.util.List;

/**
 * A heading together with all clippings that fall under it.
 */
public record HeadingGroup(
    Heading heading,
    List<Clipping> clippings
) {
}
