package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.awt.*;
import java.util.Arrays;

public class TagOnCardRequirement implements Requirement<TMCard> {

    final TMTypes.Tag[] tags;  // card must contain all of these tags

    public TagOnCardRequirement(TMTypes.Tag[] t) {
        this.tags = t;
    }

    @Override
    public boolean testCondition(TMCard card) {
        if (card == null) return false;
        if (tags == null) return true;
        for (TMTypes.Tag tag: tags) {
            boolean found = false;
            for (TMTypes.Tag t : card.tags) {
                if (t == tag) {
                    found = true;
                    break;
                }
            }
            if (! found) return false;
        }
        return true;
    }

    @Override
    public boolean isMax() {
        return false;
    }

    @Override
    public boolean appliesWhenAnyPlayer() {
        return false;
    }

    @Override
    public String getDisplayText(TMGameState gs) {
        return null;
    }

    @Override
    public String getReasonForFailure(TMGameState gs) {
        return null;
    }

    @Override
    public Image[] getDisplayImages() {
        return null;
    }

    public TagOnCardRequirement copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagOnCardRequirement)) return false;
        TagOnCardRequirement that = (TagOnCardRequirement) o;
        return Arrays.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(tags);
    }

    @Override
    public String toString() {
        return "Tag On Card";
    }
}
