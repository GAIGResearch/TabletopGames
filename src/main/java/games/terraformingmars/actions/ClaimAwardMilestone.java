package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.rules.Award;
import games.terraformingmars.rules.Milestone;

import java.util.Objects;

public class ClaimAwardMilestone extends TMAction {
    final Award toClaim;

    public ClaimAwardMilestone(int player, Award toClaim) {
        super((toClaim instanceof Milestone? TMTypes.ActionType.ClaimMilestone : TMTypes.ActionType.FundAward), player, false);
        this.toClaim = toClaim;
        this.costResource = TMTypes.Resource.MegaCredit;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        int player = this.player;
        if (player == -1) player = gs.getCurrentPlayer();
        if (toClaim.claim((TMGameState) gs, player)) {
            if (toClaim instanceof Milestone) {
                ((TMGameState)gs).getnMilestonesClaimed().increment(1);
            } else {
                ((TMGameState)gs).getnAwardsFunded().increment(1);
            }
            return super.execute(gs);
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new ClaimAwardMilestone(player, toClaim.copy());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClaimAwardMilestone)) return false;
        if (!super.equals(o)) return false;
        ClaimAwardMilestone that = (ClaimAwardMilestone) o;
        return Objects.equals(toClaim, that.toClaim);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), toClaim);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return (toClaim instanceof Milestone? "Claim milestone " + toClaim.name : "Fund award " + toClaim.name);
    }

    @Override
    public String toString() {
        return (toClaim instanceof Milestone? "Claim milestone " + toClaim.name : "Fund award " + toClaim.name);
    }

    public int getCost(TMGameState gs) {
        TMGameParameters gp = (TMGameParameters)gs.getGameParameters();
        if (toClaim instanceof Milestone) {
            return gp.getnCostMilestone()[gs.getnMilestonesClaimed().getValue()];
        }
        return gp.getnCostAwards()[gs.getnAwardsFunded().getValue()];
    }

    @Override
    public boolean canBePlayed(TMGameState gs) {
        if (!super.canBePlayed(gs)) return false;
        int p = player;
        if (p == -1) {
            // Can current player pay?
            p = gs.getCurrentPlayer();
        }
        if (toClaim instanceof Milestone) return !gs.getnMilestonesClaimed().isMaximum() && toClaim.canClaim(gs, p);
        return !gs.getnAwardsFunded().isMaximum() && toClaim.canClaim(gs, p);
    }
}
