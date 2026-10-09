package games.risk;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import games.GameType;
import games.risk.components.RiskCard;
import games.risk.components.RiskMission;
import utilities.DeterminisationUtilities;

import java.util.*;

/**
 * <p>State tracked (the board itself is fixed, in RiskParameters.getMap()):</p>
 * <ul>
 *     <li>owner - the player holding each territory (indexed by RiskTerritory.index()), or -1 if unclaimed</li>
 *     <li>armies - the armies on each territory</li>
 *     <li>armiesToPlace - the armies each player still has to put on the board: their remaining starting armies
 *     during the initial placement, then the current player's new armies during REINFORCE</li>
 *     <li>finalPlace - the place each eliminated player finished in (nPlayers for the first out), 0 while in play</li>
 *     <li>drawDeck - the face-down RISK cards, top first</li>
 *     <li>hands - each player's RISK cards</li>
 *     <li>discardPile - the cards traded in, face up</li>
 *     <li>nSetsTraded - the sets traded in so far by all players</li>
 *     <li>capturedThisTurn - the current player has captured a territory this turn</li>
 *     <li>territoryBonusTaken - the current player has had the extra armies for a traded card's territory this
 *     turn</li>
 *     <li>missions - each player's Secret Mission card, seen only by them (none without secretMission)</li>
 *     <li>unusedMissions - the mission cards out of the game, face down</li>
 *     <li>eliminatedBy - the player who eliminated each player, or -1 while they are in</li>
 *     <li>missionWinner - the player who won by completing their mission, or -1</li>
 * </ul>
 * The step of the turn (claiming, reinforcing, attacking, fortifying) is the game phase, a RiskGamePhase.
 * Army pieces (Infantry 1, Cavalry 5, Artillery 10) are only a way of counting, so only the totals are kept.
 */
public class RiskGameState extends AbstractGameState {

    int[] owner;
    int[] armies;
    int[] armiesToPlace;
    int[] finalPlace;
    Deck<RiskCard> drawDeck;
    List<Deck<RiskCard>> hands;
    Deck<RiskCard> discardPile;
    int nSetsTraded;
    boolean capturedThisTurn;
    boolean territoryBonusTaken;
    List<Deck<RiskMission>> missions;
    Deck<RiskMission> unusedMissions;
    int[] eliminatedBy;
    int missionWinner;
    // for testing only (setNextRolls)
    private Deque<Integer> nextRolls;

    public RiskGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Risk;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.add(drawDeck);
        components.addAll(hands);
        components.add(discardPile);
        components.addAll(missions);
        components.add(unusedMissions);
        return components;
    }

    public int getOwner(RiskTerritory territory) {
        return owner[territory.index()];
    }

    public int getArmies(RiskTerritory territory) {
        return armies[territory.index()];
    }

    public int getArmiesToPlace(int player) {
        return armiesToPlace[player];
    }

    public RiskMap getMap() {
        return ((RiskParameters) gameParameters).getMap();
    }

    public Deck<RiskCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<RiskCard> getHand(int player) {
        return hands.get(player);
    }

    public Deck<RiskCard> getDiscardPile() {
        return discardPile;
    }

    public int getNSetsTraded() {
        return nSetsTraded;
    }

    public void setNSetsTraded(int n) {
        nSetsTraded = n;
    }

    public boolean hasCapturedThisTurn() {
        return capturedThisTurn;
    }

    public void setCapturedThisTurn(boolean captured) {
        capturedThisTurn = captured;
    }

    public boolean isTerritoryBonusTaken() {
        return territoryBonusTaken;
    }

    public void setTerritoryBonusTaken(boolean taken) {
        territoryBonusTaken = taken;
    }

    /**
     * The player's Secret Mission card, or null if they hold none.
     */
    public RiskMission getMission(int player) {
        Deck<RiskMission> deck = missions.get(player);
        return deck.getSize() == 0 ? null : deck.peek();
    }

    /**
     * Gives the player the mission card. The card they held goes to the unused missions. The caller takes the new
     * card from wherever it was, so that none is duplicated.
     */
    public void setMission(int player, RiskMission mission) {
        Deck<RiskMission> deck = missions.get(player);
        if (deck.getSize() > 0)
            unusedMissions.add(deck.draw());
        deck.add(mission);
    }

    /**
     * Removes the player's mission card and returns it (null if they hold none).
     */
    public RiskMission takeMission(int player) {
        Deck<RiskMission> deck = missions.get(player);
        return deck.getSize() == 0 ? null : deck.draw();
    }

    public void setEliminatedBy(int player, int by) {
        eliminatedBy[player] = by;
    }

    public void setMissionWinner(int player) {
        missionWinner = player;
    }

    public Deck<RiskMission> getUnusedMissions() {
        return unusedMissions;
    }

    public int getEliminatedBy(int player) {
        return eliminatedBy[player];
    }

    public int getMissionWinner() {
        return missionWinner;
    }

    public int getFinalPlace(int player) {
        return finalPlace[player];
    }

    public boolean isEliminated(int player) {
        return finalPlace[player] > 0;
    }

    /**
     * The territories the player holds, in index order.
     */
    public List<RiskTerritory> getTerritories(int player) {
        List<RiskTerritory> held = new ArrayList<>();
        for (RiskTerritory t : getMap().territories())
            if (owner[t.index()] == player)
                held.add(t);
        return held;
    }

    public int getNTerritories(int player) {
        int n = 0;
        for (int o : owner)
            if (o == player) n++;
        return n;
    }

    public int getTotalArmies(int player) {
        int total = 0;
        for (int i = 0; i < owner.length; i++)
            if (owner[i] == player) total += armies[i];
        return total;
    }

    /**
     * Whether the player holds every territory of the continent.
     */
    public boolean holdsContinent(int player, RiskContinent continent) {
        for (RiskTerritory t : getMap().territories(continent))
            if (owner[t.index()] != player)
                return false;
        return true;
    }

    /**
     * How many more armies the territory may take under maxArmiesPerTerritory (Integer.MAX_VALUE with no limit).
     */
    public int getRoom(RiskTerritory territory) {
        int limit = ((RiskParameters) gameParameters).maxArmiesPerTerritory;
        return limit == 0 ? Integer.MAX_VALUE : limit - armies[territory.index()];
    }

    /**
     * The player's territories that can take another army (all of them with no maxArmiesPerTerritory).
     */
    public List<RiskTerritory> getPlaceableTerritories(int player) {
        List<RiskTerritory> placeable = new ArrayList<>();
        for (RiskTerritory t : getTerritories(player))
            if (getRoom(t) > 0)
                placeable.add(t);
        return placeable;
    }

    /**
     * The player's armies still to place are lost if none of their territories has room for another army
     * (maxArmiesPerTerritory).
     */
    public void loseArmiesWithNoRoom(int player) {
        if (armiesToPlace[player] > 0 && getPlaceableTerritories(player).isEmpty())
            armiesToPlace[player] = 0;
    }

    public void setOwner(RiskTerritory territory, int player) {
        owner[territory.index()] = player;
    }

    /**
     * Changes the armies on the territory by the given amount (negative to remove).
     */
    public void addArmies(RiskTerritory territory, int amount) {
        armies[territory.index()] += amount;
    }

    public void setArmiesToPlace(int player, int amount) {
        armiesToPlace[player] = amount;
    }

    public void setFinalPlace(int player, int place) {
        finalPlace[player] = place;
    }

    /**
     * The new armies the player receives at the start of their turn.
     */
    public int getReinforcements(int player) {
        RiskParameters params = (RiskParameters) gameParameters;
        // one for each territoriesPerArmy territories held, but at least minReinforcements, and the bonus for each
        // continent held in full
        int armies = Math.max(params.minReinforcements, getNTerritories(player) / params.territoriesPerArmy);
        for (RiskContinent c : getMap().continents())
            if (holdsContinent(player, c))
                armies += c.bonus();
        return armies;
    }

    public int getNEliminated() {
        int n = 0;
        for (int place : finalPlace)
            if (place > 0) n++;
        return n;
    }

    /**
     * Rolls one die, giving 1-6. A value queued by setNextRolls is used first.
     */
    public int rollDie() {
        if (nextRolls != null && !nextRolls.isEmpty())
            return nextRolls.poll();
        return getRnd().nextInt(6) + 1;
    }

    /**
     * For testing only: the given values will be the next dice rolled, in order (attacker's dice first, then the
     * defender's, for each roll). Not copied, and not part of equals.
     */
    public void setNextRolls(int... values) {
        nextRolls = new ArrayDeque<>();
        for (int v : values)
            nextRolls.add(v);
    }

    @Override
    protected RiskGameState _copy(int playerId) {
        RiskGameState copy = new RiskGameState(gameParameters, getNPlayers());
        copy.owner = owner.clone();
        copy.armies = armies.clone();
        copy.armiesToPlace = armiesToPlace.clone();
        copy.finalPlace = finalPlace.clone();
        copy.drawDeck = drawDeck.copy();
        copy.hands = new ArrayList<>();
        for (Deck<RiskCard> hand : hands)
            copy.hands.add(hand.copy());
        copy.discardPile = discardPile.copy();
        copy.nSetsTraded = nSetsTraded;
        copy.capturedThisTurn = capturedThisTurn;
        copy.territoryBonusTaken = territoryBonusTaken;
        copy.missions = new ArrayList<>();
        for (Deck<RiskMission> mission : missions)
            copy.missions.add(mission.copy());
        copy.unusedMissions = unusedMissions.copy();
        copy.eliminatedBy = eliminatedBy.clone();
        copy.missionWinner = missionWinner;
        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // the other players' cards are shuffled with the draw deck
            List<Deck<RiskCard>> decks = new ArrayList<>(copy.hands);
            decks.add(copy.drawDeck);
            DeterminisationUtilities.reshuffle(playerId, decks, c -> true, redeterminisationRnd);
            // and the other players' missions with the ones out of the game
            List<Deck<RiskMission>> missionDecks = new ArrayList<>(copy.missions);
            missionDecks.add(copy.unusedMissions);
            DeterminisationUtilities.reshuffle(playerId, missionDecks, c -> true, redeterminisationRnd);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        // win 1, draw 0.5, lose 0
        if (!isNotTerminal())
            return (getPlayerResults()[playerId].value + 1) / 2.0;
        return (double) getNTerritories(playerId) / owner.length;
    }

    /**
     * The number of territories the player holds.
     */
    @Override
    public double getGameScore(int playerId) {
        return getNTerritories(playerId);
    }

    @Override
    public int getOrdinalPosition(int playerId) {
        // a player who completed their Secret Mission is first, and eliminated players are last, in the order they
        // went out; the players still in come between, by territories held, then by total armies
        if (playerId == missionWinner)
            return 1;
        if (isEliminated(playerId))
            return finalPlace[playerId];
        int territories = getNTerritories(playerId);
        int armies = getTotalArmies(playerId);
        int position = missionWinner == -1 ? 1 : 2;
        for (int p = 0; p < getNPlayers(); p++) {
            if (p == playerId || p == missionWinner || isEliminated(p)) continue;
            int t = getNTerritories(p);
            if (t > territories || (t == territories && getTotalArmies(p) > armies))
                position++;
        }
        return position;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RiskGameState that)) return false;
        return Arrays.equals(owner, that.owner) &&
                Arrays.equals(armies, that.armies) &&
                Arrays.equals(armiesToPlace, that.armiesToPlace) &&
                Arrays.equals(finalPlace, that.finalPlace) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(hands, that.hands) &&
                Objects.equals(discardPile, that.discardPile) &&
                nSetsTraded == that.nSetsTraded &&
                capturedThisTurn == that.capturedThisTurn &&
                territoryBonusTaken == that.territoryBonusTaken &&
                Objects.equals(missions, that.missions) &&
                Objects.equals(unusedMissions, that.unusedMissions) &&
                Arrays.equals(eliminatedBy, that.eliminatedBy) &&
                missionWinner == that.missionWinner;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), drawDeck, hands, discardPile, nSetsTraded,
                capturedThisTurn, territoryBonusTaken, missions, unusedMissions, missionWinner);
        result = 31 * result + Arrays.hashCode(eliminatedBy);
        result = 31 * result + Arrays.hashCode(owner);
        result = 31 * result + Arrays.hashCode(armies);
        result = 31 * result + Arrays.hashCode(armiesToPlace);
        result = 31 * result + Arrays.hashCode(finalPlace);
        return result;
    }
}
