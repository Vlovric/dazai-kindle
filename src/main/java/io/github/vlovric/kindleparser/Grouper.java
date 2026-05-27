package io.github.vlovric.kindleparser;

import io.github.vlovric.kindleparser.models.Clipping;
import io.github.vlovric.kindleparser.models.Heading;
import io.github.vlovric.kindleparser.models.HeadingGroup;
import io.github.vlovric.kindleparser.models.TocEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups Clipping objects under the correct Heading based on location bounds.
 * Knows nothing about EPUBs, file formats, or output contexts.
 */
public class Grouper {

    private static final Heading BEFORE_FIRST = new Heading(
            new TocEntry("(Before first heading)", "", null, 0),
            0,
            0
    );

    /**
     * Assigns each Clipping to the last Heading whose location is <=
     * the clipping's location, producing a list of HeadingGroups.
     * Empty groups (headings with no clippings) are excluded from the output.
     *
     * @param clippings the list of clippings parsed from MyClippings.txt
     * @param headings  the resolved Table of Contents headings from the EPUB
     * @return a list of populated HeadingGroups
     */
    public List<HeadingGroup> group(List<Clipping> clippings, List<Heading> headings) {
        Map<Integer, HeadingGroup> groups = makeGroups(headings);

        for (Clipping clipping : clippings) {
            // Null location implies 0 for grouping purposes
            int loc = clipping.location() != null ? clipping.location() : 0;
            int target = findHeadingIndex(loc, headings);
            groups.get(target).clippings().add(clipping);
        }

        List<HeadingGroup> result = new ArrayList<>();
        for (HeadingGroup group : groups.values()) {
            if (!group.clippings().isEmpty()) {
                result.add(group);
            }
        }
        return result;
    }

    /**
     * Creates an ordered map of groups keyed by heading index.
     * Index -1 acts as the "before-first" sentinel heading for clippings
     * that occur prior to the first formal book section.
     */
    private Map<Integer, HeadingGroup> makeGroups(List<Heading> headings) {
        Map<Integer, HeadingGroup> groups = new LinkedHashMap<>();
        groups.put(-1, new HeadingGroup(BEFORE_FIRST, new ArrayList<>()));
        for (int i = 0; i < headings.size(); i++) {
            groups.put(i, new HeadingGroup(headings.get(i), new ArrayList<>()));
        }
        return groups;
    }

    /**
     * Binary-search for the last heading whose location <= clipping location.
     * Returns -1 if the clipping precedes all headings.
     */
    private int findHeadingIndex(int location, List<Heading> headings) {
        int lo = 0;
        int hi = headings.size() - 1;
        int result = -1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (headings.get(mid).location() <= location) {
                result = mid;       // Potentially best, but search higher
                lo = mid + 1;
            } else {
                hi = mid - 1;       // Too high, search lower
            }
        }
        return result;
    }
}
