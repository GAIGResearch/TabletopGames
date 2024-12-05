package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.explodingkittens.ExplodingKittensGameState;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class DefuseKitten implements IExtendedSequence {

    boolean executed;
    final int player;


    public DefuseKitten(int player) {
        this.player = player;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) state;
        return IntStream.rangeClosed(0, ekgs.getDrawPile().getSize()).mapToObj(PlaceKitten::new).collect(toList());
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof PlaceKitten)
            executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public DefuseKitten copy() {
        DefuseKitten retValue = new DefuseKitten(player);
        retValue.executed = executed;
        return retValue;
    }

    @Override
    public String toString() {
        return "Defuse Kitten";
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof DefuseKitten dk && dk.player == player && dk.executed == executed;
    }

    @Override
    public int hashCode() {
        return 9981 - player * 63 + (executed ? 1 : 0);
    }
}
