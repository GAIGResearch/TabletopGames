package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import utilities.ImageIO;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class TagsPlayedRequirement implements Requirement<TMGameState> {

    public final TMTypes.Tag[] tags;
    public final int[] nMin;
    int nTags;

    public TagsPlayedRequirement(TMTypes.Tag[] tag, int[] nMin) {
        this.tags = tag;
        this.nMin = nMin;

        nTags = 0;
        if (tags != null && nMin != null)
        for (int k = 0; k < tags.length; k++) {
            nTags += nMin[k];
        }
    }

    @Override
    public boolean testCondition(TMGameState gs) {
        for (int i = 0; i < nMin.length; i++) {
            TMTypes.Tag tag = tags[i];
            if (gs.getPlayerCardsPlayedTags()[gs.getCurrentPlayer()].get(tag).getValue() < nMin[i]) return false;
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
        if (nTags > 4) return ""+nMin[0];
        return null;
    }

    @Override
    public String getReasonForFailure(TMGameState gs) {
        String reasons = "";
        for (int i = 0; i < nMin.length; i++) {
            TMTypes.Tag tag = tags[i];
            if (gs.getPlayerCardsPlayedTags()[gs.getCurrentPlayer()].get(tag).getValue() < nMin[i]) {
                reasons += "Need " + nMin[i] + " " + tag + " tags. ";
            } else {
                reasons += "Enough " + tag + " tags. ";
            }
        }
        return reasons;
    }

    @Override
    public Image[] getDisplayImages() {
        int n = nTags;
        if (n > 4) n = tags.length;
        Image[] imgs = new Image[n];
        int i = 0;
        for (int k = 0; k < tags.length; k++) {
            String path = tags[k].getImagePath();
            if (n == nTags) {
                for (int j = 0; j < nMin[k]; j++) {
                    imgs[i] = ImageIO.GetInstance().getImage(path);
                    i++;
                }
            } else {
                imgs[i] = ImageIO.GetInstance().getImage(path);
                i++;
            }
        }
        return imgs;
    }

    @Override
    public TagsPlayedRequirement copy() {
        return new TagsPlayedRequirement(tags.clone(), nMin.clone());
    }

    @Override
    public Requirement<TMGameState> copySerializable() {
        return new TagsPlayedRequirement(tags != null && tags.length > 0 ? tags.clone() : null, nMin != null && nMin.length > 0 ? nMin.clone() : null);
    }

    @Override
    public String toString() {
        return "Tags Played";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagsPlayedRequirement)) return false;
        TagsPlayedRequirement that = (TagsPlayedRequirement) o;
        return nTags == that.nTags && Arrays.equals(tags, that.tags) && Arrays.equals(nMin, that.nMin);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(nTags);
        result = 31 * result + Arrays.hashCode(tags);
        result = 31 * result + Arrays.hashCode(nMin);
        return result;
    }
}
