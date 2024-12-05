package games.seasaltpaper;

import core.*;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.actions.DrawCard;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.seasaltpaper.actions.DrawAndDiscard;
import games.seasaltpaper.actions.LastChance;
import games.seasaltpaper.actions.PlayDuo;
import games.seasaltpaper.actions.Stop;
import games.seasaltpaper.cards.*;
import games.seasaltpaper.SeaSaltPaperGameState.TurnPhase;
import utilities.Pair;

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
        sspgs.playerDiscards = new ArrayList<>(sspgs.getNPlayers());
        sspgs.playerPoints = new int[sspgs.getNPlayers()];
        // Set up first round
        setupRound(sspgs);
    }

    protected void setupRound(SeaSaltPaperGameState sspgs)
    {
        sspgs.drawPile.clear();
        sspgs.discardPile1.clear();
        sspgs.discardPile2.clear();

        //TODO ADD ACTUAL CARDs
        // Placeholder
//        for (int i = 0; i < 10; i++) {
//            sspgs.drawPile.add(new SeaSaltPaperCard(CardColor.BLUE, CardSuite.SHELL, CardType.DUO));
//        }
//        sspgs.drawPile.shuffle(sspgs.getRnd());
        setupDrawPile(sspgs);

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

                Deck<SeaSaltPaperCard> playerDiscard = new Deck<>("PlayerHandDiscard"+i, i, VisibilityMode.VISIBLE_TO_ALL);
                sspgs.playerDiscards.add(playerDiscard);
            }
        }

        // Set-up discard piles
        sspgs.discardPile1.add(sspgs.drawPile.draw());
        sspgs.discardPile2.add(sspgs.drawPile.draw());

        // Reset player status
        for (int i = 0; i < sspgs.getNPlayers(); i++) {
            sspgs.setPlayerResult(GameResult.GAME_ONGOING, i);
            sspgs.playerHands.get(i).clear();
            sspgs.playerDiscards.get(i).clear();
        }

        // Update components in the game state
        sspgs.updateComponents();
    }

    private void setupDrawPile(SeaSaltPaperGameState gs) {
        SeaSaltPaperParameters parameters = (SeaSaltPaperParameters) gs.getGameParameters();

        for (Pair<CardSuite, CardType> p: parameters.cardsInit.keySet()) {
            int count = parameters.cardsInit.get(p).a;
            for (int i = 0; i < count; i++) {
                CardColor c = parameters.cardsInit.get(p).b[i];
                gs.drawPile.add(new SeaSaltPaperCard(c, p.a, p.b));
            }
        }
        gs.drawPile.shuffle(gs.getRnd());
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
            case DUO:
                System.out.println("this is duo/stop phase");
                actions.addAll(HandManager.generateDuoActions(sspgs, sspgs.getCurrentPlayer()));
                actions.add(new DoNothing());
                if (HandManager.calculatePoint(sspgs, sspgs.getCurrentPlayer()) >= 7) {
                    actions.add(new Stop(gameState.getCurrentPlayer()));
                    actions.add(new LastChance(gameState.getCurrentPlayer()));
                }
                break;
            default:
                actions.add(new DoNothing());
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        if (gameState.isActionInProgress()) return;

        System.out.println("ACTIONS EXECUTED XD!!");
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gameState;
        if (!(action instanceof PlayDuo)) {
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
