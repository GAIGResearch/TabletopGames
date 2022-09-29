package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.Award;
import games.terraformingmars.components.Milestone;
import games.terraformingmars.rules.requirements.ClaimableAwardMilestoneRequirement;

import java.util.Objects;

public class ClaimAwardMilestone extends TMAction {
    int toClaimID;

    public ClaimAwardMilestone() { super(); } // This is needed for JSON Deserializer

    public ClaimAwardMilestone(int player, Award toClaim, int cost) {
        super((toClaim instanceof Milestone? TMTypes.ActionType.ClaimMilestone : TMTypes.ActionType.FundAward), player, false);
        this.toClaimID = toClaim.getComponentID();
        this.setActionCost(TMTypes.Resource.MegaCredit, cost, -1);
        this.requirements.add(new ClaimableAwardMilestoneRequirement(toClaimID, player));
    }

    public ClaimAwardMilestone(int player, int toClaimID, TMTypes.ActionType at, int cost) {
        super(at, player, false);
        this.toClaimID = toClaimID;
        this.setActionCost(TMTypes.Resource.MegaCredit, cost, -1);
        this.requirements.add(new ClaimableAwardMilestoneRequirement(toClaimID, player));
    }

    @Override
    public boolean _execute(TMGameState gs) {
        int player = this.player;
        if (player == -1) player = gs.getCurrentPlayer();

        Award toClaim = (Award) gs.getComponentById(toClaimID);
        if (toClaim.claim(gs, player)) {
            if (toClaim instanceof Milestone) {
                gs.getnMilestonesClaimed().increment(1);
            } else {
                gs.getnAwardsFunded().increment(1);
            }
            return true;
        }
        return false;
    }

    @Override
    public ClaimAwardMilestone _copy() {
        return new ClaimAwardMilestone(player, toClaimID, actionType, getCost());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClaimAwardMilestone)) return false;
        if (!super.equals(o)) return false;
        ClaimAwardMilestone that = (ClaimAwardMilestone) o;
        return toClaimID == that.toClaimID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), toClaimID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Award toClaim = (Award) gameState.getComponentById(toClaimID);
        return (toClaim instanceof Milestone? "Claim milestone " + toClaim.getComponentName() : "Fund award " + toClaim.getComponentName());
    }

    @Override
    public String toString() {
        return (actionType == TMTypes.ActionType.ClaimMilestone ? "Claim milestone " + toClaimID : "Fund award " + toClaimID);
    }
}
