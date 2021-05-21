package games.terraformingmars.components;

import games.terraformingmars.TMGameState;

import java.util.Objects;

public class Milestone extends Award {
    public final int min;

    public Milestone(String name, int min, String counterID) {
        super(name, counterID);
        this.min = min;
    }

    protected Milestone(String name, int min, String counterID, int componentID) {
        super(name, counterID, componentID);
        this.min = min;
    }

    @Override
    public boolean canClaim(TMGameState gs, int player) {
        int count = checkProgress(gs, player);
        return !gs.getnMilestonesClaimed().isMaximum() && claimed == -1 && count >= min;
    }

    public Milestone copy() {
        Milestone copy = new Milestone(componentName, min, counterID, componentID);
        copy.claimed = claimed;
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Milestone)) return false;
        if (!super.equals(o)) return false;
        Milestone milestone = (Milestone) o;
        return min == milestone.min;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), min);
    }

    @Override
    public String toString() {
        return "Milestone{" + counterID + "}";
    }
}

