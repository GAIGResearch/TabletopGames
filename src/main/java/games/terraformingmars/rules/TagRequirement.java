package games.terraformingmars.rules;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

public class TagRequirement implements Requirement {

    TMTypes.Tag[] tags;
    int[] nMin;

    public TagRequirement(TMTypes.Tag[] tag, int[] nMin) {
        this.tags = tag;
        this.nMin = nMin;
    }

    @Override
    public boolean testCondition(TMGameState gs) {
        for (int i = 0; i < nMin.length; i++) {
            TMTypes.Tag tag = tags[i];
            int nCount = 0;
            for (TMCard card : gs.getPlayerCardsPlayed()[gs.getCurrentPlayer()].getComponents()) {
                for (TMTypes.Tag t : card.tags) {
                    if (t == tag) {
                        nCount++;
                    }
                }
            }
            if (nCount < nMin[i]) return false;
        }
        return true;
    }
}
