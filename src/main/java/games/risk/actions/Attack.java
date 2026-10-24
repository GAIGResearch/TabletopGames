package games.risk.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Deck;
import games.risk.components.RiskCard;
import games.risk.components.RiskMission;
import games.risk.RiskGameState;
import games.risk.RiskParameters;
import games.risk.RiskTerritory;
import games.risk.RiskUtils;

import java.util.Objects;

/**
 * The current player attacks the territory to from the territory from with nDice dice, for one roll.
 */
public class Attack extends AbstractAction {

    public final RiskTerritory from;
    public final RiskTerritory to;
    public final int nDice;

    public Attack(RiskTerritory from, RiskTerritory to, int nDice) {
        this.from = from;
        this.to = to;
        this.nDice = nDice;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RiskGameState state = (RiskGameState) gs;
        RiskParameters params = (RiskParameters) state.getGameParameters();
        if (params.defenderChoosesDice) {
            // the defender chooses their dice (DefendWith), and the roll happens then
            state.setActionInProgress(new DefenderDice(state.getOwner(to), from, to, nDice));
            return true;
        }
        rollAndCapture(state, state.getCurrentPlayer(), from, to, nDice, maxDefendDice(state, to));
        return true;
    }

    /**
     * The most dice the defender of the territory may roll: maxDefendDice, but no more than their armies there.
     */
    static int maxDefendDice(RiskGameState state, RiskTerritory to) {
        RiskParameters params = (RiskParameters) state.getGameParameters();
        return Math.min(params.maxDefendDice, state.getArmies(to));
    }

    /**
     * One roll, and the capture if it empties the territory.
     */
    static void rollAndCapture(RiskGameState state, int attacker, RiskTerritory from, RiskTerritory to,
                               int nAttackDice, int nDefendDice) {
        roll(state, from, to, nAttackDice, nDefendDice);
        if (state.getArmies(to) == 0)
            capture(state, attacker, from, to, nAttackDice);
    }

    /**
     * One roll of the battle: the attacker rolls nAttackDice dice, the defender nDefendDice, and the losses are
     * taken off the two territories.
     */
    private static void roll(RiskGameState state, RiskTerritory from, RiskTerritory to, int nAttackDice,
                             int nDefendDice) {
        int[] attackDice = new int[nAttackDice];
        for (int i = 0; i < nAttackDice; i++)
            attackDice[i] = state.rollDie();
        int[] defendDice = new int[nDefendDice];
        for (int i = 0; i < defendDice.length; i++)
            defendDice[i] = state.rollDie();
        int[] losses = RiskUtils.battleLosses(attackDice, defendDice);
        state.addArmies(from, -losses[0]);
        state.addArmies(to, -losses[1]);
    }

    /**
     * The attacker takes the emptied territory.
     */
    private static void capture(RiskGameState state, int player, RiskTerritory from, RiskTerritory to, int nDice) {
        RiskParameters params = (RiskParameters) state.getGameParameters();
        int defender = state.getOwner(to);
        state.setOwner(to, player);
        state.setCapturedThisTurn(true);
        // a defender with no territory left is eliminated, and the attacker takes their cards
        boolean eliminated = state.getNTerritories(defender) == 0;
        if (eliminated) {
            state.setFinalPlace(defender, state.getNPlayers() - state.getNEliminated());
            state.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, defender);
            state.setEliminatedBy(defender, player);
            if (params.secretMission)
                handOverDestroyMission(state, player, defender);
            Deck<RiskCard> hand = state.getHand(player);
            for (RiskCard card : state.getHand(defender).getComponents())
                hand.add(card);
            state.getHand(defender).clear();
        }
        if (state.getNTerritories(player) == state.getMap().nTerritories()) {
            // the capture wins the game: the dice of the last roll move in at once
            state.addArmies(from, -nDice);
            state.addArmies(to, nDice);
        } else {
            // the attacker chooses how many armies to move in, at least the dice of the last roll. If they took an
            // eliminated player's cards and now hold eliminationTradeLimit or more, they then trade and place the
            // armies. The stack runs the last pushed first, so the trade is pushed first.
            if (eliminated && state.getHand(player).getSize() >= params.eliminationTradeLimit)
                state.setActionInProgress(new EliminationTrade(player));
            state.setActionInProgress(new MoveArmiesChoice(player, from, to, nDice));
        }
    }

    /**
     * The player (other than the one who eliminated them) whose Secret Mission was to destroy the defender takes the
     * defender's mission card instead. Their old card goes out of the game.
     */
    private static void handOverDestroyMission(RiskGameState state, int attacker, int defender) {
        for (int p = 0; p < state.getNPlayers(); p++) {
            RiskMission mission = state.getMission(p);
            if (p != attacker && !state.isEliminated(p) && mission != null
                    && mission.kind == RiskMission.Kind.DESTROY && mission.target == defender) {
                RiskMission taken = state.takeMission(defender);
                if (taken != null)
                    state.setMission(p, taken);
            }
        }
    }

    @Override
    public Attack copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Attack other && other.from.equals(from) && other.to.equals(to) && other.nDice == nDice;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, nDice) + 731207;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Attack(" + from + ", " + to + ", " + nDice + ")";
    }
}
