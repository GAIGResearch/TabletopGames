package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import utilities.ImageIO;

import java.awt.*;
import java.util.ArrayList;

public class TagsPlayedRequirement implements Requirement<TMGameState> {

    public TMTypes.Tag[] tags;
    public int[] nMin;
    int nTags;

    public TagsPlayedRequirement(TMTypes.Tag[] tag, int[] nMin) {
        this.tags = tag;
        this.nMin = nMin;

        nTags = 0;
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
}
