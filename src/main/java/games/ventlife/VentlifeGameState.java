package games.ventlife;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.grid.HexCoord;
import core.components.grid.SparseGrid;
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

    public enum Phase implements IGamePhase {DRAFT, PLACE_TILE, PLACE_CREATURES}

    // the top hex at each covered position
    SparseGrid<HexCoord, HexCell> field;
    // the creatures on each occupied hex
    SparseGrid<HexCoord, Creature> creatures;
    Deck<VentTile> drawDeck;
    List<Deck<VentTile>> hands = new ArrayList<>();
    List<Species> speciesInPlay = new ArrayList<>();
    // the species drafted so far, in order (the Advanced Variant): to be used at 2 and 4 players, not used at 3
    List<Species> drafted = new ArrayList<>();
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

    public Map<HexCoord, HexCell> getField() {
        return field.asMap();
    }

    public Map<HexCoord, Creature> getCreatures() {
        return creatures.asMap();
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

    public List<Species> getDrafted() {
        return Collections.unmodifiableList(drafted);
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
    public HexCell getCell(HexCoord hex) {
        return field.get(hex);
    }

    /**
     * The level of the top hex at the position: 0 where no tile covers it.
     */
    public int getLevel(HexCoord hex) {
        HexCell cell = field.get(hex);
        return cell == null ? 0 : cell.level();
    }

    public Creature getCreature(HexCoord hex) {
        return creatures.get(hex);
    }

    /**
     * A covered position with no creature on it.
     */
    public boolean isEmptyHex(HexCoord hex) {
        return field.contains(hex) && !creatures.contains(hex);
    }

    /**
     * Whether the position is an Edge hex: covered, with at least one neighbour that is not.
     */
    public boolean isEdge(HexCoord hex) {
        return field.isOnBorder(hex);
    }

    /**
     * The player's score for one species, as at the end of the game.
     */
    public int speciesScore(int player, Species species) {
        return species.score(this, player);
    }

    // ---------------------------------------------------------------- changes, made by the actions

    public void setCell(HexCoord hex, HexCell cell) {
        field.set(hex, cell);
    }

    public void setCreature(HexCoord hex, Creature creature) {
        creatures.set(hex, creature);
    }

    /**
     * Takes the creatures off the hex, returning them (null if there were none).
     */
    public Creature removeCreature(HexCoord hex) {
        return creatures.remove(hex);
    }

    public void addDrafted(Species species) {
        drafted.add(species);
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
        copy.field = field.copy();
        copy.creatures = creatures.copy();
        copy.drawDeck = drawDeck.copy();
        copy.hands = new ArrayList<>();
        for (Deck<VentTile> h : hands)
            copy.hands.add(h.copy());
        copy.speciesInPlay = new ArrayList<>(speciesInPlay);
        copy.drafted = new ArrayList<>(drafted);
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
        // a win 1, a draw 0.5, a loss 0
        return (getPlayerResults()[playerId].value + 1) / 2.0;
    }

    @Override
    public double getGameScore(int playerId) {
        int score = 0;
        for (Species s : speciesInPlay)
            score += speciesScore(playerId, s);
        return score;
    }

    /**
     * Tier 1 is the player's best species score, and tier 2 their Tube Worm score.
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
                && speciesInPlay.equals(that.speciesInPlay) && drafted.equals(that.drafted)
                && Arrays.deepEquals(supply, that.supply);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), field, creatures, drawDeck, hands, speciesInPlay, drafted, tilesPlaced)
                + 31 * Arrays.deepHashCode(supply);
    }
}
