package games.resistance.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.resistance.ResGameState;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class ResTeamBuilding extends AbstractAction implements IExtendedSequence {
    public final int playerId;
    private final int[] team;

    public ResTeamBuilding(int playerId, int[] team) {
        this.playerId = playerId;
        this.team = team;
    }

    public int[] getTeam() {return team.clone();}
    @Override
    public boolean execute(AbstractGameState gs) {
        ((ResGameState)gs).addTeamChoice(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        ResGameState resgs = (ResGameState) state;

        List<AbstractAction> actions = new ArrayList<>();


            int[] players = new int[resgs.getNPlayers()];
            for (int i = 0; i < resgs.getNPlayers(); i++) {
                players[i] = i;
            }
            ArrayList<int[]> choiceOfTeams = Utils.generateCombinations(players, resgs.gameBoard.getMissionSuccessValues()[resgs.getRoundCounter()]);
            for(int[] team : choiceOfTeams) {
                actions.add(new ResTeamBuilding(playerId, team));
                if (team.length == 0){throw new AssertionError("Team Size Zero");}
            }

        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerId;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {}

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return false;
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
        return Objects.hash(playerId)  + Arrays.hashCode(team);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Leader Has Suggested Team :  " + Arrays.toString(team);
    }



}
