package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.sirius.*;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class MovePolice extends AbstractAction implements IExtendedSequence {

    final int destinationMoon;
    int decidingPlayer = -1;
    int[] playersToStealFrom = new int[0];

    public MovePolice(int to) {
        destinationMoon = to;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        decidingPlayer = gs.getCurrentPlayer();
        for (Moon m : state.getAllMoons())
            m.removePolicePresence();
        state.getMoon(destinationMoon).setPolicePresence();
        playersToStealFrom = state.getPlayersAt(destinationMoon);
        if (playersToStealFrom.length > 0)
            state.setActionInProgress(this);  // only if there is anyone to steal from
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        int targetPlayer = playersToStealFrom[0];
        SiriusGameState state = (SiriusGameState) gs;
        List<AbstractAction> retValue = state.getPlayerHand(targetPlayer).stream()
                .filter(c -> c.cardType != SiriusConstants.SiriusCardType.FAVOUR)
                .map(c -> new StealCard(c, targetPlayer))
                .distinct().collect(toList());
        if (retValue.isEmpty())
            retValue.add(new StealCard(null, targetPlayer)); // in case they have no cards to steal
        return retValue;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return decidingPlayer;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        if (action instanceof StealCard) {
            StealCard theft = (StealCard) action;
            if (theft.targetPlayer != playersToStealFrom[0])
                throw new AssertionError("We are expecting to process a StealCard action targeting " + playersToStealFrom[0] + ", not " + theft.targetPlayer);
            // we have processed that player
            playersToStealFrom = Arrays.copyOfRange(playersToStealFrom, 1, playersToStealFrom.length);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return playersToStealFrom.length == 0;
    }

    @Override
    public MovePolice copy() {
        MovePolice retValue = new MovePolice(destinationMoon);
        retValue.playersToStealFrom = playersToStealFrom.clone();
        retValue.decidingPlayer = decidingPlayer;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MovePolice) {
            MovePolice other = (MovePolice) obj;
            return destinationMoon == other.destinationMoon && decidingPlayer == other.decidingPlayer &&
                    Arrays.equals(playersToStealFrom, other.playersToStealFrom);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return -902 + Objects.hash(destinationMoon, decidingPlayer) + 31 * Arrays.hashCode(playersToStealFrom);
    }

    @Override
    public String toString() {
        return String.format("Move Police Pawn to moon %d, and steal from %s", destinationMoon, Arrays.toString(playersToStealFrom));
    }

    @Override
    public String getString(AbstractGameState gameState) {
        SiriusGameState sgs = (SiriusGameState) gameState;
        return String.format("Move Police Pawn to moon %s, and steal from %s", sgs.getMoon(destinationMoon), Arrays.toString(playersToStealFrom));
    }
}
