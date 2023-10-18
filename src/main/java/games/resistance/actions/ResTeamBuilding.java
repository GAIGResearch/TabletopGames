package games.resistance.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.resistance.ResGameState;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class ResTeamBuilding extends AbstractAction {
    public final int playerId;
    private final int[] team;

    public ResTeamBuilding(int playerId, int[] team) {
        this.playerId = playerId;
        this.team = team.clone();
    }

    public List<Integer> getTeam() {
        return Arrays.stream(team).boxed().collect(Collectors.toList());
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((ResGameState) gs).addTeamChoice(this);
        return true;
    }

    @Override
    public ResTeamBuilding copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResTeamBuilding)) return false;
        ResTeamBuilding that = (ResTeamBuilding) o;
        return playerId == that.playerId && Arrays.equals(team, that.team);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId) + Arrays.hashCode(team);
    }

    @Override
    public String toString() {
        return "Player " + playerId + " suggested team " + Arrays.toString(team);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

}
