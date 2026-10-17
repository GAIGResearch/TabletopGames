package games.scopa;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.TarotCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.CoreConstants.VisibilityMode.*;

public class ScopaForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        ScopaGameState state = (ScopaGameState) firstState;
        int nPlayers = state.getNPlayers();

        state.playerHands = new ArrayList<>();
        state.capturedCards = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++) {
            state.playerHands.add(new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER));
            state.capturedCards.add(new Deck<>("Captured " + p, p, VISIBLE_TO_ALL));
        }
        state.drawDeck = new Deck<>("Draw deck", HIDDEN_TO_ALL);
        state.table = new Deck<>("Table", VISIBLE_TO_ALL);
        state.scopas = new int[nPlayers];
        state.bankedScores = new int[nPlayers];
        // the last player deals first, so player 0 plays first
        state.setFirstPlayer(0);
        deal(state);
    }

    /**
     * Clears away the last deal, if any, and deals afresh from a newly shuffled pack.
     */
    void deal(ScopaGameState state) {
        Deck<TarotCard> pack = TarotCard.generateItalianDeck("Pack", HIDDEN_TO_ALL);
        pack.shuffle(state.getRnd());
        deal(state, pack);
    }

    /**
     * Clears away the last deal, if any, and deals from the given pack in its order (index 0 first).
     */
    void deal(ScopaGameState state, Deck<TarotCard> pack) {
        ScopaParameters params = (ScopaParameters) state.getGameParameters();
        for (Deck<TarotCard> hand : state.playerHands)
            hand.clear();
        for (Deck<TarotCard> captured : state.capturedCards)
            captured.clear();
        state.table.clear();
        state.drawDeck.clear();
        for (int i = pack.getSize() - 1; i >= 0; i--)
            state.drawDeck.add(pack.get(i));  // add puts each card on top, so this keeps the pack's order
        Arrays.fill(state.scopas, 0);
        state.lastCapturer = -1;
        dealTableAndHands(state);
        // with three or four Kings on the table, the same dealer gathers the cards, shuffles and deals again
        while (params.redealOnKings && state.table.stream().filter(c -> c.number == TarotCard.KING).count() >= 3) {
            gatherIntoDrawDeck(state);
            state.drawDeck.shuffle(state.getRnd());
            dealTableAndHands(state);
        }
    }

    private void dealTableAndHands(ScopaGameState state) {
        ScopaParameters params = (ScopaParameters) state.getGameParameters();
        for (int n = 0; n < params.tableSize; n++)
            state.table.add(state.drawDeck.draw());
        dealHands(state);
    }

    /**
     * Moves every card on the table, in the hands and in the captured piles back into the draw deck.
     */
    private void gatherIntoDrawDeck(ScopaGameState state) {
        List<Deck<TarotCard>> decks = new ArrayList<>(state.playerHands);
        decks.addAll(state.capturedCards);
        decks.add(state.table);
        for (Deck<TarotCard> deck : decks) {
            state.drawDeck.add(deck);
            deck.clear();
        }
    }

    /**
     * Deals a new hand to each player from the draw deck, starting with the player after the dealer.
     */
    void dealHands(ScopaGameState state) {
        ScopaParameters params = (ScopaParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        int dealer = state.getDealer();
        for (int i = 1; i <= nPlayers; i++)
            for (int n = 0; n < params.handSize; n++)
                state.playerHands.get((dealer + i) % nPlayers).add(state.drawDeck.draw());
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ScopaGameState state = (ScopaGameState) gameState;
        List<TarotCard> table = state.table.getComponents();
        List<AbstractAction> actions = new ArrayList<>();
        for (TarotCard card : state.getPlayerHand(state.getCurrentPlayer()).getComponents()) {
            // capture is compulsory: a card goes to the table only if it captures nothing
            List<List<TarotCard>> captures = ScopaUtils.captures(card, table);
            if (captures.isEmpty())
                actions.add(new PlayCard(card));
            for (List<TarotCard> captured : captures)
                actions.add(new PlayCard(card, captured));
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        ScopaGameState state = (ScopaGameState) currentState;
        if (state.playerHands.stream().anyMatch(h -> h.getSize() > 0)) {
            endPlayerTurn(state);
            return;
        }
        int firstToPlay = (state.getDealer() + 1) % state.getNPlayers();
        if (state.drawDeck.getSize() > 0) {
            dealHands(state);
            endPlayerTurn(state, firstToPlay);
            return;
        }
        // the end of the deal: the last player to capture takes the cards left on the table
        if (state.lastCapturer >= 0) {
            state.capturedCards.get(state.lastCapturer).add(state.table);
            state.table.clear();
        }
        if (isGameOver(state)) {
            endGame(state);
            return;
        }
        // bank the deal's points and gather the cards, then the deal passes to the other player, and the old dealer
        // plays first
        for (int p = 0; p < state.getNPlayers(); p++)
            state.bankedScores[p] += state.getDealScore(p);
        gatherIntoDrawDeck(state);
        Arrays.fill(state.scopas, 0);
        endRound(state, state.getDealer());
        if (state.isNotTerminal())  // endRound ends the game at the framework's maxRounds
            deal(state);
    }

    /**
     * Whether the game ends with the deal just finished.
     */
    private boolean isGameOver(ScopaGameState state) {
        int target = ((ScopaParameters) state.getGameParameters()).targetScore;
        if (target == 0)  // a single deal
            return true;
        // otherwise a total must have reached the target, with no other total equal to it or higher
        double best = -1;
        boolean tied = false;
        for (int p = 0; p < state.getNPlayers(); p++) {
            double total = state.getGameScore(p);
            if (total > best) {
                best = total;
                tied = false;
            } else if (total == best) {
                tied = true;
            }
        }
        return best >= target && !tied;
    }
}
