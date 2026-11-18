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

    public enum Phase implements IGamePhase {DRAFT, PLACE_TILE, PLACE_CREATURES}

    // the top hex at each covered position
    Map<Hex, HexCell> field = new HashMap<>();
    // the creatures on each occupied hex
    Map<Hex, Creature> creatures = new HashMap<>();
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
     * Whether the position is an Edge hex: covered, with at least one neighbour that is not.
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
     * The player's score for one species, as at the end of the game.
     */
    public int speciesScore(int player, Species species) {
        return species.score(this, player);
    }

    // ---------------------------------------------------------------- changes, made by the actions

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
        copy.field = new HashMap<>(field);
        copy.creatures = new HashMap<>(creatures);
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
