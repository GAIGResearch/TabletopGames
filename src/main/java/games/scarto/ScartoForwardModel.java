package games.scarto;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.TarotCard;
import games.tricktaking.KnownVoids;
import games.tricktaking.PlayCard;
import games.tricktaking.Trick;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.VisibilityMode.*;

public class ScartoForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        ScartoGameState state = (ScartoGameState) firstState;
        int nPlayers = state.getNPlayers();

        state.playerHands = new ArrayList<>();
        state.cardsWon = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++) {
            state.playerHands.add(new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER));
            state.cardsWon.add(new Deck<>("Cards won " + p, p, VISIBLE_TO_ALL));
        }
        state.scarto = new Deck<>("Scarto", HIDDEN_TO_ALL);
        state.knownVoids = new KnownVoids<>(nPlayers, TarotCard.Suit.class);
        state.bankedScores = new int[nPlayers];
        // the last player deals first, so player 0, next after the dealer, leads the first trick
        state.setFirstPlayer(0);
        deal(state);
    }

    /**
     * Clears away the last deal, if any, and deals afresh.
     */
    void deal(ScartoGameState state) {
        ScartoParameters params = (ScartoParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        int dealer = state.getDealer();
        Deck<TarotCard> pack = TarotCard.generateDeck("Pack", HIDDEN_TO_ALL);
        pack.shuffle(state.getRnd());
        for (Deck<TarotCard> hand : state.playerHands)
            hand.clear();
        state.scarto.clear();
        for (Deck<TarotCard> won : state.cardsWon)
            won.clear();
        state.knownVoids.clear();
        // each player in turn, from the player after the dealer, is dealt their whole hand; the rest form the scarto
        for (int i = 1; i <= nPlayers; i++)
            for (int n = 0; n < params.handSize; n++)
                state.playerHands.get((dealer + i) % nPlayers).add(pack.draw());
        state.scarto.add(pack);
        state.currentTrick = new Trick<>("CurrentTrick", nPlayers, (dealer + 1) % nPlayers, ScartoCardOrder.INSTANCE);
        if (params.dealerExchange) {
            // the dealer takes the scarto into their hand, and will discard as many cards to it, which only they see
            state.scarto.setOwnerId(dealer);
            state.scarto.setVisibility(VISIBLE_TO_OWNER);
            state.playerHands.get(dealer).add(state.scarto);
            state.scarto.clear();
            state.setTurnOwner(dealer);
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ScartoGameState state = (ScartoGameState) gameState;
        List<TarotCard> hand = state.getPlayerHand(state.getCurrentPlayer()).getComponents();
        List<AbstractAction> actions = new ArrayList<>();
        if (state.isExchanging()) {
            for (TarotCard card : ScartoUtils.discardable(hand))
                actions.add(new Discard(card));
            return actions;
        }
        for (TarotCard card : ScartoUtils.PLAY_RULE.legalPlays(hand, state.currentTrick))
            actions.add(new PlayCard<>(card));
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        ScartoGameState state = (ScartoGameState) currentState;
        Trick<TarotCard, TarotCard.Suit> trick = state.currentTrick;
        if (actionTaken instanceof Discard) {
            // the dealer discards until their hand is back to size; then the first trick's leader plays
            if (!state.isExchanging())
                endPlayerTurn(state, trick.getLeader());
            return;
        }
        if (!trick.isComplete()) {
            endPlayerTurn(state);
            return;
        }
        // the Fool goes back to the player who played it; the winner takes the other cards and leads the next trick
        int winner = trick.winner(TarotCard.Suit.Trumps);
        for (int i = 0; i < trick.getSize(); i++) {
            TarotCard card = trick.get(i);
            state.cardsWon.get(card.isFool() ? trick.playerOf(i) : winner).add(card);
        }
        state.currentTrick = new Trick<>("CurrentTrick", state.getNPlayers(), winner, ScartoCardOrder.INSTANCE);
        if (state.playerHands.stream().anyMatch(h -> h.getSize() > 0)) {
            endPlayerTurn(state, winner);
            return;
        }
        ScartoParameters params = (ScartoParameters) state.getGameParameters();
        if (state.getRoundCounter() + 1 >= params.nDeals) {
            endGame(state);
            return;
        }
        for (int p = 0; p < state.getNPlayers(); p++)
            state.bankedScores[p] += state.getDealScore(p);
        // the deal passes to the next player, and the player after the new dealer leads
        endRound(state, (state.getDealer() + 2) % state.getNPlayers());
        if (state.isNotTerminal())  // endRound ends the game at the framework's maxRounds
            deal(state);
    }
}
