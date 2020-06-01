package games.pandemic;

import core.interfaces.IGamePhase;
import core.components.*;
import core.observations.VectorObservation;
import core.properties.*;
import core.AbstractGameState;
import core.components.Area;
import core.AbstractGameParameters;
import core.interfaces.IObservation;
import players.heuristics.PandemicDiffHeuristic;
import players.heuristics.PandemicHeuristic;
import utilities.Hash;
import utilities.Utils;

import java.util.*;

import static games.pandemic.PandemicConstants.*;
import static core.CoreConstants.*;


public class PandemicGameState extends AbstractGameState implements IObservation {

    // The Pandemic game phase enum distinguishes 2 more phases on top of the default ones for players forced to
    // discard cards, or an opportunity to play a "Resilient Population" event card.
    public enum PandemicGamePhase implements IGamePhase {
        DiscardReaction,
        RPReaction
    }

    // Collection of areas, mapped to player ID. -1 is the general game area containing the board, counters and several decks.
    HashMap<Integer, Area> areas;
    // Temporary deck used as a buffer by several actions.
    Deck<Card> tempDeck;

    // The main game board
    GraphBoard world;
    // Was a quiet night card played?
    boolean quietNight;
    // Was an epidemic card drawn?
    boolean epidemic;
    // How many cards the current player has drawn in their turn
    int nCardsDrawn;
    // Keeps track of locations of all research stations (list of names of cities / board nodes)
    ArrayList<String> researchStationLocations;

    @Override
    public void addAllComponents() {
        for (Area a : areas.values()) {
            allComponents.putComponents(a);
        }
        allComponents.putComponent(tempDeck);
        allComponents.putComponent(world);
    }

    /**
     * Retrieves an observation specific to the given player from this game state object. Components which are not
     * observed by the player are removed, the rest are copied.
     * @return - IObservation, the observation for this player.
     */
    @Override
    public VectorObservation getVectorObservation() {
        return null;
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    public double[] getDistanceFeatures(int playerId) {
        // Win if all disease counters >= 1
        // Lose if player deck is empty
        // Lose if too many outbreaks
        // Lose if no more disease cubes when needed

        double[] features = new double[3 + colors.length];
        int nDiseasesCured = 0;
        int i = 3;
        for (String color: colors) {
            Counter disease = (Counter) getComponent(Hash.GetInstance().hash("Disease " + color));
            if (disease.getValue() >= 1) nDiseasesCured++;

            Counter diseaseCube = (Counter) getComponent(Hash.GetInstance().hash("Disease cube " + color));
            features[i++] = diseaseCube.getValue();
        }

        int nCardsPlayerDeck = ((Deck<Card>)getComponent(playerDeckHash)).getSize();
        int nOutbreaks = ((Counter)getComponent(outbreaksHash)).getValue();
        features[0] = nDiseasesCured;
        features[1] = nCardsPlayerDeck;
        features[2] = nOutbreaks;
        return features;
    }

    @Override
    public HashMap<HashMap<Integer, Double>, Utils.GameResult> getTerminalFeatures(int playerId) {
        HashMap<HashMap<Integer, Double>, Utils.GameResult> terminals = new HashMap<>();
        terminals.put(new HashMap<Integer, Double>() {{ put(0, (double) colors.length); }}, Utils.GameResult.GAME_WIN);
        terminals.put(new HashMap<Integer, Double>() {{ put(1, 0.0); }}, Utils.GameResult.GAME_LOSE);
        terminals.put(new HashMap<Integer, Double>() {{ put(2, (double) ((PandemicParameters)gameParameters).lose_max_outbreak); }},
                Utils.GameResult.GAME_LOSE);
        int i = 3;
        for (String color: colors) {
            int id = i;
            terminals.put(new HashMap<Integer, Double>() {{ put(id, 0.0); }}, Utils.GameResult.GAME_LOSE);
            i++;
        }
        return terminals;
    }

    @Override
    public double getScore(int playerId) {
        // Martin's heuristic. // TODO maybe improvements?
        PandemicHeuristic ph = new PandemicHeuristic(this);
        return ph.evaluateState(this);
    }

    /**
     * Constructor. Calls super with objects corresponding to this game and loads the data for the game.
     * @param pp - Game parameters.
     * @param nPlayers - number of players.
     */
    public PandemicGameState(AbstractGameParameters pp, int nPlayers) {
        super(pp, new PandemicTurnOrder(nPlayers, ((PandemicParameters)pp).n_actions_per_turn));
        data = new PandemicData();
        data.load(((PandemicParameters)gameParameters).getDataPath());
    }

    // Getters & setters
    public Component getComponent(int componentId, int playerId) {
        return areas.get(playerId).getComponent(componentId);
    }
    public Component getComponentActingPlayer(int componentId) {
        return areas.get(turnOrder.getCurrentPlayer(this)).getComponent(componentId);
    }
    public Component getComponent(int componentId) {
        return getComponent(componentId, -1);
    }
    Area getArea(int playerId) {
        return areas.get(playerId);
    }
    public void addResearchStation(String location) { researchStationLocations.add(location); }
    public void removeResearchStation(String location) { researchStationLocations.remove(location); }
    public void setQuietNight(boolean qn) {
        quietNight = qn;
    }
    public boolean isQuietNight() {
        return quietNight;
    }
    public void setEpidemic(boolean epidemic) {
        this.epidemic = epidemic;
    }
    public boolean isEpidemic() {
        return epidemic;
    }
    public void cardWasDrawn() {
        nCardsDrawn++;
    }
    public void setNCardsDrawn(int nCardsDrawn) {
        this.nCardsDrawn = nCardsDrawn;
    }
    public int getNCardsDrawn() {
        return nCardsDrawn;
    }
    public void clearTempDeck() {
        tempDeck.clear();
    }
    public Deck<Card> getTempDeck() {
        return tempDeck;
    }
    public String getPlayerRoleActingPlayer() {
        return getPlayerRole(turnOrder.getCurrentPlayer(this));

    }
    public String getPlayerRole(int i) {
        Card playerCard = ((Card) getComponent(playerCardHash, i));
        return ((PropertyString) playerCard.getProperty(nameHash)).value;
    }
    public GraphBoard getWorld() {
        return world;
    }

    PandemicData getData() {  // Only FM should have access to this for initialisation
        return (PandemicData)data;
    }

    @Override
    protected AbstractGameState copy(int playerId) {
        // TODO copy all components based on what this player observes
        // TODO partial observability: leave the top 6 cards as in the real game to allow player to see them for RearrangeCardWithCards action

        PandemicGameState gs = new PandemicGameState(gameParameters, getNPlayers());

        gs.areas = new HashMap<>();
        for(int key : areas.keySet())
        {
            Area a = areas.get(key);
            gs.areas.put(key, a.copy());
        }
        gs.tempDeck = tempDeck.copy();

        gs.world = world.copy();
        gs.quietNight = quietNight;
        gs.epidemic = epidemic;
        gs.nCardsDrawn = nCardsDrawn;

        gs.researchStationLocations = new ArrayList<>(researchStationLocations);

        return gs;
    }
}
