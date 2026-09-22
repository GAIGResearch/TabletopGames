package games.gofish;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.gofish.actions.GoFishAsk;

import java.util.*;

import static core.CoreConstants.VisibilityMode.*;

/**
 * <p>The rules of Go Fish, as described at https://www.pagat.com/quartet/gofish.html.</p>
 */
public class GoFishForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        GoFishGameState state = (GoFishGameState) firstState;
        GoFishParameters params = (GoFishParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();

        state.drawDeck = FrenchCard.generateDeck("DrawDeck", HIDDEN_TO_ALL);
        state.drawDeck.shuffle(state.getRnd());

        state.playerHands = new ArrayList<>();
        state.playerBooks = new ArrayList<>();
        for (int i = 0; i < nPlayers; i++) {
            state.playerHands.add(new PartialObservableDeck<>("hand_" + i, i, nPlayers, VISIBLE_TO_OWNER));
            state.playerBooks.add(new Deck<>("books_" + i, i, VISIBLE_TO_ALL));
        }

        for (int j = 0; j < params.handSize(nPlayers); j++)
            for (int i = 0; i < nPlayers; i++)
                state.playerHands.get(i).add(state.drawDeck.draw());
        for (int i = 0; i < nPlayers; i++)
            layDownBooks(state, i);
        state.extraTurn = false;
        state.knownVoids = new GoFishKnownVoids(nPlayers);
        state.setFirstPlayer(0);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        GoFishGameState state = (GoFishGameState) gameState;
        int currentPlayer = state.getCurrentPlayer();
        List<AbstractAction> actions = new ArrayList<>();

        Set<Integer> ranks = new TreeSet<>();
        for (FrenchCard c : state.getPlayerHands().get(currentPlayer).getComponents())
            ranks.add(c.number);

        for (int target = 0; target < state.getNPlayers(); target++) {
            if (target == currentPlayer || state.getPlayerHands().get(target).getSize() == 0) continue;
            for (int r : ranks)
                actions.add(new GoFishAsk(target, r));
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        GoFishGameState state = (GoFishGameState) gameState;
        GoFishParameters params = (GoFishParameters) state.getGameParameters();
        layDownBooks(state, state.getCurrentPlayer());

        if (!params.playUntilAllBooks) {
            // the game ends as soon as a hand or the draw deck is empty
            if (state.drawDeck.getSize() == 0 || holders(state) < state.getNPlayers())
                endGame(state);
            else if (!state.extraTurn)
                passTurn(state);
            return;
        }

        if (!state.extraTurn)
            passTurn(state);
        // a player about to take a turn with an empty hand draws a card, or is skipped if there is none to draw
        while (state.isNotTerminal() && state.playerHands.get(state.getCurrentPlayer()).getSize() == 0) {
            int player = state.getCurrentPlayer();
            if (state.drawDeck.getSize() > 0) {
                state.playerHands.get(player).add(state.drawDeck.draw());
                state.knownVoids.drew(player);
            } else if (holders(state) <= 1) {
                break;
            } else {
                passTurn(state);
            }
        }
        // the game ends when the player to act has nobody to ask; once all 13 books are laid down nobody holds cards
        if (state.isNotTerminal() && holders(state) <= 1)
            endGame(state);
    }

    private void passTurn(GoFishGameState state) {
        endPlayerTurn(state);
        if (state.getCurrentPlayer() == state.getFirstPlayer())
            endRound(state);
    }

    /**
     * Moves any book (all four cards of a rank) in the player's hand to their books.
     */
    static void layDownBooks(GoFishGameState state, int playerId) {
        Deck<FrenchCard> hand = state.playerHands.get(playerId);
        Map<Integer, Integer> counts = new HashMap<>();
        for (FrenchCard c : hand.getComponents())
            counts.merge(c.number, 1, Integer::sum);
        for (Map.Entry<Integer, Integer> e : counts.entrySet())
            if (e.getValue() == 4)
                state.playerBooks.get(playerId).add(removeCardsOfRank(hand, e.getKey()));
    }

    public static Deck<FrenchCard> removeCardsOfRank(Deck<FrenchCard> hand, int rank) {
        Deck<FrenchCard> removed = new Deck<>("removed", HIDDEN_TO_ALL);
        for (int i = hand.getSize() - 1; i >= 0; i--)
            if (hand.get(i).number == rank)
                removed.add(hand.pick(i));
        return removed;
    }

    private static int holders(GoFishGameState state) {
        int holders = 0;
        for (Deck<FrenchCard> hand : state.playerHands)
            if (hand.getSize() > 0)
                holders++;
        return holders;
    }
}
