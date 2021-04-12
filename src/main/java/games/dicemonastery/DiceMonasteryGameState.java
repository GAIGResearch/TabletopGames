package games.dicemonastery;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.Token;
import games.GameType;
import utilities.Utils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static core.CoreConstants.VisibilityMode.FIRST_VISIBLE_TO_ALL;
import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.SUMMER;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class DiceMonasteryGameState extends AbstractGameState {

    Map<ActionArea, DMArea> actionAreas = new HashMap<>();
    Map<Integer, Monk> allMonks = new HashMap<>();
    Map<Integer, ActionArea> monkLocations = new HashMap<>();
    List<EnumMap<Resource, Integer>> playerTreasuries = new ArrayList<>();
    Map<Integer, Map<Resource, Integer>> playerBids = new HashMap<>();
    int nextRetirementReward = 0;
    Map<ILLUMINATED_TEXT, Integer> textsWritten = new EnumMap<>(ILLUMINATED_TEXT.class);
    Map<TREASURE, Integer> treasuresCommissioned = new EnumMap<>(TREASURE.class);
    List<List<TREASURE>> treasuresOwnedPerPlayer = new ArrayList<>();
    Map<Pilgrimage.DESTINATION, Deck<Pilgrimage>> pilgrimageDecks = new HashMap<>(4);
    List<Pilgrimage> pilgrimagesStarted = new ArrayList<>();
    Deck<MarketCard> marketCards = new Deck<>("Market Deck", FIRST_VISIBLE_TO_ALL);
    int[] victoryPoints;
    Random rnd;

    public DiceMonasteryGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new DiceMonasteryTurnOrder(nPlayers, (DiceMonasteryParams) gameParameters), GameType.DiceMonastery);
        rnd = new Random(gameParameters.getRandomSeed());
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
        playerBids = new HashMap<>();
        for (int p = 0; p < getNPlayers(); p++) {
            playerTreasuries.add(new EnumMap<>(Resource.class));
            treasuresOwnedPerPlayer.add(new ArrayList<>());
        }
        nextRetirementReward = 0;
        textsWritten = new EnumMap<>(ILLUMINATED_TEXT.class);
        for (ILLUMINATED_TEXT text : ILLUMINATED_TEXT.values())
            textsWritten.put(text, 0);
        treasuresCommissioned = new EnumMap<>(TREASURE.class);
        for (TREASURE item : TREASURE.values())
            treasuresCommissioned.put(item, 0);
        pilgrimageDecks = new HashMap<>();
        for (Pilgrimage.DESTINATION destination : Pilgrimage.DESTINATION.values())
            pilgrimageDecks.put(destination, new Deck<>("Pilgrimages to "+destination.name(), FIRST_VISIBLE_TO_ALL));
        pilgrimagesStarted = new ArrayList<>();
    }

    public Monk createMonk(int piety, int player) {
        Monk monk = new Monk(piety, player);
        int id = monk.getComponentID();
        allMonks.put(id, monk);
        monkLocations.put(id, DORMITORY);
        actionAreas.get(DORMITORY).putComponent(monk);
        return monk;
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

        playerBids.put(player, new HashMap<>());
        playerBids.get(player).put(Resource.BEER, beer);
        playerBids.get(player).put(Resource.MEAD, mead);
        return true;
    }

    public void executeBids() {
        if (((DiceMonasteryTurnOrder) turnOrder).season != SUMMER)
            throw new AssertionError(String.format("Wrong season (%s) for Viking raids!", ((DiceMonasteryTurnOrder) turnOrder).season));

        List<Integer> bidPerPlayer = IntStream.range(0, getNPlayers()).map(player -> {
                    Map<Resource, Integer> bid = playerBids.get(player);
                    return bid.getOrDefault(Resource.BEER, 0) + bid.getOrDefault(Resource.MEAD, 0) * 2;
                }
        ).boxed().collect(toList());

        int lowestBid = bidPerPlayer.stream().min(comparingInt(Integer::intValue)).orElseThrow(() -> new AssertionError("Empty List?!"));

        // sorted in descending order
        List<Integer> sortedBids = bidPerPlayer.stream().sorted(comparingInt(i -> -i)).collect(toList());

        // contains the ordinality of the player bids, 0 = best bid (including joint equals) and so on.
        List<Integer> playerOrdinality = bidPerPlayer.stream().map(sortedBids::indexOf).collect(toList());

        for (int player = 0; player < bidPerPlayer.size(); player++) {
            Map<Resource, Integer> treasury = playerTreasuries.get(player);
            if (bidPerPlayer.get(player) == lowestBid) {
                // this takes precedence over ordinality - no VP, and lose a Treasure, or a Monk
                // TODO: Technically this is a choice that the player can make
                if (treasuresOwnedPerPlayer.get(player).isEmpty()) {
                    Optional<Monk> lowestMonk = monksIn(DORMITORY, player).stream().min(comparingInt(Monk::getPiety));
                    lowestMonk.ifPresent(monk -> moveMonk(monk.getComponentID(), DORMITORY, GRAVEYARD));
                } else {
                    List<TREASURE> treasures = treasuresOwnedPerPlayer.get(player);
                    TREASURE loss = treasures.stream().max(comparingInt(t -> t.vp)).orElseThrow(() -> new AssertionError("No Treasures to lose?"));
                    treasures.remove(loss);
                    addVP(-loss.vp, player);
                }
                playerBids.remove(player);
            } else {
                // Gain VP
                int vp = VIKING_REWARDS[bidPerPlayer.size() - 1][playerOrdinality.get(player)];
                // TODO: Technically need to divide this among people with same ordinality
                addVP(vp, player);
                // and then lose stuff in Bid
                treasury.merge(Resource.BEER, -playerBids.get(player).getOrDefault(Resource.BEER, 0), Integer::sum);
                treasury.merge(Resource.MEAD, -playerBids.get(player).getOrDefault(Resource.MEAD, 0), Integer::sum);
                playerBids.remove(player);
            }
        }
    }

    public void retireMonk(Monk monk) {
        if (getMonkLocation(monk.getComponentID()) == RETIRED)
            throw new AssertionError("Already retired!");
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

    public void writeText(ILLUMINATED_TEXT textType) {
        int currentNumber = textsWritten.get(textType);
        if (currentNumber >= textType.rewards.length) {
            throw new AssertionError("Cannot write any more " + textType);
        }
        textsWritten.put(textType, currentNumber + 1);
    }

    public int getNumberWritten(ILLUMINATED_TEXT textType) {
        return textsWritten.get(textType);
    }

    public void acquireTreasure(TREASURE item, int player) {
        if (treasuresCommissioned.get(item) < item.limit) {
            treasuresCommissioned.put(item, treasuresCommissioned.get(item) + 1);
            addVP(item.vp, player);
            treasuresOwnedPerPlayer.get(player).add(item);
        } else
            throw new AssertionError("Cannot buy treasure as none left: " + item);
    }

    public void addTreasure(TREASURE item) {
        treasuresCommissioned.merge(item, -1, Integer::sum);
    }

    public int getNumberCommissioned(TREASURE item) {
        return treasuresCommissioned.get(item);
    }

    public List<TREASURE> getTreasures(int player) {
        return treasuresOwnedPerPlayer.get(player);
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

    public Map<Resource, Integer> getStores(int player, Predicate<Resource> predicate) {
        return playerTreasuries.get(player).entrySet().stream()
                .filter(e -> e.getValue() > 0 && predicate.test(e.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum, () -> new EnumMap<>(Resource.class)));
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

    public Monk getMonkById(int id) {
        return allMonks.get(id);
    }

    public ActionArea getMonkLocation(int id) {
        return monkLocations.get(id);
    }

    public boolean allBidsIn() {
        return IntStream.range(0, getNPlayers()).allMatch(playerBids::containsKey);
    }

    void springAutumnHousekeeping() {
        // We move PROTO_ stuff on its merry way
        for (int player = 0; player < turnOrder.nPlayers(); player++) {
            int almostBeer = getResource(player, Resource.PROTO_BEER_2, STOREROOM);
            addResource(player, Resource.BEER, almostBeer);
            int notBeer = getResource(player, Resource.PROTO_BEER_1, STOREROOM);
            addResource(player, Resource.PROTO_BEER_2, notBeer - almostBeer);
            addResource(player, Resource.PROTO_BEER_1, -notBeer);
            int almostMead = getResource(player, Resource.PROTO_MEAD_2, STOREROOM);
            addResource(player, Resource.MEAD, almostMead);
            int notMead = getResource(player, Resource.PROTO_MEAD_1, STOREROOM);
            addResource(player, Resource.PROTO_MEAD_2, notMead - almostMead);
            addResource(player, Resource.PROTO_MEAD_1, -notMead);
        }
        advancePilgrims();
        drawBonusTokens();
        marketCards.draw();
    }


    void checkAtLeastOneMonk() {
        for (int player = 0; player < getNPlayers(); player++) {
            if (monksIn(null, player).isEmpty()) {
                // Hire a free novice!
                createMonk(1, player);
            }
        }
    }

    public Pilgrimage peekAtNextPilgrimageTo(Pilgrimage.DESTINATION destination) {
        return pilgrimageDecks.get(destination).peek();
    }

    public MarketCard getCurrentMarket() {
        return marketCards.peek();
    }

    public Pilgrimage startPilgrimage(Pilgrimage.DESTINATION destination, Monk monk) {
        Pilgrimage retValue = pilgrimageDecks.get(destination).draw();
        retValue.startPilgrimage(monk, this);
        pilgrimagesStarted.add(retValue);
        return retValue;
    }

    public int pilgrimagesLeft(Pilgrimage.DESTINATION to) {
        return pilgrimageDecks.get(to).getSize();
    }

    public List<Pilgrimage> getPilgrimagesStarted() {
        return pilgrimagesStarted;
    }

    void advancePilgrims() {
        for (Pilgrimage p : pilgrimagesStarted) {
            if (p.active) {
                p.advance(this);
            }
        }
    }

    void drawBonusTokens() {
        int tokensPerArea = ((DiceMonasteryParams) getGameParameters()).BONUS_TOKENS_PER_PLAYER[getNPlayers()];
        for (ActionArea key : actionAreas.keySet()) {
            if (key.dieMinimum > 0) {
                DMArea area = actionAreas.get(key);
                for (int i = 0; i < tokensPerArea; i++) {
                    area.tokens[i] = drawToken(rnd);
                }
            }
        }
    }

    BONUS_TOKEN drawToken(Random rnd) {
        double dieRoll = rnd.nextDouble();
        for (BONUS_TOKEN value : BONUS_TOKEN.values()) {
            dieRoll -= value.getChance();
            if (dieRoll < 0.0)
                return value;
        }
        throw new AssertionError("BONUS_TOKEN probabilities sum to less than 1.0");
    }

    public List<BONUS_TOKEN> availableBonusTokens(ActionArea area) {
        DMArea dma = actionAreas.get(area);
        if (dma == null)
            throw new AssertionError("Area does not exist: " + area);
        return Arrays.stream(dma.tokens).filter(Objects::nonNull).collect(toList());
    }

    public void removeToken(BONUS_TOKEN token, ActionArea area) {
        BONUS_TOKEN[] tokens = actionAreas.get(area).tokens;
        DiceMonasteryTurnOrder to = (DiceMonasteryTurnOrder) turnOrder;
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] == token) {
                tokens[i] = null;
                return;
            }
        }
        throw new AssertionError(String.format("No %s token found in %s", token, area));
    }

    public void addActionPoints(int number) {
        ((DiceMonasteryTurnOrder) turnOrder).addActionPoints(number);
    }

    public void putToken(ActionArea area, BONUS_TOKEN token, int position) {
        actionAreas.get(area).tokens[position] = token;
    }

    void winterHousekeeping() {
        for (int player = 0; player < turnOrder.nPlayers(); player++) {
            // for each player feed monks, and then discard perishables
            List<Monk> monks = monksIn(DORMITORY, player);
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
        // Deliberately do not check monks here...that is done afterwards...Winter housekeeping is done before winter actions
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
        List<Component> retValue = new ArrayList<>(allMonks.values());
        retValue.addAll(pilgrimagesStarted);
        for (Deck<Pilgrimage> pdeck : pilgrimageDecks.values())
            retValue.addAll(pdeck.getComponents());
        return retValue;
    }

    /*
        List<Map<Resource, Integer>> playerTreasuries = new ArrayList<>();
    */
    @Override
    protected DiceMonasteryGameState _copy(int playerId) {
        DiceMonasteryGameState retValue = new DiceMonasteryGameState(gameParameters.copy(), getNPlayers());
        DiceMonasteryTurnOrder dmto = (DiceMonasteryTurnOrder) turnOrder;
        rnd = new Random(System.currentTimeMillis());
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
        retValue.playerBids = new HashMap<>();
        for (int p = 0; p < getNPlayers(); p++) {
            retValue.playerTreasuries.add(new EnumMap<>(playerTreasuries.get(p)));
            retValue.treasuresOwnedPerPlayer.add(new ArrayList<>(treasuresOwnedPerPlayer.get(p)));
            if (playerBids.containsKey(p))
                retValue.playerBids.put(p, new HashMap<>(playerBids.get(p)));
        }
        retValue.nextRetirementReward = nextRetirementReward;
        retValue.textsWritten.putAll(textsWritten);
        retValue.treasuresCommissioned.putAll(treasuresCommissioned);

        retValue.marketCards = marketCards.copy();
        if (playerId != -1) { // shuffle all except the top card
            MarketCard topCard = retValue.marketCards.draw();
            retValue.marketCards.shuffle(rnd);
            retValue.marketCards.add(topCard);
        }

        for (Pilgrimage p : pilgrimagesStarted)
            retValue.pilgrimagesStarted.add(p.copy());

        for (Pilgrimage.DESTINATION destination : Pilgrimage.DESTINATION.values()) {
            Deck<Pilgrimage> thisDeck = pilgrimageDecks.get(destination);
            Deck<Pilgrimage> copyDeck = thisDeck.copy();
            if (playerId != -1) {// only top card is visible, so shuffle if copied from any player's perspective
                Pilgrimage topCard = copyDeck.draw();
                copyDeck.shuffle(rnd);
                if (topCard != null)
                    copyDeck.add(topCard);
            }
            retValue.pilgrimageDecks.put(destination, copyDeck);
        }

        retValue.victoryPoints = Arrays.copyOf(victoryPoints, getNPlayers());

        if (playerId != -1 && dmto.getSeason() == SUMMER && !allBidsIn()) {
            // we are in the middle of obtaining all Bids. This is hidden information.
            // So we blank out all current bids (which will force Turn Order to go through them all)
            retValue.playerBids = new HashMap<>();
        }
        return retValue;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        return playerTreasuries.get(playerId).getOrDefault(Resource.BEER, 0) +
                playerTreasuries.get(playerId).getOrDefault(Resource.MEAD, 0) * 2 +
                getVictoryPoints(playerId);
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof DiceMonasteryGameState))
            return false;
        DiceMonasteryGameState other = (DiceMonasteryGameState) o;
        return other.allMonks.equals(allMonks) && other.monkLocations.equals(monkLocations) &&
                other.playerTreasuries.equals(playerTreasuries) && other.actionsInProgress.equals(actionsInProgress) &&
                other.playerBids.equals(playerBids) && other.treasuresOwnedPerPlayer.equals(treasuresOwnedPerPlayer) &&
                other.nextRetirementReward == nextRetirementReward && other.actionAreas.equals(actionAreas) &&
                other.textsWritten.equals(textsWritten) && other.treasuresCommissioned.equals(treasuresCommissioned) &&
                other.pilgrimagesStarted.equals(pilgrimagesStarted) && other.pilgrimageDecks.equals(pilgrimageDecks) &&
                other.marketCards == marketCards &&
                Arrays.equals(other.victoryPoints, victoryPoints) && Arrays.equals(other.playerResults, playerResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionAreas, allMonks, monkLocations, playerTreasuries, actionsInProgress, gameStatus, gamePhase,
                gameParameters, turnOrder, nextRetirementReward, playerBids, textsWritten, treasuresCommissioned,
                pilgrimageDecks, pilgrimagesStarted, treasuresOwnedPerPlayer, marketCards) +
                31 * Arrays.hashCode(playerResults) + 871 * Arrays.hashCode(victoryPoints);
    }

}
