package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.components.DescentDice;
import games.descent2e.components.DicePool;
import games.descent2e.components.DiceType;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class RollDie extends AbstractAction {
    Map<DiceType, Integer> desiredDice;
    /**
     * @param dice A map consisting of coloured dice to be rolled (key) and how many (values)
     *             <p>
     *             Rolled dice are stored in the dice pool in the game state
     *             In the form of a Map<String, List<DescentDice>>
     */
    public RollDie(Map<DiceType, Integer> dice) {
        // Maps from color of die to how many of that color should be rolled
        this.desiredDice = dice;
    }

    /**
     * Rolls dice specified in desiredDice
     *
     * @param gs - game state which should be modified by this action.
     */
    @Override
    public boolean execute(AbstractGameState gs) {
        DescentGameState dgs = (DescentGameState) gs;
        DicePool newPool = DicePool.constructDicePool(desiredDice);
        newPool.roll(dgs.getRnd());
        dgs.setAttributeDicePool(newPool);
        return true;
    }

    @Override
    public RollDie copy() {
        return new RollDie(desiredDice);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RollDie rd) {
            return desiredDice.equals(rd.desiredDice);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(desiredDice);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Roll dice";
    }

}
