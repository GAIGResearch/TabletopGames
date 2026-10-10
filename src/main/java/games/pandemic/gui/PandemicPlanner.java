package games.pandemic.gui;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.properties.PropertyString;
import games.pandemic.*;
import games.pandemic.actions.Forecast;
import games.pandemic.actions.MovePlayer;
import games.pandemic.actions.TreatDisease;
import gui.IMovePlanner;
import utilities.Hash;

import java.util.List;

import static core.CoreConstants.nameHash;
import static core.CoreConstants.playerHandHash;

/**
 * Plans a player's actions for their turn (4 by default): moves, treating, building, sharing and curing, in order,
 * each undoable, sent together. Nothing random or hidden happens between them; the cards are drawn and the cities
 * infected only after the last, so that is where the plan ends. Forecast, which looks at the infection deck, is not
 * planned; nor is anything once a hand is over its limit (a discard must then be chosen).
 * <p>
 * A planned action is carried out as the forward model's PlayerAction rule carries it out (with the Medic's treating on
 * arrival, and a step of the turn), not by the forward model itself: it is rule-based, and keeps its place in the turn
 * in itself, so it must not be run on a copy of the game.
 */
public class PandemicPlanner implements IMovePlanner {

    private final PandemicForwardModel fm;

    public PandemicPlanner(PandemicForwardModel fm) {
        this.fm = fm;
    }

    @Override
    public boolean plans(AbstractGameState state, int player) {
        return planning((PandemicGameState) state, player);
    }

    /**
     * Whether the player is to take one of their turn's actions.
     */
    private static boolean planning(PandemicGameState s, int player) {
        PandemicTurnOrder order = (PandemicTurnOrder) s.getTurnOrder();
        PandemicParameters params = (PandemicParameters) s.getGameParameters();
        return s.isNotTerminal() && s.getGamePhase() == CoreConstants.DefaultGamePhase.Main
                && order.getCurrentPlayer(s) == player && order.getTurnStep() < params.getnActionsPerTurn()
                && !handOverLimit(s);
    }

    private static boolean handOverLimit(PandemicGameState s) {
        for (int p = 0; p < s.getNPlayers(); p++)
            if (((Deck<?>) s.getComponent(playerHandHash, p)).isOverCapacity())
                return true;
        return false;
    }

    @Override
    public List<AbstractAction> options(AbstractGameState planned, int player) {
        PandemicGameState s = (PandemicGameState) planned;
        if (!planning(s, player)) return List.of();
        return fm.computeAvailableActions(s).stream().filter(a -> !(a instanceof Forecast)).toList();
    }

    /**
     * As the PlayerAction rule: the action, the Medic's treating of cured diseases in the city moved to, and a step
     * of the turn.
     */
    @Override
    public void apply(AbstractGameState planned, AbstractAction action) {
        PandemicGameState s = (PandemicGameState) planned;
        PandemicTurnOrder order = (PandemicTurnOrder) s.getTurnOrder();
        int player = order.getCurrentPlayer(s);
        action.execute(s);
        if (action instanceof MovePlayer move) {
            Card role = (Card) s.getComponent(PandemicConstants.playerCardHash, move.getPlayerToMove());
            if (((PropertyString) role.getProperty(nameHash)).value.equals("Medic")) {
                int cubes = ((PandemicParameters) s.getGameParameters()).getnInitialDiseaseCubes();
                for (String colour : PandemicConstants.colors) {
                    Counter cure = (Counter) s.getComponent(Hash.GetInstance().hash("Disease " + colour));
                    if (cure.getValue() > 0)
                        new TreatDisease(cubes, colour, move.getDestination(), true).execute(s);
                }
            }
        }
        order.endPlayerTurnStep();
    }

    @Override
    public List<String> warnings(AbstractGameState planned, int player) {
        PandemicGameState s = (PandemicGameState) planned;
        PandemicTurnOrder order = (PandemicTurnOrder) s.getTurnOrder();
        int left = ((PandemicParameters) s.getGameParameters()).getnActionsPerTurn() - order.getTurnStep();
        if (s.getGamePhase() != CoreConstants.DefaultGamePhase.Main || order.getCurrentPlayer(s) != player || left <= 0)
            return List.of();
        if (handOverLimit(s))
            return List.of("A hand is over the limit: send the plan, and a card must be discarded.");
        return List.of(left + (left == 1 ? " action" : " actions") + " left this turn: you will be asked for "
                + (left == 1 ? "it" : "them") + " after sending.");
    }

    @Override
    public String sendLabel() {
        return "Take actions";
    }
}
