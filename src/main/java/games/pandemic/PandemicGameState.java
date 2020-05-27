package games.pandemic;

import core.gamephase.GamePhase;
import core.components.*;
import core.properties.*;
import core.AbstractGameState;
import core.components.Area;
import core.GameParameters;
import core.observations.IObservation;

import java.util.*;

import static games.pandemic.PandemicConstants.*;
import static utilities.CoreConstants.*;


public class PandemicGameState extends AbstractGameState implements IObservation {

    // The Pandemic game phase enum distinguishes 2 more phases on top of the default ones for players forced to
    // discard cards, or an opportunity to play a "Resilient Population" event card.
    public enum PandemicGamePhase implements GamePhase {
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
        for (Map.Entry<Integer, Area> e: areas.entrySet()) {
            allComponents.putComponents(e.getValue());
        }
        allComponents.putComponent(tempDeck);
        allComponents.putComponent(world);
    }

    /**
     * Constructor. Calls super with objects corresponding to this game and loads the data for the game.
     * @param pp - Game parameters.
     * @param nPlayers - number of players.
     */
    public PandemicGameState(GameParameters pp, int nPlayers) {
        super(pp, new PandemicTurnOrder(nPlayers, ((PandemicParameters)pp).n_actions_per_turn));
        data = new PandemicData();
        data.load(((PandemicParameters)gameParameters).getDataPath());
    }

    /**
     * Retrieves an observation specific to the given player from this game state object. Components which are not
     * observed by the player are removed, the rest are copied.
     * @param player - player observing this game state.
     * @return - IObservation, the observation for this player.
     */
    @Override
    public IObservation getObservation(int player) {
        // TODO copy all components based on what this player observes
        // TODO partial observability: leave the top 6 cards as in the real game to allow player to see them for RearrangeCardWithCards action
        return this;
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

    /*
    public AbstractGameState createNewGameState() {
        return new PandemicGameState((PandemicParameters) this.gameParameters);
    }

     * Creates a copy of the game state. Overwriting this method changes the
     * way GameState copies the fields of the super GameState object.
     * This method is called before copyTo().
     * @param playerId id of the player the copy is being prepared for
     * @return a copy of the game state.

    protected AbstractGameState _copy(int playerId)
    {
        //Insert code here to change the way super.decks, etc are copied (i.e. for PO).

        //This line below is the same as doing nothing, just here for demonstration purposes.
        return createNewGameState();
    }


    public void copyTo(PandemicGameState dest, int playerId)
    {
        PandemicGameState gs = dest;

        gs.world = this.world.copy();
        gs.numAvailableActions = numAvailableActions;
        gs.availableActions = new ArrayList<>(availableActions); // Deep?
        gs.quietNight = quietNight;
        gs.nCardsDrawn = nCardsDrawn;
        gs.epidemic = epidemic;

        gs.areas = new HashMap<>();
        for(int key : areas.keySet())
        {
            Area a = areas.get(key);
            gs.areas.put(key, a.copy());
        }

        gs._data = _data.copy();
    }
    */

}
