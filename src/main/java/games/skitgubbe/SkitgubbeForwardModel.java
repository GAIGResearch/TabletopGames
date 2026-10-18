package games.skitgubbe;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.skitgubbe.actions.PickUp;
import games.skitgubbe.actions.PlayCard;
import games.skitgubbe.actions.TurnUpCard;

import java.util.ArrayList;
import java.util.List;

public class SkitgubbeForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        SkitgubbeGameState state = (SkitgubbeGameState) firstState;
        SkitgubbeParameters params = (SkitgubbeParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        state.playerHands = new ArrayList<>();
        state.collectedCards = new ArrayList<>();
        state.heldCards = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++) {
            state.playerHands.add(new Deck<>("Player" + p + "Hand", p, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            state.collectedCards.add(new Deck<>("Player" + p + "Collected", p, CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
            state.heldCards.add(new Deck<>("Player" + p + "Held", p, CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
        }
        // no one owns the trump card until it is drawn
        state.trumpCard = new Deck<>("TrumpCard", -1, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        state.trick = new Deck<>("Trick", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        state.discardPile = new Deck<>("DiscardPile", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        state.trumpSuit = null;
        state.trumpPlayer = -1;
        state.trickSize = 0;
        state.exitScores = new int[nPlayers];
        state.exitActions = new int[nPlayers];
        state.phaseTwoActions = 0;

        state.drawDeck = FrenchCard.generateDeck("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        state.drawDeck.shuffle(state.getRnd());
        for (int i = 0; i < params.handSize; i++)
            for (int p = 0; p < nPlayers; p++)
                state.playerHands.get(p).add(state.drawDeck.draw());

        state.setGamePhase(SkitgubbeGameState.Phase.PHASE_ONE);
        state.setFirstPlayer(0);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        SkitgubbeGameState state = (SkitgubbeGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        if (state.getGamePhase() == SkitgubbeGameState.Phase.PHASE_ONE) {
            // any card in hand, whether leading or following
            for (FrenchCard card : state.getPlayerHand(state.getCurrentPlayer()).getComponents())
                actions.add(new PlayCard(card));
            // or turn up the top card of the draw deck, unless it is the last card (the trump card)
            if (state.drawDeck.getSize() > 1)
                actions.add(new TurnUpCard());
        } else {
            Deck<FrenchCard> collected = state.getCollectedCards(state.getCurrentPlayer());
            if (state.trick.getSize() == 0) {
                // lead any card
                for (FrenchCard card : collected.getComponents())
                    actions.add(new PlayCard(card));
            } else {
                // beat the top card of the trick, or pick it up
                FrenchCard top = state.trick.peek();
                for (FrenchCard card : collected.getComponents())
                    if (SkitgubbeUtils.beats(card, top, state.trumpSuit))
                        actions.add(new PlayCard(card));
                actions.add(new PickUp());
            }
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        SkitgubbeGameState state = (SkitgubbeGameState) currentState;
        if (state.getGamePhase() == SkitgubbeGameState.Phase.PHASE_ONE)
            afterPhaseOnePlay(state);
        else
            afterPhaseTwoAction(state);
    }

    /**
     * Refills the hand of the player who has just played, and resolves the trick once it holds two cards.
     */
    private void afterPhaseOnePlay(SkitgubbeGameState state) {
        SkitgubbeParameters params = (SkitgubbeParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        int player = state.getCurrentPlayer();
        Deck<FrenchCard> hand = state.playerHands.get(player);
        if (state.drawDeck.getSize() > 0 && hand.getSize() < params.handSize) {
            if (state.drawDeck.getSize() == 1) {
                // the last card of the draw deck is the trump card, kept apart from the hand
                state.trumpCard.setOwnerId(player);
                state.trumpCard.add(state.drawDeck.draw());
                state.trumpPlayer = player;
            } else {
                hand.add(state.drawDeck.draw());
            }
        }

        int next;
        if (state.trick.getSize() == 1) {
            // the leader has played; the player after them follows
            next = (player + 1) % nPlayers;
        } else {
            // the follower has played: the leader's card is at the bottom of the trick
            int leader = (player + nPlayers - 1) % nPlayers;
            FrenchCard followerCard = state.trick.get(0);
            FrenchCard leaderCard = state.trick.get(1);
            if (leaderCard.number == followerCard.number) {
                // a bounce: the cards wait for the winner of the next trick, which the same leader leads
                state.heldCards.get(leader).add(leaderCard);
                state.heldCards.get(player).add(followerCard);
                state.trick.clear();
                next = leader;
            } else {
                int winner = leaderCard.number > followerCard.number ? leader : player;
                Deck<FrenchCard> won = state.collectedCards.get(winner);
                won.add(state.trick);
                state.trick.clear();
                for (Deck<FrenchCard> held : state.heldCards) {
                    won.add(held);
                    held.clear();
                }
                next = winner;
            }
        }
        // phase one ends when the player due to play has no cards (the draw deck is then empty)
        if (state.playerHands.get(next).getSize() == 0)
            startPhaseTwo(state);
        else
            endPlayerTurn(state, next);
    }

    private void startPhaseTwo(SkitgubbeGameState state) {
        int nPlayers = state.getNPlayers();
        // a card on the trick is an unanswered lead by the player who has just played
        if (state.trick.getSize() > 0) {
            state.collectedCards.get(state.getCurrentPlayer()).add(state.trick);
            state.trick.clear();
        }
        // every player's cards join their collected cards, which they must get rid of in phase two
        for (int p = 0; p < nPlayers; p++) {
            Deck<FrenchCard> collected = state.collectedCards.get(p);
            collected.add(state.heldCards.get(p));
            state.heldCards.get(p).clear();
            collected.add(state.playerHands.get(p));
            state.playerHands.get(p).clear();
        }
        FrenchCard trump = state.trumpCard.draw();
        state.trumpSuit = trump.suite;
        state.collectedCards.get(state.trumpPlayer).add(trump);

        state.setGamePhase(SkitgubbeGameState.Phase.PHASE_TWO);
        state.trickSize = holders(state);
        // a player who has no cards takes no part in phase two, and scores as if out in its first trick
        for (int p = 0; p < nPlayers; p++)
            if (state.collectedCards.get(p).getSize() == 0)
                state.exitScores[p] = state.trickSize;
        endPlayerTurn(state, state.trumpPlayer);
    }

    private void afterPhaseTwoAction(SkitgubbeGameState state) {
        SkitgubbeParameters params = (SkitgubbeParameters) state.getGameParameters();
        int player = state.getCurrentPlayer();
        state.phaseTwoActions++;
        boolean out = state.collectedCards.get(player).getSize() == 0;
        if (out) {
            // RECYCLE: the number of players holding cards when the trick began
            state.exitScores[player] = state.trickSize;
            state.exitActions[player] = state.phaseTwoActions;
        }
        boolean completed = state.trick.getSize() == state.trickSize;
        if (completed) {
            state.discardPile.add(state.trick);
            state.trick.clear();
        }
        int holders = holders(state);
        if (state.trick.getSize() == 0)
            state.trickSize = holders;
        // checked after every action, so the game can end with a trick unfinished
        if (holders <= 1 || state.phaseTwoActions >= params.maxPhaseTwoActions) {
            endGame(state);
            return;
        }
        // SkitgubbeParameters.completerLeads: the player who completed the trick leads the next, unless they are out
        if (completed && params.completerLeads && !out)
            endPlayerTurn(state, player);
        else
            endPlayerTurn(state, nextHolder(state, player));
    }

    /**
     * @return the number of players holding cards in phase two
     */
    private int holders(SkitgubbeGameState state) {
        int holders = 0;
        for (Deck<FrenchCard> collected : state.collectedCards)
            if (collected.getSize() > 0) holders++;
        return holders;
    }

    /**
     * @return the first player clockwise after the given one who holds cards in phase two
     */
    private int nextHolder(SkitgubbeGameState state, int player) {
        int next = (player + 1) % state.getNPlayers();
        while (state.collectedCards.get(next).getSize() == 0)
            next = (next + 1) % state.getNPlayers();
        return next;
    }
}
