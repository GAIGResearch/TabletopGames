package games.dicemonastery;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.Token;
import games.GameType;
import games.dicemonastery.components.*;
import utilities.Utils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static core.CoreConstants.VisibilityMode.FIRST_VISIBLE_TO_ALL;
import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
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
    Map<Treasure, Integer> treasuresCommissioned = new HashMap<>();
    List<List<Treasure>> treasuresOwnedPerPlayer = new ArrayList<>();
    List<Deck<Pilgrimage>> pilgrimageDecks = new ArrayList<>(2);
    List<Pilgrimage> pilgrimagesStarted = new ArrayList<>();
    Deck<MarketCard> marketCards = new Deck<>("Market Deck", FIRST_VISIBLE_TO_ALL);
    Deck<ForageCard> forageCards = new Deck<>("Forage Deck", FIRST_VISIBLE_TO_ALL);
    Map<IlluminatedText, Integer> writtenTexts = new HashMap<>();
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
        treasuresOwnedPerPlayer = new ArrayList<>();
        playerBids = new HashMap<>();
        for (int p = 0; p < getNPlayers(); p++) {
            playerTreasuries.add(new EnumMap<>(Resource.class));
            treasuresOwnedPerPlayer.add(new ArrayList<>());
        }
        nextRetirementReward = 0;
        writtenTexts.replaceAll((t, v) -> 0);
        treasuresCommissioned.replaceAll((t, v) -> 0);
        pilgrimageDecks = new ArrayList<>(2);
        pilgrimageDecks.add(0, new Deck<>("Short Pilgrimages", FIRST_VISIBLE_TO_ALL));
        pilgrimageDecks.add(1, new Deck<>("Long Pilgrimages", FIRST_VISIBLE_TO_ALL));
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
        if (movingMonk.getPiety() < to.dieMinimum)
            throw new AssertionError(String.format("Monk only has a piety of %d, so cannot move to %s", movingMonk.getPiety(), to));
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

    public void moveCubes(int player, Resource resource, int count, ActionArea from, ActionArea to) {
        for (int i = 0; i < count; i++)
            moveCube(player, resource, from, to);
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
        int totalBeer = getResource(player, BEER, STOREROOM);
        int totalMead = getResource(player, MEAD, STOREROOM);
        if (beer > totalBeer || mead > totalMead)
            throw new AssertionError(String.format("Cannot bid more beer or mead than you have %d of %d, %d of %d", beer, totalBeer, mead, totalMead));

        playerBids.put(player, new HashMap<>());
        playerBids.get(player).put(BEER, beer);
        playerBids.get(player).put(MEAD, mead);
        return true;
    }

    public void loseTreasure(int player, Treasure treasure) {
        treasuresOwnedPerPlayer.get(player).remove(treasure);
        addVP(-treasure.vp, player);
    }

    public DiceMonasteryParams getParams() {
        return (DiceMonasteryParams) gameParameters;
    }

    public List<Integer> executeBids() {
        DiceMonasteryTurnOrder dmto = (DiceMonasteryTurnOrder) turnOrder;
        if (dmto.season != SUMMER)
            throw new AssertionError(String.format("Wrong season (%s) for Viking raids!", ((DiceMonasteryTurnOrder) turnOrder).season));

        List<Integer> bidPerPlayer = IntStream.range(0, getNPlayers()).map(player -> {
                    Map<Resource, Integer> bid = playerBids.get(player);
                    return bid.getOrDefault(BEER, 0) + bid.getOrDefault(MEAD, 0) * 2;
                }
        ).boxed().collect(toList());

        int lowestBid = bidPerPlayer.stream().min(comparingInt(Integer::intValue)).orElseThrow(() -> new AssertionError("Empty List?!"));

        // sorted in descending order
        List<Integer> sortedBids = bidPerPlayer.stream().sorted(comparingInt(i -> -i)).collect(toList());

        // contains the ordinality of the player bids, 0 = best bid (including joint equals) and so on.
        // this automatically takes account of ties correctly
        List<Integer> playerOrdinality = bidPerPlayer.stream().map(sortedBids::indexOf).collect(toList());

        // create map from ordinality to number of players with that ordinality
        Map<Integer, Integer> allOrdinalities = new HashMap<>();
        for (Integer integer : playerOrdinality)
            allOrdinalities.merge(integer, 1, Integer::sum);

        int[] playerRewards = new int[playerOrdinality.size()];
        int vpTotal = 0;
        boolean losers = true;
        for (Integer o = getNPlayers() - 1; o >= 0; o--) {
            int ordinalityVP = VIKING_REWARDS[getNPlayers() - 1][o];
            if (!allOrdinalities.containsKey(o)) {
                // we therefore add this vp to the next set
                vpTotal += ordinalityVP;
            } else {
                // we have players with this ordinality, so divide available points between them
                // the first time we hit this is for the losing player(s), who by definition score 0 points
                playerRewards[o] = losers ? 0 : (ordinalityVP + vpTotal) / allOrdinalities.get(o);
                vpTotal = 0;
                losers = false;
            }
        }

        List<Integer> retValue = new ArrayList<>();
        for (int player = 0; player < bidPerPlayer.size(); player++) {
            Map<Resource, Integer> treasury = playerTreasuries.get(player);
            if (bidPerPlayer.get(player) == lowestBid) {
                // add them to the list of player to make a sacrifice decision
                retValue.add(player);
                playerBids.remove(player);
            } else {
                // Gain VP
                int vp = playerRewards[playerOrdinality.get(player)];
                addVP(vp, player);
                // and then lose stuff in Bid
                treasury.merge(BEER, -playerBids.get(player).getOrDefault(BEER, 0), Integer::sum);
                treasury.merge(MEAD, -playerBids.get(player).getOrDefault(MEAD, 0), Integer::sum);
                playerBids.remove(player);
            }
        }
        return retValue;
    }

    public void retireMonk(Monk monk) {
        DiceMonasteryTurnOrder dmto = (DiceMonasteryTurnOrder) turnOrder;
        if (getMonkLocation(monk.getComponentID()) == RETIRED)
            throw new AssertionError("Already retired!");
        moveMonk(monk.getComponentID(), getMonkLocation(monk.getComponentID()), RETIRED);
        int vp = 1;
        if (nextRetirementReward < RETIREMENT_REWARDS.length) {
            vp = RETIREMENT_REWARDS[nextRetirementReward];
        }
        int finalVp = vp;
        dmto.logEvent(() -> "Monk retired for " + finalVp + " VP", this);
        addVP(vp, monk.getOwnerId());
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

    public void writeText(IlluminatedText textType) {
        int currentNumber = writtenTexts.get(textType);
        if (currentNumber >= textType.rewards.length) {
            throw new AssertionError("Cannot write any more " + textType);
        }
        writtenTexts.put(textType, currentNumber + 1);
    }

    public Set<IlluminatedText> getAvailableTexts() {
        return writtenTexts.keySet();
    }

    public int getNumberWritten(IlluminatedText textType) {
        return writtenTexts.get(textType);
    }

    public void acquireTreasure(Treasure item, int player) {
        if (treasuresCommissioned.get(item) < item.limit) {
            treasuresCommissioned.put(item, treasuresCommissioned.get(item) + 1);
            addVP(item.vp, player);
            treasuresOwnedPerPlayer.get(player).add(item);
        } else
            throw new AssertionError("Cannot buy treasure as none left: " + item);
    }

    public void addTreasure(Treasure item) {
        treasuresCommissioned.merge(item, -1, Integer::sum);
    }

    public int getNumberCommissioned(Treasure item) {
        return treasuresCommissioned.get(item);
    }

    public List<Treasure> getTreasures(int player) {
        return treasuresOwnedPerPlayer.get(player);
    }

    public List<Treasure> availableTreasures() {
        return treasuresCommissioned.keySet().stream()
                .filter(t -> treasuresCommissioned.get(t) < t.limit)
                .collect(toList());
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

    public int getHighestPietyMonk(ActionArea area, int player) {
        List<Monk> eligibleMonks = monksIn(area, player);
        return eligibleMonks.isEmpty() ? 0 : eligibleMonks.stream()
                .max(comparingInt(Monk::getPiety))
                .orElseThrow(() -> new AssertionError("No Monks in Gatehouse?"))
                .getPiety();
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
            int almostBeer = getResource(player, PROTO_BEER_2, STOREROOM);
            addResource(player, BEER, almostBeer);
            int notBeer = getResource(player, PROTO_BEER_1, STOREROOM);
            addResource(player, PROTO_BEER_2, notBeer - almostBeer);
            addResource(player, PROTO_BEER_1, -notBeer);
            int almostMead = getResource(player, PROTO_MEAD_2, STOREROOM);
            addResource(player, MEAD, almostMead);
            int notMead = getResource(player, PROTO_MEAD_1, STOREROOM);
            addResource(player, PROTO_MEAD_2, notMead - almostMead);
            addResource(player, PROTO_MEAD_1, -notMead);
        }
        advancePilgrims();
        drawBonusTokens();
        marketCards.draw();
        forageCards.draw();
        replenishPigmentInMeadow();
    }


    void checkAtLeastOneMonk() {
        for (int player = 0; player < getNPlayers(); player++) {
            if (monksIn(null, player).isEmpty()) {
                // Hire a free novice!
                int finalPlayer = player;
                ((DiceMonasteryTurnOrder) turnOrder).logEvent(() -> String.format("Player %d gets new Novice due to lack of monks", finalPlayer), this);
                createMonk(1, player);
            }
        }
    }

    public Pilgrimage peekAtNextShortPilgrimage() {
        return pilgrimageDecks.get(0).peek();
    }

    public Pilgrimage peekAtNextLongPilgrimage() {
        return pilgrimageDecks.get(1).peek();
    }

    public MarketCard getCurrentMarket() {
        return marketCards.peek();
    }

    public ForageCard getCurrentForage() {
        return forageCards.peek();
    }

    public Pilgrimage startPilgrimage(Pilgrimage destination, Monk monk) {
        // find card on a deck
        Pilgrimage retValue = null;
        for (Deck<Pilgrimage> deck : pilgrimageDecks) {
            if (deck.getSize() > 0 && deck.peek().equals((destination))) {
                retValue = deck.draw();
                break;
            }
        }
        if (retValue == null)
            throw new AssertionError(String.format("Top card %s is not found", destination));
        retValue.startPilgrimage(monk, this);
        pilgrimagesStarted.add(retValue);
        return retValue;
    }

    public int pilgrimagesLeft(boolean isLong) {
        return pilgrimageDecks.get(isLong ? 1 : 0).getSize();
    }

    public List<Pilgrimage> getPilgrimagesStarted() {
        return pilgrimagesStarted;
    }

    void advancePilgrims() {
        for (Pilgrimage p : pilgrimagesStarted) {
            if (p.isActive()) {
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
                    area.setToken(i, drawToken(rnd));
                }
            }
        }
    }

    void replenishPigmentInMeadow() {
        // remove any left from last season
        for (Resource pigment : new Resource[]{PALE_RED_PIGMENT, PALE_BLUE_PIGMENT, PALE_GREEN_PIGMENT}) {
            int unharvested = getResource(-1, pigment, MEADOW);
            if (unharvested > 0)
                moveCubes(-1, pigment, unharvested, MEADOW, SUPPLY);
        }

        // look at the top Forage card, and add the needed cubes
        ForageCard topCard = forageCards.peek();
        moveCubes(-1, PALE_BLUE_PIGMENT, topCard.blue, SUPPLY, MEADOW);
        moveCubes(-1, PALE_GREEN_PIGMENT, topCard.green, SUPPLY, MEADOW);
        moveCubes(-1, PALE_RED_PIGMENT, topCard.red, SUPPLY, MEADOW);
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
        return Arrays.stream(dma.getTokens()).filter(Objects::nonNull).collect(toList());
    }

    public void removeToken(BONUS_TOKEN token, ActionArea area) {
        BONUS_TOKEN[] tokens = actionAreas.get(area).getTokens();
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] == token) {
                actionAreas.get(area).setToken(i, null);
                return;
            }
        }
        throw new AssertionError(String.format("No %s token found in %s", token, area));
    }

    public void addActionPoints(int number) {
        ((DiceMonasteryTurnOrder) turnOrder).addActionPoints(number);
    }

    public void putToken(ActionArea area, BONUS_TOKEN token, int position) {
        actionAreas.get(area).setToken(position, token);
    }

    void winterHousekeeping() {
        for (int player = 0; player < turnOrder.nPlayers(); player++) {
            // for each player feed monks, and then discard perishables
            List<Monk> monks = monksIn(DORMITORY, player);
            int requiredFood = monks.size();
            DiceMonasteryTurnOrder dmto = (DiceMonasteryTurnOrder) turnOrder;
            requiredFood -= getResource(player, BREAD, STOREROOM);
            if (requiredFood > 0) {
                int honeyEaten = Math.min(requiredFood, getResource(player, HONEY, STOREROOM));
                addResource(player, HONEY, -honeyEaten);
                requiredFood -= honeyEaten;
            }
            if (requiredFood > 0) {
                // monks starve
                int finalRequiredFood = requiredFood;
                int finalPlayer = player;
                dmto.logEvent(() -> String.format("Player %d fails to feed %d of %d monks", finalPlayer, finalRequiredFood, monks.size()), this);
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
            addResource(player, BREAD, -getResource(player, BREAD, STOREROOM));
            if (getParams().calfSkinsRotInWinter)
                addResource(player, CALF_SKIN, -getResource(player, CALF_SKIN, STOREROOM));
            int unharvestedWheat = getResource(player, GRAIN, MEADOW);
            for (int i = 0; i < unharvestedWheat; i++)
                moveCube(player, GRAIN, MEADOW, SUPPLY);
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
        for (Deck<Pilgrimage> pdeck : pilgrimageDecks)
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
        retValue.writtenTexts.putAll(writtenTexts);
        retValue.treasuresCommissioned.putAll(treasuresCommissioned);

        retValue.marketCards = marketCards.copy();
        if (playerId != -1 && marketCards.getSize() > 1) { // shuffle all except the top card
            MarketCard topCard = retValue.marketCards.draw();
            retValue.marketCards.shuffle(rnd);
            retValue.marketCards.add(topCard);
        }
        retValue.forageCards = forageCards.copy();
        if (playerId != -1 && forageCards.getSize() > 1) { // shuffle all except the top card
            ForageCard topCard = retValue.forageCards.draw();
            retValue.forageCards.shuffle(rnd);
            retValue.forageCards.add(topCard);
        }

        for (Pilgrimage p : pilgrimagesStarted)
            retValue.pilgrimagesStarted.add(p.copy());

        for (Deck<Pilgrimage> pilgrimDeck : pilgrimageDecks) {
            Deck<Pilgrimage> copyDeck = pilgrimDeck.copy();
            if (playerId != -1 && copyDeck.getSize() > 1) {// only top card is visible, so shuffle if copied from any player's perspective
                Pilgrimage topCard = copyDeck.draw();
                copyDeck.shuffle(rnd);
                copyDeck.add(topCard);
            }
            retValue.pilgrimageDecks.add(copyDeck);
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
        if (isNotTerminal())
            return getGameScore(playerId) / 60.0;
        return playerResults[playerId].value;
    }

    @Override
    public double getGameScore(int playerId) {
        return (double) (playerTreasuries.get(playerId).getOrDefault(BEER, 0) / 2) +
                playerTreasuries.get(playerId).getOrDefault(MEAD, 0) +
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
                other.writtenTexts.equals(writtenTexts) && other.treasuresCommissioned.equals(treasuresCommissioned) &&
                other.pilgrimagesStarted.equals(pilgrimagesStarted) && other.pilgrimageDecks.equals(pilgrimageDecks) &&
                other.marketCards == marketCards && other.forageCards == forageCards &&
                Arrays.equals(other.victoryPoints, victoryPoints) && Arrays.equals(other.playerResults, playerResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionAreas, allMonks, monkLocations, playerTreasuries, actionsInProgress, gameStatus, gamePhase,
                gameParameters, turnOrder, nextRetirementReward, playerBids, writtenTexts, treasuresCommissioned,
                pilgrimageDecks, pilgrimagesStarted, treasuresOwnedPerPlayer, marketCards, forageCards) +
                31 * Arrays.hashCode(playerResults) + 871 * Arrays.hashCode(victoryPoints);
    }

}
