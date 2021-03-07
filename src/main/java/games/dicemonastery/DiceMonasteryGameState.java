package games.dicemonastery;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Token;
import utilities.Utils;

import java.util.*;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;


public class DiceMonasteryGameState extends AbstractGameState {

    Map<ActionArea, DMArea> actionAreas = new HashMap<>();
    Map<Integer, Monk> allMonks = new HashMap<>();
    Map<Integer, ActionArea> monkLocations = new HashMap<>();
    List<Map<Resource, Integer>> playerTreasuries = new ArrayList<>();
    List<Map<Resource, Integer>> playerBids = new ArrayList<>();
    int nextRetirementReward = 0;
    int[] victoryPoints;

    public DiceMonasteryGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new DiceMonasteryTurnOrder(nPlayers, (DiceMonasteryParams) gameParameters));
    }

    @Override
    protected void _reset() {
        actionAreas = new HashMap<>();
        Arrays.stream(ActionArea.values()).forEach(a ->
                actionAreas.put(a, new DMArea(-1, a.name()))
        );

        victoryPoints = new int[getNPlayers()];
        allMonks = new HashMap<>();
        monkLocations = new HashMap<>();
        playerTreasuries = new ArrayList<>();
        playerBids = new ArrayList<>();
        for (int p = 0; p < getNPlayers(); p++) {
            playerTreasuries.add(new HashMap<>());
            playerBids.add(new HashMap<>());
        }
        nextRetirementReward = 0;
    }

    public void createMonk(int piety, int player) {
        Monk monk = new Monk(piety, player);
        int id = monk.getComponentID();
        allMonks.put(id, monk);
        monkLocations.put(id, DORMITORY);
        actionAreas.get(DORMITORY).putComponent(monk);
    }

    public void moveMonk(int id, ActionArea from, ActionArea to) {
        Monk movingMonk = allMonks.get(id);
    //    System.out.printf("\tMoving Monk %s%n", movingMonk);
        if (movingMonk == null)
            throw new IllegalArgumentException("Monk does not exist : " + id);
        if (movingMonk.piety < to.dieMinimum)
            throw new AssertionError(String.format("Monk only has a piety of %d, so cannot move to %s", movingMonk.piety, to));
        monkLocations.put(id, to);
        actionAreas.get(from).removeComponent(movingMonk);
        actionAreas.get(to).putComponent(movingMonk);
    }

    public void addResource(int player, Resource resource, int amount) {
        int currentLevel = getResource(player, resource, STOREROOM);
        if (currentLevel + amount < 0)
            throw new IllegalArgumentException(String.format("Only have %d %s in stock; cannot remove %d", currentLevel, resource, -amount));
        playerTreasuries.get(player).put(resource, currentLevel + amount);
    }

    public void moveCube(int player, Resource resource, ActionArea from, ActionArea to) {
        Token cubeMoved = null;
        if (from == STOREROOM) {
            addResource(player, resource, -1);
        } else if (from != SUPPLY) {
            cubeMoved = actionAreas.get(from).take(resource, player);
        }
        if (to == STOREROOM) {
            addResource(player, resource, 1);
        } else if (to != SUPPLY) {
            if (cubeMoved == null)
                cubeMoved = new Token("Cube");
            cubeMoved.setOwnerId(player);
            cubeMoved.setTokenType(resource.toString());
            actionAreas.get(to).putComponent(cubeMoved);
        }
    }

    public boolean reserveBid(int beer, int mead) {
        int player = getCurrentPlayer();
        int totalBeer = getResource(player, Resource.BEER, STOREROOM);
        int totalMead = getResource(player, Resource.MEAD, STOREROOM);
        if (beer > totalBeer || mead > totalMead)
            throw new AssertionError(String.format("Cannot bid more beer or mead than you have %d of %d, %d of %d", beer, totalBeer, mead, totalMead));

        playerBids.get(player).put(Resource.BEER, beer);
        playerBids.get(player).put(Resource.MEAD, mead);
        return true;
    }

    public void executeBids() {
        if (((DiceMonasteryTurnOrder) turnOrder).season != Season.SUMMER)
            throw new AssertionError(String.format("Wrong season (%s) for Viking raids!", ((DiceMonasteryTurnOrder) turnOrder).season));

        List<Integer> bidPerPlayer = playerBids.stream().map(
                bid -> bid.getOrDefault(Resource.BEER, 0) + bid.getOrDefault(Resource.MEAD, 0) * 2
        ).collect(toList());

        int lowestBid = bidPerPlayer.stream().min(comparingInt(Integer::intValue)).orElseThrow(() -> new AssertionError("Empty List?!"));

        // sorted in descending order
        List<Integer> sortedBids = bidPerPlayer.stream().sorted(comparingInt(i -> -i)).collect(toList());

        // contains the ordinality of the player bids, 0 = best bid (including joint equals) and so on.
        List<Integer> playerOrdinality = bidPerPlayer.stream().map(sortedBids::indexOf).collect(toList());

        for (int player = 0; player < bidPerPlayer.size(); player++) {
            Map<Resource, Integer> treasury = playerTreasuries.get(player);
            if (bidPerPlayer.get(player) == lowestBid) {
                // this takes precedence over ordinality - no VP, and lose a monk
                Monk lowestMonk = monksIn(DORMITORY, player).stream().min(comparingInt(Monk::getPiety))
                        .orElseThrow(() -> new AssertionError("No monks...?"));
                moveMonk(lowestMonk.getComponentID(), DORMITORY, GRAVEYARD);
                playerBids.get(player).clear();
            } else {
                // Gain VP
                int vp = VIKING_REWARDS[bidPerPlayer.size() - 1][playerOrdinality.get(player)];
                // TODO: Technically need to divide this among people with same ordinality
                addVP(vp, player);
                // and then lose stuff in Bid
                treasury.merge(Resource.BEER, -playerBids.get(player).getOrDefault(Resource.BEER, 0), Integer::sum);
                treasury.merge(Resource.MEAD, -playerBids.get(player).getOrDefault(Resource.MEAD, 0), Integer::sum);
                playerBids.get(player).clear();
            }
        }
    }

    public void retireMonk(Monk monk) {
        moveMonk(monk.getComponentID(), getMonkLocation(monk.getComponentID()), RETIRED);
        if (nextRetirementReward >= RETIREMENT_REWARDS.length) {
            // no more benefits to retirement
            return;
        }
        addVP(RETIREMENT_REWARDS[nextRetirementReward], monk.getOwnerId());
        nextRetirementReward++;
    }

    public int getVictoryPoints(int player) {
        return victoryPoints[player];
    }

    public void addVP(int amount, int player) {
        victoryPoints[player] += amount;
        if (victoryPoints[player] < 0)
            victoryPoints[player] = 0;
    }

    public void useAP(int actionPointsSpent) {
        DiceMonasteryTurnOrder dto = (DiceMonasteryTurnOrder) turnOrder;
        if (dto.actionPointsLeftForCurrentPlayer < actionPointsSpent) {
            throw new IllegalArgumentException("Not enough action points available");
        }
        dto.actionPointsLeftForCurrentPlayer -= actionPointsSpent;
    }

    public int getAPLeft() {
        return ((DiceMonasteryTurnOrder) turnOrder).getActionPointsLeft();
    }

    public int getResource(int player, Resource resource, ActionArea location) {
        if (location == STOREROOM) {
            return playerTreasuries.get(player).getOrDefault(resource, 0);
        }
        return actionAreas.get(location).count(resource, player);
    }

    public List<Monk> monksIn(ActionArea region, int player) {
        return allMonks.values().stream()
                .filter(m -> {
                    ActionArea location = monkLocations.get(m.getComponentID());
                    return (
                            ((region == null && location != RETIRED && location != GRAVEYARD) || region == location)
                                    && (player == -1 || player == m.getOwnerId()));
                })
                .collect(toList());
    }

    public ActionArea getMonkLocation(int id) {
        return monkLocations.get(id);
    }

    public List<Integer> getDominantPlayers() {
        DiceMonasteryTurnOrder dto = (DiceMonasteryTurnOrder) turnOrder;
        return dto.dominantPlayers;
    }

    void winterHousekeeping() {
         for (int player = 0; player < turnOrder.nPlayers(); player++) {
             // for each player feed monks, and then discard perishables
             List<Monk> monks = monksIn(null, player);
             int requiredFood = monks.size();
             requiredFood -= getResource(player, Resource.BERRIES, STOREROOM);
             requiredFood -= getResource(player, Resource.BREAD, STOREROOM);
             if (requiredFood > 0) {
                 int honeyEaten = Math.min(requiredFood, getResource(player, Resource.HONEY, STOREROOM));
                 addResource(player, Resource.HONEY, -honeyEaten);
                 requiredFood -= honeyEaten;
             }
             if (requiredFood > 0) {
                 // monks starve
                 addVP(-requiredFood, player);
                 // we also need to down-pip monks; let's assume we start at the lower value ones...excluding 1
                 // TODO: Make this a player decision
                 monks.stream()
                         .filter(m -> m.getPiety() > 1)
                         .sorted(comparingInt(Monk::getPiety))
                         .limit(requiredFood)
                         .forEach(Monk::demote);
             }
             // then remove all perishable goods from Storeroom, and unharvested wheat from the Meadow
             addResource(player, Resource.BREAD, -getResource(player, Resource.BREAD, STOREROOM));
             addResource(player, Resource.BERRIES, -getResource(player, Resource.BERRIES, STOREROOM));
             addResource(player, Resource.CALF_SKIN, -getResource(player, Resource.CALF_SKIN, STOREROOM));
             int unharvestedWheat = getResource(player, Resource.GRAIN, MEADOW);
             for (int i = 0; i < unharvestedWheat; i++)
                 moveCube(player, Resource.GRAIN, MEADOW, SUPPLY);
         }
    }

    void endGame() {
        setGameStatus(Utils.GameResult.GAME_END);
        int[] finalScores = new int[getNPlayers()];
        for (int p = 0; p < getNPlayers(); p++) {
            finalScores[p] = (int) getGameScore(p);
        }
        int winningScore = Arrays.stream(finalScores).max().orElseThrow(() -> new AssertionError("No MAX score found"));
        for (int p = 0; p < getNPlayers(); p++) {
            setPlayerResult(finalScores[p] == winningScore ? Utils.GameResult.WIN : Utils.GameResult.LOSE, p);
        }
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>(allMonks.values());
    }

    /*
        List<Map<Resource, Integer>> playerTreasuries = new ArrayList<>();
    */
    @Override
    protected DiceMonasteryGameState _copy(int playerId) {
        DiceMonasteryGameState retValue = new DiceMonasteryGameState(gameParameters.copy(), getNPlayers());
        for (ActionArea a : actionAreas.keySet()) {
            retValue.actionAreas.put(a, actionAreas.get(a).copy());
        }
        retValue.allMonks.clear();
        retValue.monkLocations.clear();
        for (int monkId : allMonks.keySet()) {
            retValue.allMonks.put(monkId, allMonks.get(monkId).copy());
        }
        // monkLocations contains immutable things, so we just create a new mapping
        retValue.monkLocations = new HashMap<>(monkLocations);

        retValue.playerTreasuries = new ArrayList<>();
        retValue.playerBids = new ArrayList<>();
        for (int p = 0; p < getNPlayers(); p++) {
            retValue.playerTreasuries.add(new HashMap<>(playerTreasuries.get(p)));
            retValue.playerBids.add(new HashMap<>(playerBids.get(p)));
        }
        retValue.nextRetirementReward = nextRetirementReward;

        retValue.victoryPoints = Arrays.copyOf(victoryPoints, getNPlayers());
        return retValue;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        return allMonks.values().stream()
                .filter(m -> m.getOwnerId() == playerId)
                .mapToInt(Monk::getPiety)
                .sum() + getVictoryPoints(playerId);
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof DiceMonasteryGameState))
            return false;
        DiceMonasteryGameState other = (DiceMonasteryGameState) o;
        return other.allMonks.equals(allMonks) && other.monkLocations.equals(monkLocations) &&
                other.playerTreasuries.equals(playerTreasuries) && other.actionsInProgress.equals(actionsInProgress) &&
                 other.playerBids.equals(playerBids) &&
                other.nextRetirementReward == nextRetirementReward && other.actionAreas.equals(actionAreas) &&
                Arrays.equals(other.victoryPoints, victoryPoints) && Arrays.equals(other.playerResults, playerResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionAreas, allMonks, monkLocations, playerTreasuries, actionsInProgress, gameStatus, gamePhase,
                gameParameters, turnOrder, nextRetirementReward, playerBids) +
                31 * Arrays.hashCode(playerResults) + 871 * Arrays.hashCode(victoryPoints);
    }

}
