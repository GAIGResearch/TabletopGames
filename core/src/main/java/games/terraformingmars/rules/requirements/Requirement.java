package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;

import java.awt.*;

public interface Requirement<T> {
    /*
     2 cases implemented:
        - counter: global parameter / player resource / player production min or max;
        - minimum N tags on cards played by player
     */
    boolean testCondition(T o);
    boolean isMax();
    boolean appliesWhenAnyPlayer();
    String getDisplayText(TMGameState gs);
    String getReasonForFailure(TMGameState gs);
    Image[] getDisplayImages();

    static Requirement stringToRequirement(String s) {
        String[] split = s.split(":");
        // First is counter
        if (split[0].contains("tag")) {
            // Format: tag-tag1-tag2:min1-min2
            split[0] = split[0].replace("tag-", "");
            String[] tagDef = split[0].split("-");
            String[] minDef = split[1].split("-");
            TMTypes.Tag[] tags = new TMTypes.Tag[tagDef.length];
            int[] minValues = new int[tagDef.length];
            for (int i = 0; i < tagDef.length; i++) {
                tags[i] = TMTypes.Tag.valueOf(tagDef[i]);
                minValues[i] = Integer.parseInt(minDef[i]);
            }
            return new TagsPlayedRequirement(tags, minValues);
        } else {
            return new CounterRequirement(split[0], Integer.parseInt(split[1]), split[2].equalsIgnoreCase("max"));
        }
    }

    Requirement<T> copy();
    default Requirement<T> copySerializable() {return copy();}
}
