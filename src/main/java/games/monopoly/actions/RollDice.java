package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolyParameters;

/**
 * The current player rolls the two dice and moves by their total, or tries to roll a double to leave Jail.
 */
public class RollDice extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        MonopolyParameters params = (MonopolyParameters) state.getGameParameters();
        int player = state.getCurrentPlayer();
        int[] dice = state.rollDice();
        boolean doubles = dice[0] == dice[1];
        int total = dice[0] + dice[1];
        state.setAnotherRoll(false);

        if (state.isInJail(player)) {
            // a double frees the player, who moves by it with no further roll. The maxJailRolls-th failure makes them
            // pay the fine and move by the roll
            if (!doubles) {
                int failures = state.getJailRolls(player) + 1;
                state.setJailRolls(player, failures);
                if (failures < params.maxJailRolls)
                    return true;
                // the fine is paid before moving, so money raised for it comes first
                if (state.getCash(player) < params.jailFine && state.getRaisableValue(player) >= params.jailFine) {
                    state.setActionInProgress(new RaiseMoney(player, -1, params.jailFine, total));
                    return true;
                }
                state.pay(player, -1, params.jailFine);
                if (state.isBankrupt(player))
                    return true;
            }
            state.leaveJail(player);
            state.moveForward(player, total);
            return true;
        }

        // the maxDoubles-th double in a row sends the player to Jail; any other double gives another roll
        if (doubles) {
            state.setNDoubles(state.getNDoubles() + 1);
            if (state.getNDoubles() >= params.maxDoubles) {
                state.sendToJail(player);
                return true;
            }
        }
        state.moveForward(player, total);
        state.setAnotherRoll(doubles && !state.isInJail(player) && !state.isBankrupt(player));
        return true;
    }

    @Override
    public RollDice copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RollDice;
    }

    @Override
    public int hashCode() {
        return 640211;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Roll the dice";
    }

    @Override
    public String toString() {
        return "RollDice";
    }
}
