package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import utilities.ImageIO;

import java.awt.*;

public class TagsPlayedRequirement implements Requirement<TMGameState> {

    public TMTypes.Tag[] tags;
    public int[] nMin;

    public TagsPlayedRequirement(TMTypes.Tag[] tag, int[] nMin) {
        this.tags = tag;
        this.nMin = nMin;
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
        return null;
    }

    @Override
    public Image[] getDisplayImages() {
        Image[] imgs = new Image[tags.length];
        int i = 0;
        for (TMTypes.Tag t: tags) {
            imgs[i] = ImageIO.GetInstance().getImage(t.getImagePath());
            i++;
        }
        return imgs;
    }
}
