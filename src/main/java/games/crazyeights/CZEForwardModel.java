package games.crazyeights;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.crazyeights.actions.DrawCard;
import games.crazyeights.actions.NominateSuit;
import games.crazyeights.actions.Pass;
import games.crazyeights.actions.PlayCard;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.GameResult.*;
import static core.CoreConstants.VisibilityMode.*;

/**
 * <p>The forward model contains all the game rules and logic for Crazy Eights.</p>
 * <p>See claude_game_creator/CrazyEights_plan.txt for the implementation phases. Implemented:
 * Phase A (deal, matching rules with suit nomination for Eights, drawing, winning by emptying your hand) and
 * Phase B (reshuffling the discards into a new stock, passing, and the blocked-game ending),
 * Phase C (ranking) and Phase D (optional dealer nomination of the suit for a starter Eight).</p>
 */
public class CZEForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        CZEGameState state = (CZEGameState) firstState;
        CZEParameters params = (CZEParameters) state.getGameParameters();

        state.drawDeck = FrenchCard.generateDeck("DrawDeck", HIDDEN_TO_ALL);
        state.drawDeck.shuffle(state.getRnd());
        state.discardPile = new Deck<>("DiscardPile", VISIBLE_TO_ALL);

        state.playerHands = new ArrayList<>();
        int nCards = params.cardsToDeal(state.getNPlayers());
        for (int p = 0; p < state.getNPlayers(); p++) {
            Deck<FrenchCard> hand = new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER);
            for (int i = 0; i < nCards; i++)
                hand.add(state.drawDeck.draw());
            state.playerHands.add(hand);
        }

        FrenchCard starter = state.drawDeck.draw();
        state.discardPile.add(starter);
        state.currentSuit = CZEGameState.isEight(starter) ? params.starterEightSuit : starter.suite;
        state.consecutivePasses = 0;
        if (params.dealerNominatesStarterSuit && CZEGameState.isEight(starter))
            // the dealer chooses the suit before player 0 starts; starterEightSuit is only a placeholder until then
            state.setActionInProgress(new CZEStarterSuitNomination());
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        CZEGameState state = (CZEGameState) gameState;
        int player = state.getCurrentPlayer();
        List<AbstractAction> actions = new ArrayList<>();
        for (FrenchCard card : state.getPlayerHands().get(player).getComponents()) {
            if (!state.canPlay(card)) continue;
            if (CZEGameState.isEight(card)) {
                for (FrenchCard.Suite suit : FrenchCard.Suite.values())
                    actions.add(new PlayCard(player, card, suit));
            } else {
                actions.add(new PlayCard(player, card, card.suite));
            }
        }
        if (actions.isEmpty()) {
            // You must play if you can; only otherwise draw, and pass only if there is nothing to draw
            actions.add(state.canDraw() ? new DrawCard(player) : new Pass(player));
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        CZEGameState state = (CZEGameState) currentState;
        if (actionTaken instanceof NominateSuit)
            return;   // handled by CZEStarterSuitNomination: not a turn, and play has not started
        if (actionTaken instanceof Pass)
            state.setConsecutivePasses(state.getConsecutivePasses() + 1);
        else
            state.setConsecutivePasses(0);

        if (state.getPlayerHands().get(state.getCurrentPlayer()).getSize() == 0
                || state.getConsecutivePasses() >= state.getNPlayers()) {
            endGame(state);
        } else {
            endPlayerTurn(state);
        }
    }

    /**
     * A player who goes out has no penalty, so the highest score, and the default results apply.
     * A blocked game (every player passed in succession) is won by every player with the fewest cards in hand.
     * Game.run() calls this again once the game is over, so the blocked-game results must be set here.
     */
    @Override
    protected void endGame(AbstractGameState gs) {
        CZEGameState state = (CZEGameState) gs;
        if (state.getConsecutivePasses() < state.getNPlayers()) {
            super.endGame(gs);
            return;
        }
        state.setGameStatus(GAME_END);
        int fewest = state.getPlayerHands().stream().mapToInt(Deck::getSize).min().orElseThrow();
        for (int p = 0; p < state.getNPlayers(); p++)
            state.setPlayerResult(state.getPlayerHands().get(p).getSize() == fewest ? WIN_GAME : LOSE_GAME, p);
    }
}
