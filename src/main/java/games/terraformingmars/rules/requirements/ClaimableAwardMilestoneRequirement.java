package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.components.Award;
import games.terraformingmars.components.Milestone;

import java.awt.*;
import java.util.Objects;

public class ClaimableAwardMilestoneRequirement implements Requirement<TMGameState> {

    final int awardMilestoneID;
    final int player;

    public ClaimableAwardMilestoneRequirement(int amID, int player) {
        this.awardMilestoneID = amID;
        this.player = player;
    }

    @Override
    public boolean testCondition(TMGameState gs) {
        int p = player;
        if (p == -1) {
            p = gs.getCurrentPlayer();
        }
        Award am = (Award) gs.getComponentById(awardMilestoneID);
        return am.canClaim(gs, p);
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
        Award am = (Award) gs.getComponentById(awardMilestoneID);
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
    public ClaimableAwardMilestoneRequirement copy() {
        return new ClaimableAwardMilestoneRequirement(awardMilestoneID, player);
    }

    @Override
    public String toString() {
        return "Award/Milestone";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClaimableAwardMilestoneRequirement)) return false;
        ClaimableAwardMilestoneRequirement that = (ClaimableAwardMilestoneRequirement) o;
        return awardMilestoneID == that.awardMilestoneID && player == that.player;
    }

    @Override
    public int hashCode() {
        return Objects.hash(awardMilestoneID, player);
    }
}
