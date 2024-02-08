package games.pandemic;

import core.AbstractGameStateWithTurnOrder;
import core.interfaces.IFeatureRepresentation;
import core.interfaces.IGamePhase;
import core.components.*;
import core.properties.*;
import core.components.Area;
import core.AbstractParameters;
import core.turnorders.TurnOrder;
import games.GameType;
import utilities.Hash;

import java.util.*;

import static games.pandemic.PandemicConstants.*;
import static core.CoreConstants.*;
import static games.pandemic.PandemicGameState.PandemicGamePhase.Forecast;


public class PandemicGameState extends AbstractGameStateWithTurnOrder implements IFeatureRepresentation {

    // The Pandemic game phase enum distinguishes 3 more phases on top of the default ones for players forced to
    // discard cards, a player wishing to play a "Forecast" event card
    // or an opportunity to play a "Resilient Population" event card.
    public enum PandemicGamePhase implements IGamePhase {
        DiscardReaction,
        Forecast,
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
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(areas.values());
        components.add(tempDeck);
        components.add(world);
        return components;
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
    public HashMap<HashMap<Integer, Double>, GameResult> getTerminalFeatures(int playerId) {
        HashMap<HashMap<Integer, Double>, GameResult> terminals = new HashMap<>();
        terminals.put(new HashMap<Integer, Double>() {{ put(0, (double) colors.length); }}, GameResult.WIN_GAME);
        terminals.put(new HashMap<Integer, Double>() {{ put(1, 0.0); }}, GameResult.LOSE_GAME);
        terminals.put(new HashMap<Integer, Double>() {{ put(2, (double) ((PandemicParameters)gameParameters).loseMaxOutbreak); }},
                GameResult.LOSE_GAME);
        int i = 3;
        for (String color: colors) {
            int id = i;
            terminals.put(new HashMap<Integer, Double>() {{ put(id, 0.0); }}, GameResult.LOSE_GAME);
            i++;
        }
        return terminals;
    }


    @Override
    protected double _getHeuristicScore(int playerId) {
        return new PandemicHeuristic().evaluateState(this, playerId);
    }

    /**
     * This provides the current score in game terms. This will only be relevant for games that have the concept
     * of victory points, etc.
     * If a game does not support this directly, then just return 0.0
     *
     * @param playerId
     * @return - double, score of current state
     */
    @Override
    public double getGameScore(int playerId) {
        return 0;
    }

    protected void _reset() {
        areas = null;
        tempDeck = null;
        world = null;
        quietNight = false;
        epidemic = false;
        nCardsDrawn = 0;
        researchStationLocations = new ArrayList<>();
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PandemicGameState)) return false;
        if (!super.equals(o)) return false;
        PandemicGameState that = (PandemicGameState) o;
        return quietNight == that.quietNight &&
                epidemic == that.epidemic &&
                nCardsDrawn == that.nCardsDrawn &&
                Objects.equals(areas, that.areas) &&
                Objects.equals(tempDeck, that.tempDeck) &&
                Objects.equals(world, that.world) &&
                Objects.equals(researchStationLocations, that.researchStationLocations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), areas, tempDeck, world, quietNight, epidemic, nCardsDrawn, researchStationLocations);
    }

    /**
     * Constructor. Calls super with objects corresponding to this game and loads the data for the game.
     * @param pp - Game parameters.
     * @param nPlayers - number of players.
     */
    public PandemicGameState(AbstractParameters pp, int nPlayers) {
        super(pp, nPlayers);
    }
    @Override
    protected TurnOrder _createTurnOrder(int nPlayers) {
        return new PandemicTurnOrder(nPlayers, ((PandemicParameters)gameParameters).nActionsPerTurn);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Pandemic;
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

    @Override
    protected AbstractGameStateWithTurnOrder __copy(int playerId) {
        PandemicGameState gs = new PandemicGameState(gameParameters.copy(), getNPlayers());

        gs.areas = new HashMap<>();
        for(int key : areas.keySet())
        {
            Area a = areas.get(key);
            if (playerId != -1 && key == -1) {
                // Hiding face-down decks in game area
                a = new Area(key, "Game area");
                HashMap<Integer, Component> oldComponents = areas.get(key).getComponentsMap();
                for (Map.Entry<Integer, Component> e: oldComponents.entrySet()) {
                    if (gs.getCoreGameParameters().partialObservable && (e.getKey() == playerDeckHash || e.getKey() == infectionHash)) {
                        Deck<Card> hiddenDeck = (Deck<Card>) e.getValue().copy();
                        if (gamePhase == Forecast && e.getKey() == infectionHash) {
                            // Top N cards should be left the same, the rest shuffled
                            hiddenDeck.shuffle(((PandemicParameters)gameParameters).nForecastCards, hiddenDeck.getSize(), redeterminisationRnd);
                        } else {
                            hiddenDeck.shuffle(redeterminisationRnd);  // We know what cards are in there, a simple shuffle is enough
                        }
                        a.putComponent(e.getKey(), hiddenDeck);
                    } else {
                        a.putComponent(e.getKey(), e.getValue().copy());
                    }
                }
            }
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

    void addComponents() {
        super.addAllComponents();
    }
}
