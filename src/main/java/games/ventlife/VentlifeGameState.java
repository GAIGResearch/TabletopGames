package games.ventlife;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.ventlife.components.*;
import utilities.DeterminisationUtilities;

import java.util.*;

/**
 * The vent field (the top hex at each covered position, and the creatures on them), the tiles (the draw deck and the
 * one tile each player holds), the species in play and each player's supply of tokens.
 */
public class VentlifeGameState extends AbstractGameState {

    public enum Phase implements IGamePhase {PLACE_TILE, PLACE_CREATURES}

    // the top hex at each covered position; HexCell is immutable, so a copy shares the values
    Map<Hex, HexCell> field = new HashMap<>();
    // the creatures on each occupied hex; Creature is immutable
    Map<Hex, Creature> creatures = new HashMap<>();
    Deck<VentTile> drawDeck;
    List<Deck<VentTile>> hands = new ArrayList<>();
    List<Species> speciesInPlay = new ArrayList<>();
    // tokens left in each player's supply, by player and Species ordinal
    int[][] supply;
    // the number of tiles placed so far, which is also the id of the next tile placed
    int tilesPlaced;

    public VentlifeGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Ventlife;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> retValue = new ArrayList<>();
        retValue.add(drawDeck);
        retValue.addAll(hands);
        return retValue;
    }

    // ---------------------------------------------------------------- getters

    public VentlifeParameters getParams() {
        return (VentlifeParameters) gameParameters;
    }

    public Map<Hex, HexCell> getField() {
        return Collections.unmodifiableMap(field);
    }

    public Map<Hex, Creature> getCreatures() {
        return Collections.unmodifiableMap(creatures);
    }

    public Deck<VentTile> getDrawDeck() {
        return drawDeck;
    }

    public Deck<VentTile> getHand(int player) {
        return hands.get(player);
    }

    public List<Species> getSpeciesInPlay() {
        return Collections.unmodifiableList(speciesInPlay);
    }

    public int getSupply(int player, Species species) {
        return supply[player][species.ordinal()];
    }

    public int getTilesPlaced() {
        return tilesPlaced;
    }

    // ---------------------------------------------------------------- queries

    /**
     * The top hex at the position, or null if no tile covers it.
     */
    public HexCell getCell(Hex hex) {
        return field.get(hex);
    }

    /**
     * The level of the top hex at the position: 0 where no tile covers it.
     */
    public int getLevel(Hex hex) {
        HexCell cell = field.get(hex);
        return cell == null ? 0 : cell.level();
    }

    public Creature getCreature(Hex hex) {
        return creatures.get(hex);
    }

    /**
     * A covered position with no creature on it.
     */
    public boolean isEmptyHex(Hex hex) {
        return field.containsKey(hex) && !creatures.containsKey(hex);
    }

    /**
     * A covered position next to the deep-sea boundary: at least one of its neighbours is not covered.
     */
    public boolean isEdge(Hex hex) {
        if (!field.containsKey(hex))
            return false;
        for (Hex n : hex.neighbours())
            if (!field.containsKey(n))
                return true;
        return false;
    }

    /**
     * The number of species in play of which the player has no tokens left.
     */
    public int exhaustedSpecies(int player) {
        int n = 0;
        for (Species s : speciesInPlay)
            if (supply[player][s.ordinal()] == 0)
                n++;
        return n;
    }

    /**
     * The player's score for one species, as at the end of the game.
     */
    public int speciesScore(int player, Species species) {
        return species.score(this, player);
    }

    // ---------------------------------------------------------------- changes, made by the actions

    /**
     * Puts a new top hex at the position.
     */
    public void setCell(Hex hex, HexCell cell) {
        field.put(hex, cell);
    }

    public void setCreature(Hex hex, Creature creature) {
        creatures.put(hex, creature);
    }

    /**
     * Takes the creatures off the hex, returning them (null if there were none).
     */
    public Creature removeCreature(Hex hex) {
        return creatures.remove(hex);
    }

    public void changeSupply(int player, Species species, int delta) {
        supply[player][species.ordinal()] += delta;
    }

    /**
     * Counts a tile as placed, returning its id.
     */
    public int takeNextTileId() {
        return tilesPlaced++;
    }

    // ---------------------------------------------------------------- framework

    @Override
    protected VentlifeGameState _copy(int playerId) {
        VentlifeGameState copy = new VentlifeGameState(gameParameters, getNPlayers());
        copy.field = new HashMap<>(field);
        copy.creatures = new HashMap<>(creatures);
        copy.drawDeck = drawDeck.copy();
        copy.hands = new ArrayList<>();
        for (Deck<VentTile> h : hands)
            copy.hands.add(h.copy());
        copy.speciesInPlay = new ArrayList<>(speciesInPlay);
        copy.supply = new int[supply.length][];
        for (int p = 0; p < supply.length; p++)
            copy.supply[p] = supply[p].clone();
        copy.tilesPlaced = tilesPlaced;
        return copy;
    }

    /**
     * Shuffles the tiles hidden from the player - the other players' tiles and the draw deck - among themselves.
     */
    @Override
    public void redeterminise(int playerId) {
        List<Deck<VentTile>> hidden = new ArrayList<>(hands);
        hidden.add(drawDeck);
        DeterminisationUtilities.reshuffle(playerId, hidden, c -> true, redeterminisationRnd);
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            // the score as a share of all players' scores
            double total = 0;
            for (int p = 0; p < getNPlayers(); p++)
                total += getGameScore(p);
            return total == 0 ? 0.5 : getGameScore(playerId) / total;
        }
        return getPlayerResults()[playerId].value;
    }

    @Override
    public double getGameScore(int playerId) {
        int score = 0;
        for (Species s : speciesInPlay)
            score += speciesScore(playerId, s);
        return score;
    }

    /**
     * Tier 1: the player's highest single-species score; tier 2: their Tube Worms score.
     */
    @Override
    public double getTiebreak(int playerId, int tier) {
        return switch (tier) {
            case 1 -> speciesInPlay.stream().mapToInt(s -> speciesScore(playerId, s)).max().orElse(0);
            case 2 -> speciesInPlay.contains(Species.TUBE_WORM) ? speciesScore(playerId, Species.TUBE_WORM) : 0;
            default -> 0;
        };
    }

    @Override
    public int getTiebreakLevels() {
        return 2;
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof VentlifeGameState that)) return false;
        return tilesPlaced == that.tilesPlaced && field.equals(that.field) && creatures.equals(that.creatures)
                && drawDeck.equals(that.drawDeck) && hands.equals(that.hands)
                && speciesInPlay.equals(that.speciesInPlay) && Arrays.deepEquals(supply, that.supply);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), field, creatures, drawDeck, hands, speciesInPlay, tilesPlaced)
                + 31 * Arrays.deepHashCode(supply);
    }
}
