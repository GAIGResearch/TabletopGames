package games.terraformingmars.rules;

import games.terraformingmars.TMGameState;

import java.util.Objects;

public class Milestone extends Award {
    public final int min;

    public Milestone(String name, int min, String counterID) {
        super(name, counterID);
        this.min = min;
    }

    public boolean claim(TMGameState gs) {
        if (claimed == -1) {
            int count = checkProgress(gs, gs.getCurrentPlayer());
            if (count >= min) {
                claimed = gs.getCurrentPlayer();
                return true;
            }
        }
        return false;
    }

    public Milestone copy() {
        Milestone copy = new Milestone(name, min, counterID);
        copy.claimed = claimed;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Milestone)) return false;
        Milestone milestone = (Milestone) o;
        return min == milestone.min &&
                claimed == milestone.claimed &&
                Objects.equals(name, milestone.name) &&
                Objects.equals(counterID, milestone.counterID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, min, counterID, claimed);
    }
}

