package games.euchre;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.euchre.actions.CallTrump;
import games.euchre.actions.Discard;
import games.euchre.actions.Pass;
import games.tricktaking.KnownVoids;
import games.tricktaking.PlayCard;
import games.tricktaking.PlayRule;
import games.tricktaking.Trick;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.CoreConstants.GameResult.*;
import static core.CoreConstants.VisibilityMode.*;

/**
 * <p>The forward model contains all the game rules and logic for Euchre.</p>
 */
public class EuchreForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        EuchreGameState state = (EuchreGameState) firstState;
        EuchreParameters params = (EuchreParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();

        state.playerHands = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.playerHands.add(new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER));
        state.kitty = new Deck<>("Kitty", HIDDEN_TO_ALL);
        // the 24 cards from 9 to Ace start in the discard pile, and are gathered from there for the deal
        state.discardPile = new Deck<>("DiscardPile", VISIBLE_TO_ALL);
        for (FrenchCard card : FrenchCard.generateDeck("Deck", VISIBLE_TO_ALL).getComponents())
            if (card.number >= params.lowestCard)
                state.discardPile.add(card);
        state.teamPoints = new int[state.getNTeams()];
        state.knownVoids = new KnownVoids(nPlayers);
        state.currentTrick = new Trick("CurrentTrick", nPlayers, 0);

        // the last player deals first, so player 0 (on the dealer's left) is the first to decide on trumps
        deal(state);
    }

    /**
     * Gathers all the cards, shuffles them, and deals EuchreParameters.handSize to each player, one at a time starting
     * on the dealer's left. The rest go face down to the kitty, and its top card is turned up as the up-card. The
     * player on the dealer's left is then the first to decide on trumps.
     */
    void deal(EuchreGameState state) {
        EuchreParameters params = (EuchreParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        int dealer = state.getDealer();
        Deck<FrenchCard> deck = new Deck<>("Deck", HIDDEN_TO_ALL);
        for (Deck<FrenchCard> hand : state.playerHands) {
            deck.add(hand);
            hand.clear();
        }
        deck.add(state.kitty);
        state.kitty.clear();
        deck.add(state.currentTrick);
        deck.add(state.discardPile);
        state.discardPile.clear();
        deck.shuffle(state.getRnd());

        for (int i = 0; i < params.handSize * nPlayers; i++)
            state.playerHands.get((dealer + 1 + i) % nPlayers).add(deck.draw());
        state.kitty.add(deck);
        state.upCard = state.kitty.peek();

        state.dealerDiscard = null;
        state.trumpSuit = null;
        state.maker = -1;
        state.alone = false;
        state.passes = 0;
        state.tricksTaken = new int[nPlayers];
        state.knownVoids.clear();
        state.currentTrick = new Trick("CurrentTrick", nPlayers, (dealer + 1) % nPlayers);
        state.setFirstPlayer((dealer + 1) % nPlayers);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        EuchreGameState state = (EuchreGameState) gameState;
        EuchreParameters params = (EuchreParameters) state.getGameParameters();
        int player = state.getCurrentPlayer();
        List<AbstractAction> actions = new ArrayList<>();
        if (state.trumpSuit == null) {
            // choosing trumps: Pass, or call a suit with or without going alone
            FrenchCard.Suite upSuit = state.upCard.suite;
            if (state.isFirstBiddingRound()) {
                // only the up-card's suit may be called
                actions.add(new Pass());
                addCalls(actions, upSuit);
            } else {
                // any other suit may be called, and the dealer may not pass
                if (player != state.getDealer())
                    actions.add(new Pass());
                for (FrenchCard.Suite suit : FrenchCard.Suite.values())
                    if (suit != upSuit)
                        addCalls(actions, suit);
            }
        } else if (state.getPlayerHand(state.getDealer()).getSize() > params.handSize) {
            // the dealer has taken the up-card, and discards any card from their hand
            for (FrenchCard card : state.getPlayerHand(state.getDealer()).getComponents())
                actions.add(new Discard(card));
        } else {
            // play: any card the current player may legally play to the trick
            List<FrenchCard> hand = state.getPlayerHand(player).getComponents();
            for (FrenchCard card : PlayRule.FOLLOW_SUIT.legalPlays(hand, state.currentTrick))
                actions.add(new PlayCard(card));
        }
        return actions;
    }

    private static void addCalls(List<AbstractAction> actions, FrenchCard.Suite suit) {
        actions.add(new CallTrump(suit, false));
        actions.add(new CallTrump(suit, true));
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        EuchreGameState state = (EuchreGameState) currentState;
        EuchreParameters params = (EuchreParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        if (actionTaken instanceof Pass) {
            endPlayerTurn(state, (state.getCurrentPlayer() + 1) % nPlayers);
        } else if (actionTaken instanceof CallTrump || actionTaken instanceof Discard) {
            // a dealer who has just taken the up-card discards before play starts
            if (state.getPlayerHand(state.getDealer()).getSize() > params.handSize)
                endPlayerTurn(state, state.getDealer());
            else
                startPlay(state);
        } else {
            afterPlay(state);
        }
    }

    /**
     * The player on the dealer's left leads the first trick, or if the maker is going alone, the player on the
     * maker's left.
     */
    private void startPlay(EuchreGameState state) {
        int leader = ((state.alone ? state.maker : state.getDealer()) + 1) % state.getNPlayers();
        state.currentTrick = newTrick(state, leader);
        endPlayerTurn(state, leader);
    }

    /**
     * Passes the turn on within a trick. Once the trick is complete, its winner takes it and leads the next; after
     * the last trick the deal is scored, and either the game ends or the next deal starts.
     */
    private void afterPlay(EuchreGameState state) {
        EuchreParameters params = (EuchreParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        Trick trick = state.currentTrick;
        if (!trick.isComplete()) {
            endPlayerTurn(state, trick.playerOf(trick.getSize()));
            return;
        }
        int winner = trick.winner(state.trumpSuit);
        state.tricksTaken[winner]++;
        state.discardPile.add(trick);
        state.currentTrick = newTrick(state, winner);
        if (Arrays.stream(state.tricksTaken).sum() < params.handSize) {
            endPlayerTurn(state, winner);
            return;
        }
        scoreDeal(state);
        if (Arrays.stream(state.teamPoints).anyMatch(p -> p >= params.targetScore)) {
            endGame(state);
        } else {
            // the deal passes to the left
            endRound(state, (state.getDealer() + 2) % nPlayers);
            if (state.isNotTerminal())  // endRound ends the game at the framework's maxRounds
                deal(state);
        }
    }

    private static Trick newTrick(EuchreGameState state, int leader) {
        return new Trick("CurrentTrick", state.getNPlayers(), leader, state.getCardOrder(), state.getSittingOut());
    }

    private void scoreDeal(EuchreGameState state) {
        EuchreParameters params = (EuchreParameters) state.getGameParameters();
        int makers = state.getTeam(state.maker);
        int tricks = state.getTeamTricks(makers);
        if (tricks == params.handSize)
            // a march: the makers took all 5 tricks, scoring more if the maker went alone
            state.teamPoints[makers] += state.alone ? params.pointsAloneMarch : params.pointsMarch;
        else if (tricks * 2 > params.handSize)
            // the makers took 3 or 4 tricks
            state.teamPoints[makers] += params.pointsMade;
        else
            // euchred: the makers took fewer than 3, and the defenders score
            state.teamPoints[1 - makers] += params.pointsEuchred;
    }

    /**
     * Both partners of the team with more points win, and the other team loses; equal points is a draw.
     */
    @Override
    protected void endGame(AbstractGameState gs) {
        EuchreGameState state = (EuchreGameState) gs;
        state.setGameStatus(GAME_END);
        int difference = state.getTeamPoints(0) - state.getTeamPoints(1);
        for (int p = 0; p < state.getNPlayers(); p++) {
            int lead = state.getTeam(p) == 0 ? difference : -difference;
            state.setPlayerResult(lead > 0 ? WIN_GAME : lead < 0 ? LOSE_GAME : DRAW_GAME, p);
        }
    }
}
