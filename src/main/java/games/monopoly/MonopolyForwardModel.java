package games.monopoly;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import games.monopoly.actions.*;
import games.monopoly.components.MonopolyCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;

public class MonopolyForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        MonopolyGameState state = (MonopolyGameState) firstState;
        MonopolyParameters params = (MonopolyParameters) state.getGameParameters();
        MonopolyBoard board = params.getBoard();
        int nPlayers = state.getNPlayers();
        int nSquares = board.nSquares();

        state.cash = new int[nPlayers];
        Arrays.fill(state.cash, params.startingCash);
        // every token starts on GO
        state.position = new int[nPlayers];
        state.owner = new int[nSquares];
        Arrays.fill(state.owner, -1);
        state.buildings = new int[nSquares];
        state.mortgaged = new boolean[nSquares];
        state.inJail = new boolean[nPlayers];
        state.jailRolls = new int[nPlayers];
        state.nDoubles = 0;
        state.anotherRoll = false;
        state.dice = new int[2];
        state.finalPlace = new int[nPlayers];

        state.chanceDeck = new Deck<>("Chance", HIDDEN_TO_ALL);
        state.chanceDeck.add(board.chanceCards());
        state.chanceDeck.shuffle(state.getRnd());
        state.communityChestDeck = new Deck<>("Community Chest", HIDDEN_TO_ALL);
        state.communityChestDeck.add(board.communityChestCards());
        state.communityChestDeck.shuffle(state.getRnd());
        state.jailCards = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.jailCards.add(new Deck<>("Get Out of Jail Free " + p, p, VISIBLE_TO_ALL));

        // the highest roll of the dice goes first
        state.setFirstPlayer(state.getRnd().nextInt(nPlayers));
        state.setGamePhase(MonopolyGamePhase.ROLL);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        MonopolyGameState state = (MonopolyGameState) gameState;
        MonopolyParameters params = (MonopolyParameters) state.getGameParameters();
        int player = state.getCurrentPlayer();
        List<AbstractAction> actions = new ArrayList<>();
        if (state.getGamePhase() == MonopolyGamePhase.ROLL) {
            if (state.isInJail(player)) {
                if (state.getCash(player) >= params.jailFine)
                    actions.add(new PayJailFine());
                for (MonopolyCard card : state.getJailCards(player).getComponents())
                    actions.add(new UseJailCard(card));
            }
            actions.add(new RollDice());
        } else {
            for (MonopolySquare s : state.getProperties(player)) {
                if (state.canBuild(s))
                    actions.add(new BuildHouse(s));
                if (state.canSellBuilding(s))
                    actions.add(new SellBuilding(s));
                if (state.canMortgage(s))
                    actions.add(new Mortgage(s));
                else if (state.isMortgaged(s) && state.getCash(player) >= params.unmortgageCost(s))
                    actions.add(new Unmortgage(s));
            }
            actions.add(state.hasAnotherRoll() ? new RollDice() : new EndTurn());
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        MonopolyGameState state = (MonopolyGameState) currentState;
        // a decision pushed by the action (buying a property) is still to come
        if (state.isActionInProgress())
            return;
        if (state.getNPlayersIn() == 1) {
            endGame(state);
            return;
        }
        int player = state.getCurrentPlayer();
        // a roll that leaves the player in Jail (sent there, or failing to roll out) ends the turn at once
        if (actionTaken instanceof EndTurn || state.isBankrupt(player) || state.isInJail(player))
            endTurn(state);
        else if (!(actionTaken instanceof PayJailFine || actionTaken instanceof UseJailCard))
            state.setGamePhase(MonopolyGamePhase.MANAGE);
    }

    /**
     * Passes the turn to the next player not bankrupt.
     */
    private void endTurn(MonopolyGameState state) {
        int current = state.getCurrentPlayer();
        int next = state.nextPlayerIn(current);
        state.setNDoubles(0);
        state.setAnotherRoll(false);
        state.setGamePhase(MonopolyGamePhase.ROLL);
        endPlayerTurn(state, next);
        // a round ends each time the turn passes back round the table
        if (next <= current)
            endRound(state, next);
    }
}
