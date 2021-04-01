package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.components.Award;

import java.awt.*;

public class ClaimableAwardMilestoneRequirement implements Requirement<TMGameState> {

    int amID;
    int player;

    public ClaimableAwardMilestoneRequirement(int amID, int player) {
        this.amID = amID;
        this.player = player;
    }

    @Override
    public boolean testCondition(TMGameState gs) {
        if (player == -1) {
            player = gs.getCurrentPlayer();
        }
        Award am = (Award) gs.getComponentById(amID);
        return am.canClaim(gs, player);
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
        return null;
    }
}
