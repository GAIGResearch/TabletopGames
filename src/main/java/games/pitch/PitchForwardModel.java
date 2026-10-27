package games.pitch;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.pitch.actions.Bid;
import games.pitch.actions.Pass;
import games.pitch.actions.PlayCard;
import games.tricktaking.CardOrder;
import games.tricktaking.Trick;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PitchForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        PitchGameState state = (PitchGameState) firstState;
        int nPlayers = state.getNPlayers();
        state.playerHands = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++) {
            Deck<FrenchCard> hand = new Deck<>("Player" + p + "Hand", p, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
            state.playerHands.add(hand);
        }
        state.teamTricks = new ArrayList<>();
        for (int t = 0; t < 2; t++)
            state.teamTricks.add(new Deck<>("Team" + t + "Tricks", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
        // the same deck is refilled each deal, keeping its component ID so equal positions stay equal
        state.undealtDeck = new Deck<>("UndealtDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        Arrays.fill(state.teamScores, 0);
        deal(state);
    }

    void deal(PitchGameState state) {
        PitchParameters params = (PitchParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        for (Deck<FrenchCard> hand : state.playerHands)
            hand.clear();
        for (Deck<FrenchCard> tricks : state.teamTricks)
            tricks.clear();
        Arrays.fill(state.playerBids, -1);
        state.pitcher = -1;
        state.trumpSuit = null;
        state.knownVoids.clear();

        state.undealtDeck.clear();
        state.undealtDeck.add(FrenchCard.generateDeck("UndealtDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL));
        state.undealtDeck.shuffle(state.getRnd());
        int firstBidder = (state.getDealer() + 1) % nPlayers;
        for (int i = 0; i < params.handSize; i++)
            for (int p = 0; p < nPlayers; p++)
                state.playerHands.get((firstBidder + p) % nPlayers).add(state.undealtDeck.draw());

        state.currentTrick = new Trick<>("CurrentTrick", nPlayers, firstBidder, CardOrder.STANDARD);
        state.setGamePhase(PitchGameState.Phase.BIDDING);
        state.setFirstPlayer(firstBidder);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        PitchGameState state = (PitchGameState) gameState;
        return state.getGamePhase() == PitchGameState.Phase.BIDDING ? bidActions(state) : playActions(state);
    }

    private List<AbstractAction> bidActions(PitchGameState state) {
        PitchParameters params = (PitchParameters) state.getGameParameters();
        boolean isDealer = state.getCurrentPlayer() == state.getDealer();
        List<AbstractAction> actions = new ArrayList<>();
        // the dealer must bid if everyone else has passed
        if (!isDealer || state.getPitcher() >= 0)
            actions.add(new Pass());
        // a bid must be higher than the highest so far, but the dealer may take the bid by equalling it
        int lowest = Math.max(params.minBid, isDealer ? state.getHighestBid() : state.getHighestBid() + 1);
        for (int amount = lowest; amount <= params.smudgeBid; amount++)
            actions.add(new Bid(amount));
        return actions;
    }

    private List<AbstractAction> playActions(PitchGameState state) {
        List<FrenchCard> hand = state.getPlayerHand(state.getCurrentPlayer()).getComponents();
        List<AbstractAction> actions = new ArrayList<>();
        for (FrenchCard card : PitchUtils.playRule(state.getTrumpSuit()).legalPlays(hand, state.getCurrentTrick()))
            actions.add(new PlayCard(card));
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        PitchGameState state = (PitchGameState) currentState;
        if (actionTaken instanceof PlayCard) {
            afterPlay(state);
        } else if (state.getCurrentPlayer() == state.getDealer()) {
            // the dealer bids last; the pitcher leads the first trick
            state.setGamePhase(PitchGameState.Phase.PLAYING);
            state.currentTrick = new Trick<>("CurrentTrick", state.getNPlayers(), state.getPitcher(), CardOrder.STANDARD);
            endPlayerTurn(state, state.getPitcher());
        } else {
            endPlayerTurn(state);
        }
    }

    private void afterPlay(PitchGameState state) {
        Trick<FrenchCard, FrenchCard.Suite> trick = state.getCurrentTrick();
        if (!trick.isComplete()) {
            endPlayerTurn(state);
            return;
        }
        int winner = trick.winner(state.getTrumpSuit());
        Deck<FrenchCard> won = state.getTeamTricks(state.getTeam(winner));
        for (FrenchCard card : trick.getComponents())
            won.add(card);
        state.currentTrick = new Trick<>("CurrentTrick", state.getNPlayers(), winner, CardOrder.STANDARD);
        if (state.getPlayerHand(winner).getSize() > 0)
            endPlayerTurn(state, winner);
        else
            endDeal(state);
    }

    /**
     * Scores the deal, then ends the game or starts the next deal.
     */
    private void endDeal(PitchGameState state) {
        PitchParameters params = (PitchParameters) state.getGameParameters();
        int[] change = scoreChanges(state);
        for (int team = 0; team < 2; team++)
            state.teamScores[team] += change[team];
        if (params.targetScore <= 1 || pitchersWentOut(state)) {
            endGame(state);
        } else {
            endRound(state);
            // endRound ends the game itself after maxRounds deals
            if (state.isNotTerminal())
                deal(state);
        }
    }

    /**
     * @return the change to each team's score from the deal just played
     */
    private int[] scoreChanges(PitchGameState state) {
        PitchParameters params = (PitchParameters) state.getGameParameters();
        int[] points = PitchUtils.teamPoints(state.teamTricks, state.getTrumpSuit(), params);
        int pitchingTeam = state.getTeam(state.getPitcher());
        int bid = state.getHighestBid();
        // each team scores its points, unless the pitching team fails its bid and loses the bid instead
        int[] change = points.clone();
        if (bid == params.smudgeBid) {
            // a smudge needs every trick and all four points
            boolean made = state.getTricksWon(pitchingTeam) == params.handSize && points[pitchingTeam] == 4;
            change[pitchingTeam] = made ? params.smudgePoints : -params.smudgePoints;
        } else if (points[pitchingTeam] < bid) {
            change[pitchingTeam] = -bid;
        }
        return change;
    }

    /**
     * @return true if the game is played to PitchParameters.targetScore and the pitching team has just gone out
     */
    private boolean pitchersWentOut(PitchGameState state) {
        PitchParameters params = (PitchParameters) state.getGameParameters();
        // the cards of a finished deal stay in place when the game ends, so this can be checked again in endGame
        boolean dealFinished = state.getPitcher() >= 0 && state.getPlayerHands().stream().allMatch(h -> h.getSize() == 0);
        if (params.targetScore <= 1 || !dealFinished) return false;
        int pitchingTeam = state.getTeam(state.getPitcher());
        // only the pitching team can go out, at the end of a deal in which it made its bid
        return scoreChanges(state)[pitchingTeam] > 0 && state.getTeamScore(pitchingTeam) >= params.targetScore;
    }

    /**
     * Both partners of the winning team win; on equal scores all four draw.
     */
    @Override
    protected void endGame(AbstractGameState gs) {
        PitchGameState state = (PitchGameState) gs;
        state.setGameStatus(CoreConstants.GameResult.GAME_END);
        // the pitching team wins by going out, whatever the other team's score; otherwise (a single deal, or the
        // limit on the number of deals) the higher score wins
        int winningTeam;
        if (pitchersWentOut(state))
            winningTeam = state.getTeam(state.getPitcher());
        else if (state.getTeamScore(0) == state.getTeamScore(1))
            winningTeam = -1;
        else
            winningTeam = state.getTeamScore(0) > state.getTeamScore(1) ? 0 : 1;
        for (int p = 0; p < state.getNPlayers(); p++) {
            CoreConstants.GameResult result;
            if (winningTeam < 0) result = CoreConstants.GameResult.DRAW_GAME;
            else result = state.getTeam(p) == winningTeam ? CoreConstants.GameResult.WIN_GAME : CoreConstants.GameResult.LOSE_GAME;
            state.setPlayerResult(result, p);
        }
    }
}
