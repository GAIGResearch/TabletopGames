package games.seasaltpaper;

import core.*;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import core.actions.DrawCard;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.GameType;
import games.seasaltpaper.actions.DrawAndDiscard;
import games.seasaltpaper.actions.PlayDuo;
import games.seasaltpaper.cards.*;
import games.seasaltpaper.SeaSaltPaperGameState.TurnPhase;
import scala.Array;

import java.util.*;

import static core.CoreConstants.*;


public class SeaSaltPaperForwardModel extends StandardForwardModel {

    /**
     * Creates the initial game-state of Sea Salt and Paper.
     *
     * @param firstState - state to be modified
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) firstState;
        sspgs.drawPile = new Deck<>("drawPile", VisibilityMode.HIDDEN_TO_ALL);

        // TODO make discard pile partial observable (only top card visible)
        sspgs.discardPile1 = new Deck<>("discardPile1", VisibilityMode.VISIBLE_TO_ALL);
        sspgs.discardPile2 = new Deck<>("discardPile2", VisibilityMode.VISIBLE_TO_ALL);

        sspgs.playerHands = new ArrayList<>(sspgs.getNPlayers());
        sspgs.playerPoints = new int[sspgs.getNPlayers()];
        // Set up first round
        setupRound(sspgs);
    }

    protected void setupRound(SeaSaltPaperGameState sspgs)
    {
        sspgs.drawPile.clear();
        sspgs.discardPile1.clear();
        sspgs.discardPile2.clear();

        // Placeholder
        for (int i = 0; i < 10; i++) {
            sspgs.drawPile.add(new SeaSaltPaperCard(CardColor.BLUE, SuiteType.PENGUIN, CardType.COLLECTOR));
        }
        sspgs.drawPile.shuffle(sspgs.getRnd());

        // Set up player hands and discards
        if (sspgs.playerHands.isEmpty()) {
            for (int i = 0; i < sspgs.getNPlayers(); i++) {
                boolean[] visible = new boolean[sspgs.getNPlayers()];
                if (sspgs.getCoreGameParameters().partialObservable) {  // if partialObservable then only the owner sees it
                    visible[i] = true;
                } else {
                    Arrays.fill(visible, true);
                }
                PartialObservableDeck<SeaSaltPaperCard> playerHand = new PartialObservableDeck<SeaSaltPaperCard>("playerHand"+i, i, visible);
                sspgs.playerHands.add(playerHand);
            }
        }

        // Set-up discard piles
        sspgs.discardPile1.add(sspgs.drawPile.draw());
        sspgs.discardPile2.add(sspgs.drawPile.draw());

        // Reset player status
        for (int i = 0; i < sspgs.getNPlayers(); i++) {
            sspgs.setPlayerResult(GameResult.GAME_ONGOING, i);
            sspgs.playerHands.get(i).clear();
        }

        // Update components in the game state
        sspgs.updateComponents();
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        //Test Placeholder
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        switch (sspgs.currentPhase) {
            case DRAW: //TODO check what happen if draw pile is empty
                int currentPlayerHandId = sspgs.playerHands.get(sspgs.getCurrentPlayer()).getComponentID();
                // Draw from discard pile
                if (sspgs.discardPile1.getSize() != 0) {
                    actions.add(new DrawCard(sspgs.discardPile1.getComponentID(), currentPlayerHandId));
                }
                if (sspgs.discardPile2.getSize() != 0) {
                    actions.add(new DrawCard(sspgs.discardPile2.getComponentID(), currentPlayerHandId));
                }
                // Draw 2 from draw pile, then discard 1 to one of the discard pile
                actions.add(new DrawAndDiscard(sspgs.getCurrentPlayer()));
                break;
            case DUO:   // TODO Can play as many duo cards as they want
                System.out.println("this is duo phase");
                actions.add(new PlayDuo());
                actions.add(new DoNothing());
                break;
            case STOP:  // TODO merge this with DUO phase
                System.out.println("this is stop phase");
                actions.add(new PlayDuo());
                actions.add(new PlayDuo());
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        if (gameState.isActionInProgress()) return;

        System.out.println("ACTIONS EXECUTED XD!!");
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gameState;
        if (action instanceof PlayDuo) {
            sspgs.currentPhase = sspgs.currentPhase.next();
        }
        if (sspgs.currentPhase == TurnPhase.FINISH) // Current player's turn is finished, end the turn
        {
            int nextPlayer = (sspgs.getCurrentPlayer() + 1) % sspgs.getNPlayers();
            endPlayerTurn(sspgs, nextPlayer);
            sspgs.currentPhase = sspgs.currentPhase.next();
        }
    }

}
