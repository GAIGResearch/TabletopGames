package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.components.Award;
import games.terraformingmars.components.Milestone;

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
    public String getReasonForFailure(TMGameState gs) {
        Award am = (Award) gs.getComponentById(amID);
        String reasons = "";
        if (am.isClaimed()) reasons += "Already claimed. ";
        else if ((am instanceof Milestone && gs.getnMilestonesClaimed().isMaximum()) || (!(am instanceof Milestone) && gs.getnAwardsFunded().isMaximum())) {
            reasons += "Max claimed. ";
        }
        else if (am instanceof Milestone) reasons += "Not enough: " + am.checkProgress(gs, player) + " / " + ((Milestone) am).min + " " + am.counterID;
        return reasons;
    }

    @Override
    public Image[] getDisplayImages() {
        return null;
    }

    @Override
    public String toString() {
        return "Award/Milestone";
    }
}
