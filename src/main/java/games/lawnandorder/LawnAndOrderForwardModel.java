package games.lawnandorder;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.lawnandorder.actions.Continue;
import games.lawnandorder.actions.Pass;
import games.lawnandorder.actions.PlayObject;
import games.lawnandorder.components.LawnCard;
import games.lawnandorder.components.RuleCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.CoreConstants.GameResult.*;
import static core.CoreConstants.VisibilityMode.*;
import static games.lawnandorder.LawnAndOrderGameState.*;

public class LawnAndOrderForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        LawnAndOrderGameState state = (LawnAndOrderGameState) firstState;
        int nPlayers = state.getNPlayers();
        state.drawDeck = new Deck<>("Draw deck", HIDDEN_TO_ALL);
        state.hands = new ArrayList<>();
        state.chosenCards = new ArrayList<>();
        state.lawns = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++) {
            state.hands.add(new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER));
            state.chosenCards.add(new Deck<>("Chosen card " + p, p, VISIBLE_TO_OWNER));
            state.lawns.add(new Deck<>("Lawn " + p, p, VISIBLE_TO_ALL));
        }
        state.discardDeck = new PartialObservableDeck<>("Discard deck", -1, nPlayers, HIDDEN_TO_ALL);
        state.agenda = new Deck<>("HOA Agenda", HIDDEN_TO_ALL);
        state.insiderTips = new PartialObservableDeck<>("Insider Tips", -1, nPlayers, HIDDEN_TO_ALL);
        state.revealedRules = new Deck<>("Revealed rules", VISIBLE_TO_ALL);
        state.citations = new int[nPlayers];
        state.status = new PlayerStatus[nPlayers];
        state.decisions = new Decision[nPlayers];
        state.goodwill = new boolean[nPlayers];
        state.trackScores = new int[nPlayers][LawnCard.Category.values().length];
        startRound(state);
    }

    /**
     * Starts a round: all the Lawn cards are shuffled and dealt, and all the Rule cards shuffled into a new HOA Agenda.
     */
    void startRound(LawnAndOrderGameState state) {
        LawnAndOrderParameters params = (LawnAndOrderParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();

        state.drawDeck.clear();
        state.drawDeck.add(LawnCard.allCards());
        state.drawDeck.shuffle(state.getRnd());
        for (int p = 0; p < nPlayers; p++) {
            state.hands.get(p).clear();
            state.chosenCards.get(p).clear();
            state.lawns.get(p).clear();
            for (int i = 0; i < params.handSize; i++)
                state.hands.get(p).add(state.drawDeck.draw());
        }
        state.discardDeck.clear();

        state.agenda.clear();
        state.agenda.add(params.ruleCards());
        state.agenda.shuffle(state.getRnd());
        // One Insider Tip is dealt into each gap between neighbours, so two players have two tips.
        // Tip i lies between players i and i+1, and only they may look at it.
        state.insiderTips.clear();
        for (int i = 0; i < nPlayers; i++)
            state.insiderTips.add(state.agenda.draw(), new boolean[nPlayers]);
        for (int i = 0; i < nPlayers; i++) {
            boolean[] neighbours = new boolean[nPlayers];
            neighbours[i] = true;
            neighbours[(i + 1) % nPlayers] = true;
            state.insiderTips.setVisibilityOfComponent(i, neighbours);
        }
        state.revealedRules.clear();

        Arrays.fill(state.citations, 0);
        Arrays.fill(state.status, PlayerStatus.ACTIVE);
        Arrays.fill(state.decisions, Decision.NONE);
        state.setGamePhase(Phase.PLAY_OBJECT);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return _computeAvailableActions(gameState, gameState.getCurrentPlayer());
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, int player) {
        LawnAndOrderGameState state = (LawnAndOrderGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        // nothing for a player who is not active, or has already chosen
        if (!state.getPlayersStillToChoose().contains(player))
            return actions;
        if (state.getGamePhase() == Phase.PLAY_OBJECT) {
            for (LawnCard card : state.getHand(player))
                actions.add(new PlayObject(player, card));
        } else {
            actions.add(new Continue(player));
            actions.add(new Pass(player));
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        LawnAndOrderGameState state = (LawnAndOrderGameState) currentState;
        // the step is resolved once every active player has chosen
        if (!state.getPlayersStillToChoose().isEmpty()) {
            endPlayerTurn(state, nextPlayerToChoose(state));
            return;
        }
        boolean roundOver = state.getGamePhase() == Phase.PLAY_OBJECT ? resolvePlay(state) : resolveDecisions(state);
        if (roundOver)
            endOfRound(state);
        else
            endPlayerTurn(state, state.getPlayersStillToChoose().get(0));
    }

    /**
     * The next player, cycling round from the current one, who is still to choose.
     */
    private int nextPlayerToChoose(LawnAndOrderGameState state) {
        List<Integer> toChoose = state.getPlayersStillToChoose();
        int current = state.getCurrentPlayer();
        for (int i = 1; i <= state.getNPlayers(); i++) {
            int p = (current + i) % state.getNPlayers();
            if (toChoose.contains(p))
                return p;
        }
        throw new AssertionError("No player is still to choose");
    }

    /**
     * Steps 1 to 5 of a turn, once every active player has chosen a card. Returns whether the round is over.
     */
    private boolean resolvePlay(LawnAndOrderGameState state) {
        int nPlayers = state.getNPlayers();
        for (int p = 0; p < nPlayers; p++) {
            if (state.status[p] != PlayerStatus.ACTIVE) continue;
            // Steps 1 and 2. This turn's Rule card is not yet revealed, so only the earlier ones cite the card.
            LawnCard card = state.chosenCards.get(p).draw();
            state.lawns.get(p).add(card);
            for (LawnCard.Category category : LawnCard.Category.values())
                if (state.isCondemned(card.get(category)))
                    state.citations[p]++;
        }
        // Steps 3 and 4
        revealRule(state);
        // Step 5
        for (int p = 0; p < nPlayers; p++)
            if (state.status[p] == PlayerStatus.ACTIVE && state.citations[p] > state.getCitationLimit(p))
                ceaseAndDesist(state, p);
        if (noActivePlayer(state) || state.agenda.getSize() == 0)
            return true;
        state.setGamePhase(Phase.CONTINUE_OR_PASS);
        return false;
    }

    /**
     * Reveals the top card of the agenda, and gives the retroactive Citations for an attribute it condemns (Step 4).
     * An Emergency Session reveals the next cards in the same way.
     */
    private void revealRule(LawnAndOrderGameState state) {
        LawnAndOrderParameters params = (LawnAndOrderParameters) state.getGameParameters();
        RuleCard rule = state.agenda.draw();
        state.revealedRules.add(rule);
        if (rule.special == RuleCard.Special.EMERGENCY_SESSION) {
            // LawnAndOrderParameters.emergencySessionReveals more cards, or all that remain if there are fewer
            for (int i = 0; i < params.emergencySessionReveals && state.agenda.getSize() > 0; i++)
                revealRule(state);
            return;
        }
        // Zero Tolerance acts through getCitationLimit, and an Administrative Error does nothing
        if (rule.condemned == null)
            return;
        for (int p = 0; p < state.getNPlayers(); p++) {
            // players who have passed are immune
            if (state.status[p] != PlayerStatus.ACTIVE) continue;
            for (LawnCard card : state.lawns.get(p))
                if (card.has(rule.condemned))
                    state.citations[p]++;
        }
    }

    /**
     * The player loses their lawn and hand for the rest of the round.
     */
    private void ceaseAndDesist(LawnAndOrderGameState state, int player) {
        int nPlayers = state.getNPlayers();
        boolean[] seenByAll = new boolean[nPlayers];
        Arrays.fill(seenByAll, true);
        boolean[] seenByOwner = new boolean[nPlayers];
        seenByOwner[player] = true;
        for (LawnCard card : state.lawns.get(player))
            state.discardDeck.add(card, seenByAll.clone());
        for (LawnCard card : state.hands.get(player))
            state.discardDeck.add(card, seenByOwner.clone());
        state.lawns.get(player).clear();
        state.hands.get(player).clear();
        state.status[player] = PlayerStatus.CEASE_AND_DESIST;
    }

    /**
     * Step 6, once every active player has decided whether to continue. Returns whether the round is over.
     */
    private boolean resolveDecisions(LawnAndOrderGameState state) {
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (state.status[p] != PlayerStatus.ACTIVE) continue;
            if (state.decisions[p] == Decision.CONTINUE && state.drawDeck.getSize() > 0)
                state.hands.get(p).add(state.drawDeck.draw());
            // a player with no card to play next turn passes instead
            if (state.decisions[p] == Decision.PASS || state.hands.get(p).getSize() == 0)
                state.status[p] = PlayerStatus.PASSED;
        }
        Arrays.fill(state.decisions, Decision.NONE);
        if (noActivePlayer(state))
            return true;
        state.setGamePhase(Phase.PLAY_OBJECT);
        return false;
    }

    private boolean noActivePlayer(LawnAndOrderGameState state) {
        for (PlayerStatus s : state.status)
            if (s == PlayerStatus.ACTIVE)
                return false;
        return true;
    }

    /**
     * Scores every lawn that has not been cleared, then ends the game if a player has reached the target on every
     * track, or starts the next round.
     */
    private void endOfRound(LawnAndOrderGameState state) {
        LawnAndOrderParameters params = (LawnAndOrderParameters) state.getGameParameters();
        for (int p = 0; p < state.getNPlayers(); p++) {
            // a player with a Cease & Desist has an empty lawn
            for (LawnCard.Category category : LawnCard.Category.values())
                state.trackScores[p][category.ordinal()] +=
                        LawnAndOrderUtils.categoryScore(state.lawns.get(p).getComponents(), category, params);
        }
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (state.hasReachedTarget(p)) {
                endGame(state);
                return;
            }
        }
        // At the Round Cleanup this round's Goodwill cards are returned, and its Cease & Desist cards become Goodwill
        for (int p = 0; p < state.getNPlayers(); p++)
            state.goodwill[p] = state.status[p] == PlayerStatus.CEASE_AND_DESIST;
        endRound(state, 0);
        if (state.isNotTerminal())
            startRound(state);
    }

    @Override
    protected void endGame(AbstractGameState gs) {
        LawnAndOrderGameState state = (LawnAndOrderGameState) gs;
        int nPlayers = state.getNPlayers();
        // The highest combined total wins among the players who have reached the target on every track. If nobody
        // has (the game ended at the round limit), it wins among all players. Players tied on that total draw.
        List<Integer> candidates = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            if (state.hasReachedTarget(p))
                candidates.add(p);
        if (candidates.isEmpty())
            for (int p = 0; p < nPlayers; p++)
                candidates.add(p);
        double best = candidates.stream().mapToDouble(state::getGameScore).max().orElseThrow();
        List<Integer> winners = candidates.stream().filter(p -> state.getGameScore(p) == best).toList();
        state.setGameStatus(GAME_END);
        for (int p = 0; p < nPlayers; p++) {
            if (!winners.contains(p))
                state.setPlayerResult(LOSE_GAME, p);
            else
                state.setPlayerResult(winners.size() > 1 ? DRAW_GAME : WIN_GAME, p);
        }
    }
}
