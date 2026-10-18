package games.sueca;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.tricktaking.KnownVoids;
import games.tricktaking.PlayCard;
import games.tricktaking.PlayRule;
import games.tricktaking.Trick;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.GameResult.*;
import static core.CoreConstants.VisibilityMode.*;

public class SuecaForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        SuecaGameState state = (SuecaGameState) firstState;
        int nPlayers = state.getNPlayers();

        state.playerHands = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.playerHands.add(new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER));
        state.teamPiles = new ArrayList<>();
        for (int t = 0; t < state.getNTeams(); t++)
            state.teamPiles.add(new Deck<>("Team " + t + " Pile", VISIBLE_TO_ALL));
        state.knownVoids = new KnownVoids<>(nPlayers, FrenchCard.Suite.class);
        state.teamGames = new int[state.getNTeams()];
        state.extraGames = 0;

        deal(state,SuecaUtils.newPack("Pack", HIDDEN_TO_ALL));
        state.setFirstPlayer(state.getFirstLeader());
    }

    /**
     * Deals the pack for a new deal. The hands, the current trick and the team piles must be empty.
     */
    void deal(SuecaGameState state, Deck<FrenchCard> pack) {
        SuecaParameters params = (SuecaParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        pack.shuffle(state.getRnd());

        // each player in turn receives handSize cards in one batch, starting with the first leader and ending
        // with the dealer
        for (int i = 0; i < nPlayers; i++) {
            Deck<FrenchCard> hand = state.playerHands.get((state.getFirstLeader() + i) % nPlayers);
            for (int c = 0; c < params.handSize; c++)
                hand.add(pack.draw());
        }
        // the last card dealt (the bottom of the pack, which goes to the dealer) sets trumps; Deck.add puts it at
        // index 0 of the dealer's hand
        state.trumpCard = state.playerHands.get(state.getDealer()).get(0);
        state.currentTrick = new Trick<>("CurrentTrick", nPlayers, state.getFirstLeader(), SuecaUtils.CARD_ORDER);
        state.knownVoids.clear();
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        SuecaGameState state = (SuecaGameState) gameState;
        // one PlayCard for each card the current player may play: follow the suit led if able, otherwise anything
        List<FrenchCard> hand = state.getPlayerHand(state.getCurrentPlayer()).getComponents();
        List<AbstractAction> actions = new ArrayList<>();
        for (FrenchCard card : PlayRule.FOLLOW_SUIT.legalPlays(hand, state.currentTrick))
            actions.add(new PlayCard<>(card));
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        SuecaGameState state = (SuecaGameState) currentState;
        Trick<FrenchCard, FrenchCard.Suite> trick = state.currentTrick;
        if (!trick.isComplete()) {
            endPlayerTurn(state);
            return;
        }
        // the winner's team takes the trick, and the winner leads the next one
        int winner = trick.winner(state.getTrumpSuit());
        state.teamPiles.get(state.getTeam(winner)).add(trick);
        state.currentTrick = new Trick<>("CurrentTrick", state.getNPlayers(), winner, SuecaUtils.CARD_ORDER);
        if (!state.playerHands.stream().allMatch(h -> h.getSize() == 0)) {
            endPlayerTurn(state, winner);
            return;
        }
        SuecaParameters params = (SuecaParameters) state.getGameParameters();
        if (!params.playRubber) {
            endGame(state);
            return;
        }
        scoreDeal(state);
        if (state.teamGames[0] >= params.targetGames || state.teamGames[1] >= params.targetGames) {
            endGame(state);
        } else {
            // the deal passes to the next player, and the player after the new dealer leads
            endRound(state, (state.getFirstLeader() + 1) % state.getNPlayers());
            if (state.isNotTerminal())  // endRound ends the game at the framework's maxRounds
                deal(state, gatherCards(state));
        }
    }

    /**
     * Scores a finished deal in the rubber (SuecaParameters.playRubber).
     */
    void scoreDeal(SuecaGameState state) {
        // the team with more than 60 card points scores its games and any extra games from tied deals
        for (int team = 0; team < state.getNTeams(); team++) {
            int points = state.getCardPoints(team);
            if (points > 60) {
                boolean allTricks = state.getTricksWon(team) == ((SuecaParameters) state.getGameParameters()).handSize;
                state.teamGames[team] += SuecaUtils.gamesForDeal(points, allTricks) + state.extraGames;
                state.extraGames = 0;
                return;
            }
        }
        // at 60-60 nobody scores, and the next deal is worth one more game
        state.extraGames++;
    }

    /**
     * Moves every card from the hands, the current trick and the team piles into a new pack.
     */
    private Deck<FrenchCard> gatherCards(SuecaGameState state) {
        Deck<FrenchCard> pack = new Deck<>("Pack", HIDDEN_TO_ALL);
        for (Deck<FrenchCard> hand : state.playerHands) {
            pack.add(hand);
            hand.clear();
        }
        pack.add(state.currentTrick);
        state.currentTrick.clear();
        for (Deck<FrenchCard> pile : state.teamPiles) {
            pack.add(pile);
            pile.clear();
        }
        return pack;
    }

    /**
     * Both partners of the team with the higher game score win and the other team loses; equal scores are a draw.
     */
    @Override
    protected void endGame(AbstractGameState gs) {
        SuecaGameState state = (SuecaGameState) gs;
        state.setGameStatus(GAME_END);
        double difference = state.getGameScore(0) - state.getGameScore(1);
        for (int p = 0; p < state.getNPlayers(); p++) {
            double lead = state.getTeam(p) == 0 ? difference : -difference;
            state.setPlayerResult(lead > 0 ? WIN_GAME : lead < 0 ? LOSE_GAME : DRAW_GAME, p);
        }
    }
}
