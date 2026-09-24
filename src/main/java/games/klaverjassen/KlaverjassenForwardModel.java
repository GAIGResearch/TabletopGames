package games.klaverjassen;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.klaverjassen.actions.ChooseTrump;
import games.tricktaking.KnownVoids;
import games.tricktaking.PlayCard;
import games.tricktaking.Trick;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.GameResult.*;
import static core.CoreConstants.VisibilityMode.*;

public class KlaverjassenForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        KlaverjassenGameState state = (KlaverjassenGameState) firstState;
        int nPlayers = state.getNPlayers();

        state.playerHands = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.playerHands.add(new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER));
        // the 32-card piquet pack (Seven to Ace in each suit) starts in the discard pile, and is gathered from there
        // for the deal
        state.discardPile = FrenchCard.generateDeck("DiscardPile", VISIBLE_TO_ALL);
        state.discardPile.getComponents().removeIf(c -> c.number < 7);
        state.teamScores = new int[state.getNTeams()];
        state.knownVoids = new KnownVoids(nPlayers);
        state.currentTrick = new Trick("CurrentTrick", nPlayers, 0, new KlaverjassenCardOrder(null));

        // the last player deals first, so player 0 (on the dealer's left) chooses trumps and leads
        deal(state);
        state.setFirstPlayer(state.getTrumpChooser());
    }

    /**
     * Deals a new hand, clearing the trump suit, the hand's points, roem and tricks, and the known voids.
     */
    void deal(KlaverjassenGameState state) {
        KlaverjassenParameters params = (KlaverjassenParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        Deck<FrenchCard> deck = new Deck<>("Deck", HIDDEN_TO_ALL);
        for (Deck<FrenchCard> hand : state.playerHands) {
            deck.add(hand);
            hand.clear();
        }
        deck.add(state.currentTrick);
        deck.add(state.discardPile);
        state.discardPile.clear();
        deck.shuffle(state.getRnd());

        // one card at a time, starting on the dealer's left
        for (int i = 0; i < params.handSize * nPlayers; i++)
            state.playerHands.get((state.getTrumpChooser() + i) % nPlayers).add(deck.draw());
        // with the default hand size nothing is left over; keep any remainder out of play in the discard pile
        state.discardPile.add(deck);

        state.trumpSuit = null;
        state.currentTrick = new Trick("CurrentTrick", nPlayers, state.getTrumpChooser(), new KlaverjassenCardOrder(null));
        state.handPoints = new int[state.getNTeams()];
        state.handRoem = new int[state.getNTeams()];
        state.tricksWon = new int[state.getNTeams()];
        state.knownVoids.clear();
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        KlaverjassenGameState state = (KlaverjassenGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        // the trump chooser must choose one of the four suits before any card is played
        if (state.trumpSuit == null) {
            for (FrenchCard.Suite suit : FrenchCard.Suite.values())
                actions.add(new ChooseTrump(suit));
            return actions;
        }
        // then one PlayCard for each card the current player may play
        List<FrenchCard> hand = state.getPlayerHand(state.getCurrentPlayer()).getComponents();
        KlaverjassenParameters params = (KlaverjassenParameters) state.getGameParameters();
        for (FrenchCard card : KlaverjassenUtils.legalPlays(hand, state.currentTrick, state.trumpSuit,
                params.partnerTrumpRule))
            actions.add(new PlayCard(card));
        return actions;
    }

    @Override
    protected void _beforeAction(AbstractGameState currentState, AbstractAction actionChosen) {
        if (actionChosen instanceof PlayCard play)
            inferTrumpVoid((KlaverjassenGameState) currentState, play.card);
    }

    /**
     * Records a void in trumps if the card the current player is about to play shows they hold none.
     */
    void inferTrumpVoid(KlaverjassenGameState state, FrenchCard card) {
        // they hold none if they cannot follow suit, an opponent is winning with a card that is not a trump, and the
        // card is not a trump either (they would have had to trump)
        Trick trick = state.currentTrick;
        if (!((KlaverjassenParameters) state.getGameParameters()).rememberVoids || trick.getSize() == 0
                || card.suite == trick.getLeadSuit() || card.suite == state.trumpSuit)
            return;
        int player = state.getCurrentPlayer();
        int winner = trick.winner(state.trumpSuit);
        boolean trumped = trick.getComponents().stream().anyMatch(c -> c.suite == state.trumpSuit);
        // with no trump in the trick, the winning card is not a trump
        if (state.getTeam(winner) != state.getTeam(player) && !trumped)
            state.knownVoids.get(player).add(state.trumpSuit);
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        if (actionTaken instanceof ChooseTrump)
            return;  // the trump chooser leads the first trick
        KlaverjassenGameState state = (KlaverjassenGameState) currentState;
        Trick trick = state.currentTrick;
        if (!trick.isComplete()) {
            endPlayerTurn(state);
            return;
        }
        // the winner of the trick takes it and leads the next one
        int winner = trick.winner(state.trumpSuit);
        scoreTrick(state, trick, winner);
        state.discardPile.add(trick);
        state.currentTrick = new Trick("CurrentTrick", state.getNPlayers(), winner, trick.getOrder());
        if (!handOver(state)) {
            endPlayerTurn(state, winner);
            return;
        }
        scoreHand(state);
        KlaverjassenParameters params = (KlaverjassenParameters) state.getGameParameters();
        if (state.getRoundCounter() + 1 >= params.nHands) {
            endGame(state);
        } else {
            // the deal passes to the left, and the player on the new dealer's left chooses trumps
            endRound(state, (state.getTrumpChooser() + 1) % state.getNPlayers());
            if (state.isNotTerminal())  // endRound ends the game at the framework's maxRounds
                deal(state);
        }
    }

    private boolean handOver(KlaverjassenGameState state) {
        return state.playerHands.stream().allMatch(h -> h.getSize() == 0);
    }

    /**
     * Credits the completed trick's card points, roem and the trick itself to the winner's team.
     */
    void scoreTrick(KlaverjassenGameState state, Trick trick, int winner) {
        KlaverjassenParameters params = (KlaverjassenParameters) state.getGameParameters();
        int team = state.getTeam(winner);
        for (FrenchCard card : trick.getComponents())
            state.handPoints[team] += params.cardPoints(card, state.trumpSuit);
        // the last trick of the hand scores a bonus
        if (handOver(state))
            state.handPoints[team] += params.lastTrickBonus;
        state.handRoem[team] += KlaverjassenUtils.roem(trick.getComponents(), state.trumpSuit, params);
        state.tricksWon[team]++;
    }

    /**
     * Adds the hand's points, including the pit bonus, to the teams' scores, applying the nat.
     */
    void scoreHand(KlaverjassenGameState state) {
        KlaverjassenParameters params = (KlaverjassenParameters) state.getGameParameters();
        // a team that took every trick gains the pit bonus as roem
        for (int team = 0; team < state.getNTeams(); team++) {
            if (state.tricksWon[team] == params.handSize)
                state.handRoem[team] += params.pitBonus;
        }
        int trumpTeam = state.getTeam(state.getTrumpChooser());
        int opponents = 1 - trumpTeam;
        int trumpTotal = state.handPoints[trumpTeam] + state.handRoem[trumpTeam];
        int opponentsTotal = state.handPoints[opponents] + state.handRoem[opponents];
        // the nat: if the trump chooser's team has fewer points (or equal points, with
        // KlaverjassenParameters.tieIsFailure), the opponents score both teams' points and the chooser's team none
        if (trumpTotal < opponentsTotal || (params.tieIsFailure && trumpTotal == opponentsTotal)) {
            state.teamScores[opponents] += trumpTotal + opponentsTotal;
        } else {
            state.teamScores[trumpTeam] += trumpTotal;
            state.teamScores[opponents] += opponentsTotal;
        }
    }

    /**
     * Both partners of the team with more points win, and the other team loses; equal points is a draw.
     */
    @Override
    protected void endGame(AbstractGameState gs) {
        KlaverjassenGameState state = (KlaverjassenGameState) gs;
        state.setGameStatus(GAME_END);
        int difference = state.getTeamScore(0) - state.getTeamScore(1);
        for (int p = 0; p < state.getNPlayers(); p++) {
            int lead = state.getTeam(p) == 0 ? difference : -difference;
            state.setPlayerResult(lead > 0 ? WIN_GAME : lead < 0 ? LOSE_GAME : DRAW_GAME, p);
        }
    }
}
