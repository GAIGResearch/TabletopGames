package games.risk;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import games.GameType;
import games.risk.actions.*;
import games.risk.components.RiskCard;
import games.risk.components.RiskMission;

import java.util.*;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_OWNER;

public class RiskForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        RiskGameState state = (RiskGameState) firstState;
        RiskParameters params = (RiskParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        RiskMap map = params.getMap();
        int nTerritories = map.nTerritories();

        state.owner = new int[nTerritories];
        Arrays.fill(state.owner, -1);
        state.armies = new int[nTerritories];
        state.armiesToPlace = new int[nPlayers];
        Arrays.fill(state.armiesToPlace, params.startingArmies(nPlayers));
        state.finalPlace = new int[nPlayers];

        state.drawDeck = new Deck<>("Draw deck", HIDDEN_TO_ALL);
        for (RiskTerritory t : map.territories())
            state.drawDeck.add(new RiskCard(t));
        for (int i = 0; i < params.nWildCards; i++)
            state.drawDeck.add(new RiskCard());
        state.drawDeck.shuffle(state.getRnd());
        state.discardPile = new Deck<>("Discard pile", VISIBLE_TO_ALL);
        state.missions = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.missions.add(new Deck<>("Mission " + p, p, VISIBLE_TO_OWNER));
        state.unusedMissions = new Deck<>("Unused missions", HIDDEN_TO_ALL);
        state.eliminatedBy = new int[nPlayers];
        Arrays.fill(state.eliminatedBy, -1);
        state.missionWinner = -1;
        state.hands = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.hands.add(new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER));

        // the roll of one die for who goes first
        int firstPlayer = state.getRnd().nextInt(nPlayers);
        state.setFirstPlayer(firstPlayer);

        if (params.secretMission) {
            if (map.backupTerritories() <= 0)
                throw new IllegalArgumentException(map.fileName + " has no backupTerritories for Secret Mission");
            // each player is dealt a mission at random; the rest are out of the game
            List<RiskMission> all = new ArrayList<>(map.missions());
            // a destroy card for each colour, whether or not it is in the game
            for (int k = 0; k < GameType.Risk.getMaxPlayers(); k++)
                all.add(RiskMission.destroy(k));
            state.unusedMissions.add(all);
            state.unusedMissions.shuffle(state.getRnd());
            for (int p = 0; p < nPlayers; p++)
                state.missions.get(p).add(state.unusedMissions.draw());
        }

        if (params.randomTerritoryDeal || params.secretMission) {
            // the territories are dealt round the table from the first player, each taking one army
            List<RiskTerritory> territories = new ArrayList<>(map.territories());
            Collections.shuffle(territories, state.getRnd());
            for (int i = 0; i < territories.size(); i++) {
                int player = (firstPlayer + i) % nPlayers;
                state.setOwner(territories.get(i), player);
                state.addArmies(territories.get(i), 1);
                state.armiesToPlace[player]--;
            }
            state.setGamePhase(RiskGamePhase.PLACE_INITIAL);
        } else {
            state.setGamePhase(RiskGamePhase.CLAIM);
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        RiskGameState state = (RiskGameState) gameState;
        RiskParameters params = (RiskParameters) state.getGameParameters();
        int player = state.getCurrentPlayer();
        List<AbstractAction> actions = new ArrayList<>();
        switch ((RiskGamePhase) state.getGamePhase()) {
            case CLAIM -> {
                for (RiskTerritory t : state.getTerritories(-1))
                    actions.add(new ClaimTerritory(t));
            }
            case PLACE_INITIAL -> {
                for (RiskTerritory t : state.getPlaceableTerritories(player))
                    actions.add(new PlaceArmy(t));
            }
            case REINFORCE -> {
                actions.addAll(TradeCards.options(state, player));
                // with handLimit or more cards, the player must trade before placing
                if (state.getHand(player).getSize() < params.handLimit)
                    for (RiskTerritory t : state.getPlaceableTerritories(player))
                        actions.add(new PlaceArmy(t));
            }
            case ATTACK -> {
                for (RiskTerritory from : state.getTerritories(player)) {
                    int maxDice = Math.min(params.maxAttackDice, state.getArmies(from) - 1);
                    for (RiskTerritory to : state.getMap().neighbours(from))
                        if (state.getOwner(to) != player && maxDice > 0) {
                            for (int n = 1; n <= maxDice; n++)
                                actions.add(new Attack(from, to, n));
                            if (params.allowBlitz)
                                actions.add(new Blitz(from, to));
                        }
                }
                actions.add(new EndAttack());
            }
            case FORTIFY -> {
                for (RiskTerritory from : state.getTerritories(player))
                    if (state.getArmies(from) >= 2)
                        for (RiskTerritory to : fortifyTargets(state, from, params.fortifyAlongPath))
                            if (state.getRoom(to) > 0)
                                actions.add(new Fortify(from, to));
                actions.add(new EndTurn());
            }
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        RiskGameState state = (RiskGameState) currentState;
        int player = state.getCurrentPlayer();
        if (state.getNTerritories(player) == state.getMap().nTerritories()) {
            endGame(state);
            return;
        }
        if (((RiskParameters) state.getGameParameters()).secretMission && missionCompleted(state)) {
            endGame(state);
            return;
        }
        switch ((RiskGamePhase) state.getGamePhase()) {
            case CLAIM -> {
                if (state.getTerritories(-1).isEmpty())
                    state.setGamePhase(RiskGamePhase.PLACE_INITIAL);
                passInitialPlacement(state);
            }
            case PLACE_INITIAL -> passInitialPlacement(state);
            case REINFORCE -> endReinforcementIfDone(state, player);
            case ATTACK -> {
                if (actionTaken instanceof EndAttack)
                    state.setGamePhase(RiskGamePhase.FORTIFY);
            }
            // the fortifying move, or EndTurn
            case FORTIFY -> endTurn(state);
        }
    }

    /**
     * Whether a player still in has completed their Secret Mission. If so, they are recorded as the winner.
     */
    private boolean missionCompleted(RiskGameState state) {
        int n = state.getNPlayers();
        // the players are checked round the table from the current player, and the first found wins
        for (int i = 0; i < n; i++) {
            int p = (state.getCurrentPlayer() + i) % n;
            RiskMission mission = state.getMission(p);
            if (!state.isEliminated(p) && mission != null && mission.isComplete(state, p)) {
                state.setMissionWinner(p);
                return true;
            }
        }
        return false;
    }

    /**
     * Passes the turn during the initial placement to the next player with armies left to place. Once nobody has
     * any, play begins with the first player. These turns do not count towards the rounds.
     */
    private void passInitialPlacement(RiskGameState state) {
        int nPlayers = state.getNPlayers();
        // under maxArmiesPerTerritory, armies that fit nowhere are lost (not while territories are still unclaimed)
        boolean placing = state.getGamePhase() == RiskGamePhase.PLACE_INITIAL;
        if (placing)
            state.loseArmiesWithNoRoom(state.getCurrentPlayer());
        for (int i = 1; i <= nPlayers; i++) {
            int next = (state.getCurrentPlayer() + i) % nPlayers;
            if (placing)
                state.loseArmiesWithNoRoom(next);
            if (state.getArmiesToPlace(next) > 0) {
                endPlayerTurn(state, next);
                return;
            }
        }
        endPlayerTurn(state, state.getFirstPlayer());
        startTurn(state, state.getFirstPlayer());
    }

    /**
     * Passes the turn to the next player still in the game. A round ends each time the turn passes back round the
     * table.
     */
    private void endTurn(RiskGameState state) {
        int current = state.getCurrentPlayer();
        int next = current;
        do {
            next = (next + 1) % state.getNPlayers();
        } while (state.isEliminated(next));
        endPlayerTurn(state, next);
        if (next <= current)
            endRound(state, next);
        if (state.isNotTerminal())
            startTurn(state, next);
    }

    private void startTurn(RiskGameState state, int player) {
        state.setCapturedThisTurn(false);
        state.setTerritoryBonusTaken(false);
        state.setArmiesToPlace(player, state.getReinforcements(player));
        state.setGamePhase(RiskGamePhase.REINFORCE);
        endReinforcementIfDone(state, player);
    }

    /**
     * Ends REINFORCE when the player has no armies left to place.
     */
    private void endReinforcementIfDone(RiskGameState state, int player) {
        RiskParameters params = (RiskParameters) state.getGameParameters();
        // armies that fit nowhere are lost (maxArmiesPerTerritory), but not before a trade forced by a full hand
        if (state.getHand(player).getSize() < params.handLimit)
            state.loseArmiesWithNoRoom(player);
        if (state.getArmiesToPlace(player) == 0)
            state.setGamePhase(RiskGamePhase.ATTACK);
    }

    /**
     * The territories that the owner of from may move armies to from it when fortifying.
     */
    private List<RiskTerritory> fortifyTargets(RiskGameState state, RiskTerritory from, boolean alongPath) {
        int player = state.getOwner(from);
        List<RiskTerritory> targets = new ArrayList<>();
        if (!alongPath) {
            for (RiskTerritory to : state.getMap().neighbours(from))
                if (state.getOwner(to) == player)
                    targets.add(to);
            return targets;
        }
        // with fortifyAlongPath, every territory reached through a chain of territories the player holds
        Set<RiskTerritory> seen = new HashSet<>(List.of(from));
        Deque<RiskTerritory> queue = new ArrayDeque<>(seen);
        while (!queue.isEmpty())
            for (RiskTerritory n : state.getMap().neighbours(queue.poll()))
                if (state.getOwner(n) == player && seen.add(n)) {
                    targets.add(n);
                    queue.add(n);
                }
        targets.sort(Comparator.comparingInt(RiskTerritory::index));
        return targets;
    }
}
