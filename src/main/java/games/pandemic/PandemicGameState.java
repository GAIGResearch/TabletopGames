package games.pandemic;

import core.gamephase.GamePhase;
import core.gamephase.DefaultGamePhase;
import core.ForwardModel;
import core.actions.*;
import core.components.*;
import core.properties.*;
import core.AbstractGameState;
import core.components.Area;
import core.GameParameters;
import core.observations.IObservation;
import games.pandemic.actions.*;

import java.util.*;

import static games.pandemic.PandemicConstants.*;
import static utilities.CoreConstants.*;
import static utilities.Utils.generatePermutations;
import static utilities.Utils.indexOf;


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
     * @param model - forward model.
     * @param nPlayers - number of players.
     */
    public PandemicGameState(GameParameters pp, ForwardModel model, int nPlayers) {
        super(pp, model, new PandemicTurnOrder(nPlayers, ((PandemicParameters)pp).n_actions_per_turn));
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

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of IAction objects.
     */
    @Override
    public List<AbstractAction> computeAvailableActions() {
        if (((PandemicTurnOrder) turnOrder).reactionsFinished()) {
            gamePhase = DefaultGamePhase.Main;
        }
        if (gamePhase == PandemicGamePhase.DiscardReaction)
            return getDiscardActions();
        else if (gamePhase == PandemicGamePhase.RPReaction)
            return getRPactions();
        else if (gamePhase == DefaultGamePhase.PlayerReaction)
            return getEventActions();
        else return getPlayerActions();
    }

    /**
     * Calculates regular player actions.
     * @return - ArrayList, various action types (unique).
     */
    private List<AbstractAction> getPlayerActions() {
        PandemicParameters pp = (PandemicParameters) this.gameParameters;

        // get player's hand, role card, role string, player location name and player location BoardNode
        Deck<Card> playerHand = ((Deck<Card>)getComponentActingPlayer(playerHandHash));
        String roleString = getPlayerRoleActingPlayer();
        PropertyString playerLocationName = (PropertyString) getComponentActingPlayer(playerCardHash)
                .getProperty(playerLocationHash);
        BoardNode playerLocationNode = world.getNodeByProperty(nameHash, playerLocationName);
        int activePlayer = turnOrder.getCurrentPlayer(this);

        // Create a list for possible actions, including first move actions
        Set<AbstractAction> actions = new HashSet<>(getMoveActions(activePlayer, playerHand));

        // Build research station, discard card corresponding to current player location to build one, if not already there.
        if (!((PropertyBoolean) playerLocationNode.getProperty(researchStationHash)).value
                && ! roleString.equals("Operations Expert")) {
            int card_in_hand = -1;
            for (int idx = 0; idx < playerHand.getSize(); idx++) {
                Card card = playerHand.getComponents().get(idx);
                Property cardName = card.getProperty(nameHash);
                if (cardName.equals(playerLocationName)) {
                    card_in_hand = idx;
                    break;
                }
            }
            if (card_in_hand != -1) {
                actions.addAll(getResearchStationActions(playerLocationName.value, playerHand.getComponents().get(card_in_hand), card_in_hand));
            }
        }

        // Treat disease
        PropertyIntArray cityInfections = (PropertyIntArray)playerLocationNode.getProperty(infectionHash);
        for (int i = 0; i < cityInfections.getValues().length; i++){
            if (cityInfections.getValues()[i] > 0){
                boolean treatAll = false;
                if (roleString.equals("Medic")) treatAll = true;

                actions.add(new TreatDisease(pp.n_initial_disease_cubes, colors[i], playerLocationName.value, treatAll));
            }
        }

        // Share knowledge, give or take card, player can only have 7 cards
        // Both players have to be at the same city
        List<Integer> players = ((PropertyIntArrayList)playerLocationNode.getProperty(playersHash)).getValues();
        for (int i : players) {
            if (i != activePlayer) {
                Deck<Card> otherDeck = (Deck<Card>) getComponent(playerHandHash, i);
                String otherRoleString = getPlayerRole(i);

                // Give card
                for (int j = 0; j < playerHand.getSize(); j++) {
                    Card card = playerHand.getComponents().get(j);
                    // Researcher can give any card, others only the card that matches the city name
                    if (roleString.equals("Researcher") || (card.getProperty(nameHash)).equals(playerLocationName)) {
                        actions.add(new DrawCard(playerHand.getComponentID(), otherDeck.getComponentID(), j));
                    }
                }

                // Take card
                // Can take any card from the researcher or the card that matches the city if the player is in that city
                for (int j = 0; j < otherDeck.getSize(); j++) {
                    Card card = otherDeck.getComponents().get(j);
                    if (otherRoleString.equals("Researcher") || (card.getProperty(nameHash)).equals(playerLocationName)) {
                        actions.add(new DrawCard(otherDeck.getComponentID(), playerHand.getComponentID(), j));
                    }
                }
            }
        }

        // Discover a cure, cards of the same colour at a research station
        ArrayList<Card>[] colorCounter = new ArrayList[colors.length];
        for (Card card: playerHand.getComponents()){
            Property p  = card.getProperty(colorHash);
            if (p != null){
                // Only city cards have colours, events don't
                String color = ((PropertyColor)p).valueStr;
                int idx = indexOf(colors, color);
                if (colorCounter[idx] == null)
                    colorCounter[idx] = new ArrayList<>();
                colorCounter[idx].add(card);
            }
        }
        for (int i = 0 ; i < colorCounter.length; i++){
            if (colorCounter[i] != null){
                if (roleString.equals("Scientist") && colorCounter[i].size() >= pp.n_cards_for_cure_reduced){
                    actions.add(new CureDisease(colors[i], colorCounter[i]));
                } else if (colorCounter[i].size() >= pp.n_cards_for_cure){
                    actions.add(new CureDisease(colors[i], colorCounter[i]));
                }
            }
        }

        // Special role actions
        actions.addAll(getSpecialRoleActions(roleString, playerHand, playerLocationName.value));

        // Event actions
        actions.addAll(getEventActions());
        actions.remove(new DoNothing());  // Players can't just do nothing in main game phase

        // Done!
        return new ArrayList<>(actions);
    }

    /**
     * Calculate all special actions that can be performed by different player roles. Not included those that can
     * execute the same actions as other players but with different parameters.
     * @param role - role of player
     * @param playerHand - cards in hand for the player
     * @param playerLocation - current location of player
     * @return - list of actions for the player role.
     */
    private List<AbstractAction> getSpecialRoleActions(String role, Deck<Card> playerHand, String playerLocation) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int playerIdx = turnOrder.getCurrentPlayer(this);

        switch (role) {
            // Operations expert special actions
            case "Operations Expert":
                if (!(researchStationLocations.contains(playerLocation))) {
                    actions.addAll(getResearchStationActions(playerLocation, null, -1));
                } else {
                    // List all the other nodes with combination of all the city cards in hand
                    for (BoardNode bn : this.world.getBoardNodes()) {
                        for (int c = 0; c < playerHand.getSize(); c++) {
                            if (playerHand.getComponents().get(c).getProperty(colorHash) != null) {
                                actions.add(new MovePlayerWithCard(playerIdx, ((PropertyString) bn.getProperty(nameHash)).value, c));
                            }
                        }
                    }
                }
                break;
            // Dispatcher special actions
            case "Dispatcher":
                // Move any pawn, if its owner agrees, to any city containing another pawn.
                String[] locations = new String[turnOrder.nPlayers()];
                for (int i = 0; i < turnOrder.nPlayers(); i++) {
                    locations[i] = ((PropertyString) getComponent(playerCardHash, i)
                            .getProperty(playerLocationHash)).value;
                }
                for (int j = 0; j < turnOrder.nPlayers(); j++) {
                    for (int i = 0; i < turnOrder.nPlayers(); i++) {
                        if (i != j) {
                            actions.add(new MovePlayer(i, locations[j]));
                        }
                    }
                }

                // Move another player’s pawn, if its owner agrees, as if it were his own.
                for (int i = 0; i < turnOrder.nPlayers(); i++) {
                    if (i != playerIdx) {
                        actions.addAll(getMoveActions(i, playerHand));
                    }
                }
                break;
            // Contingency Planner special actions
            case "Contingency Planner":
                Deck<Card> plannerDeck = (Deck<Card>) getComponent(plannerDeckHash);
                if (plannerDeck.getSize() == 0) {
                    // then can pick up an event card
                    Deck<Card> infectionDiscardDeck = (Deck<Card>) getComponent(infectionDiscardHash);
                    List<Card> infDiscard = infectionDiscardDeck.getComponents();
                    for (int i = 0; i < infDiscard.size(); i++) {
                        Card card = infDiscard.get(i);
                        if (card.getProperty(colorHash) != null) {
                            actions.add(new DrawCard(infectionDiscardDeck.getComponentID(), plannerDeck.getComponentID(), i));
                        }
                    }
                }
                break;
        }
        return actions;
    }

    /**
     * Calculates AddResearchStation* actions.
     * @param playerLocation - current location of player
     * @param card - card that is used to play this action, will be discarded. Ignored if null.
     * @param cardIdx - index of card used to play this action (from player hand).
     * @return - list of AddResearchStation* actions
     */
    private List<AbstractAction> getResearchStationActions(String playerLocation, Card card, int cardIdx) {
        Set<AbstractAction> actions = new HashSet<>();
        Counter rStationCounter = (Counter) getComponent(researchStationHash);

        // Check if any research station tokens left
        if (rStationCounter.getValue() == 0) {
            // If all research stations are used, then take one from board
            for (String station : researchStationLocations) {
                if (card == null) actions.add(new AddResearchStationFrom(station, playerLocation));
                else actions.add(new AddResearchStationWithCardFrom(station, playerLocation, cardIdx));
            }
        } else {
            // Otherwise can just build here
            if (card == null) actions.add(new AddResearchStation(playerLocation));
            else actions.add(new AddResearchStationWithCard(playerLocation, cardIdx));
        }
        return new ArrayList<>(actions);
    }

    /**
     * Calculates all movement actions (drive/ferry, charter flight, direct flight, shuttle flight).
     * @param playerId - player to calculate movement for
     * @param playerHand - deck of cards to be used for movement
     * @return all movement actions
     */
    private List<AbstractAction> getMoveActions(int playerId, Deck<Card> playerHand){
        Set<AbstractAction> actions = new HashSet<>();

        PropertyString playerLocationProperty = (PropertyString) getComponent(playerCardHash, playerId)
                .getProperty(playerLocationHash);
        String playerLocationName = playerLocationProperty.value;
        BoardNode playerLocationNode = world.getNodeByProperty(nameHash, playerLocationProperty);
        HashSet<BoardNode> neighbours = playerLocationNode.getNeighbours();

        // Drive / Ferry add actions for travelling to immediate cities
        for (BoardNode otherCity : neighbours){
            actions.add(new MovePlayer(playerId, ((PropertyString)otherCity.getProperty(nameHash)).value));
        }

        // Iterate over all the cities in the world
        for (BoardNode bn: this.world.getBoardNodes()) {
            String destination = ((PropertyString) bn.getProperty(nameHash)).value;

            if (!neighbours.contains(bn)) {  // Ignore neighbours, already covered in Drive/Ferry actions
                for (int c = 0; c < playerHand.getSize(); c++){
                    Card card = playerHand.getComponents().get(c);

                    //  Check if card has country to determine if it is city card or not
                    if ((card.getProperty(countryHash)) != null){
                        String cardCity = ((PropertyString)card.getProperty(nameHash)).value;
                        if (playerLocationName.equals(cardCity)){
                            // Charter flight, discard card that matches your city and travel to any city
                            // Only add the ones that are different from the current location
                            if (!destination.equals(playerLocationName)) {
                                actions.add(new MovePlayerWithCard(playerId, destination, c));
                            }
                        } else if (destination.equals(cardCity)) {
                            // Direct Flight, discard city card and travel to that city
                            actions.add(new MovePlayerWithCard(playerId, cardCity, c));
                        }
                    }
                }
            }
        }

        // Shuttle flight, move from city with research station to any other research station
        // If current city has research station, add every city that has research stations
        if (((PropertyBoolean)playerLocationNode.getProperty(researchStationHash)).value) {
            for (String station: researchStationLocations){
                actions.add(new MovePlayer(playerId, station));
            }
        }

        return new ArrayList<>(actions);
    }

    /**
     * Calculates discard card actions for current player.
     * @return - ArrayList, DrawCard actions (from their hand to the player discard deck).
     */
    private List<AbstractAction> getDiscardActions() {
        Deck<Card> playerDeck = (Deck<Card>) getComponentActingPlayer(playerHandHash);
        Deck<Card> playerDiscardDeck = (Deck<Card>) getComponent(playerDeckDiscardHash);

        Set<AbstractAction> acts = new HashSet<>();  // Only discard card actions available
        for (int i = 0; i < playerDeck.getSize(); i++) {
            acts.add(new DrawCard(playerDeck.getComponentID(), playerDiscardDeck.getComponentID(), i));  // adding card i from player deck to player discard deck
        }
        return new ArrayList<>(acts);
    }

    /**
     * Calculates actions restricted to removing infection discarded cards (or do nothing) for current player.
     * @return - ArrayList, RemoveCardWithCard actions + DoNothing.
     */
    private List<AbstractAction> getRPactions() {
        Set<AbstractAction> acts = new HashSet<>();
        acts.add(new DoNothing());

        Deck<Card> infectionDiscard = (Deck<Card>) getComponent(infectionDiscardHash);
        int nInfectDiscards = infectionDiscard.getSize();
        Deck<Card> ph = (Deck<Card>) getComponentActingPlayer(playerHandHash);
        int nCards = ph.getSize();
        for (int cp = 0; cp < nCards; cp++) {
            Card card = ph.getComponents().get(cp);
            if (((PropertyString)card.getProperty(nameHash)).value.equals("Resilient Population")) {
                for (int idx = 0; idx < nInfectDiscards; idx++) {
                    acts.add(new RemoveCardWithCard(infectionDiscard, idx, card));
                }
                break;
            }
        }
        return new ArrayList<>(acts);
    }

    /**
     * Calculates all event actions available for the given player.
     * @return - list of all actions available based on event cards owned by the player.
     */
    private List<AbstractAction> getEventActions() {
        PandemicParameters pp = (PandemicParameters) this.gameParameters;
        Deck<Card> playerHand = (Deck<Card>) getComponentActingPlayer(playerHandHash);
        Deck<Card> playerDiscard = (Deck<Card>) getComponent(playerDeckDiscardHash);
        int fromDeck = playerHand.getComponentID();
        int toDeck = playerDiscard.getComponentID();

        Set<AbstractAction> actions = new HashSet<>();
        actions.add(new DoNothing());  // Can always do nothing

        for (Card card: playerHand.getComponents()){
            Property p  = card.getProperty(colorHash);
            if (p == null){
                // Event cards don't have colour
                int cardIdx = playerHand.getComponents().indexOf(card);
                actions.addAll(actionsFromEventCard(card, pp, fromDeck, toDeck, cardIdx));
            }
        }

        // Contingency planner gets also special deck card
        Card playerCard = ((Card) getComponentActingPlayer(playerCardHash));
        String roleString = ((PropertyString)playerCard.getProperty(nameHash)).value;
        if (roleString.equals("Contingency Planner")){
            Deck<Card> plannerDeck = (Deck<Card>) getComponent(plannerDeckHash);
            if (plannerDeck.getSize() > 0){
                // then can pick up an event card
                Card card = plannerDeck.peek();
                int cardIdx = playerHand.getComponents().indexOf(card);
                actions.addAll(actionsFromEventCard(card, pp, fromDeck, toDeck, cardIdx));
            }
        }

        return new ArrayList<>(actions);
    }

    /**
     * Calculates action variations based on event card type.
     * @param card - event card to be played
     * @param pp - game parameters
     * @return list of actions corresponding to the event card.
     */
    private List<AbstractAction> actionsFromEventCard(Card card, PandemicParameters pp, int deckFrom, int deckTo, int cardIdx){
        Set<AbstractAction> actions = new HashSet<>();
        String cardString = ((PropertyString)card.getProperty(nameHash)).value;

        switch (cardString) {
            case "Airlift":
//                System.out.println("Airlift");
//            System.out.println("Move any 1 pawn to any city. Get permission before moving another player's pawn.");
                for (BoardNode bn: world.getBoardNodes()) {
                    String cityName = ((PropertyString) bn.getProperty(nameHash)).value;
                    for (int i = 0; i < turnOrder.nPlayers(); i++) {
                        // Check if player is already there
                        String pLocation = ((PropertyString) getComponent(playerCardHash, i).getProperty(playerLocationHash)).value;
                        if (pLocation.equals(cityName)) continue;
                        actions.add(new MovePlayerWithCard(i, cityName, cardIdx));
                    }
                }

                Deck<Card> infDeck = (Deck<Card>) getComponent(infectionDiscardHash);
                Deck<Card> discardDeck = (Deck<Card>) getComponent(playerDeckDiscardHash);

                for (int i = 0; i < infDeck.getSize(); i++){
                    actions.add(new DrawCard(infDeck.getComponentID(), discardDeck.getComponentID(), i));
                }
                break;
            case "Government Grant":
                // "Add 1 research station to any city (no City card needed)."
                for (BoardNode bn: world.getBoardNodes()) {
                    if (!((PropertyBoolean) bn.getProperty(researchStationHash)).value) {
                        String cityName = ((PropertyString) bn.getProperty(nameHash)).value;
                        actions.addAll(getResearchStationActions(cityName, card, cardIdx));
                    }
                }
                break;
            case "One quiet night":
//                System.out.println("One quiet night");
//            System.out.println("Skip the next Infect Cities step (do not flip over any Infection cards).");
                actions.add(new QuietNight(card));
                break;
            case "Forecast":
//                System.out.println("Forecast");
//            System.out.println("Draw, look at, and rearrange the top 6 cards of the Infection Deck. Put them back on top.");
                // Generate all permutations. Each one is a potential action.
                Deck<Card> infectionDeck = (Deck<Card>) getComponent(infectionHash);
                int nInfectCards = infectionDeck.getSize();
                int n = Math.min(nInfectCards, pp.n_forecast_cards);
                ArrayList<int[]> permutations = new ArrayList<>();
                int[] order = new int[n];
                for (int i = 0; i < n; i++) {
                    order[i] = i;
                }
                generatePermutations(n, order, permutations);
                for (int[] perm: permutations) {
                    actions.add(new RearrangeCardsWithCard(deckFrom, deckTo, cardIdx, infectionDeck.getComponentID(), perm));
                }
                break;
        }

        return new ArrayList<>(actions);
    }

    /**
     * Informs turn order of a need to move to the next player and performs any beginning of round setup.
     */
    void nextPlayer() {
        turnOrder.endPlayerTurn(this);
        nCardsDrawn = 0;
        gamePhase = DefaultGamePhase.Main;
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
