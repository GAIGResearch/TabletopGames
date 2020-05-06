package games.pandemic;

import core.ForwardModel;
import core.actions.*;
import core.components.*;
import core.content.*;
import core.AbstractGameState;
import core.components.Area;
import core.GameParameters;
import core.observations.IObservation;
import games.pandemic.actions.*;
import utilities.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static games.pandemic.PandemicConstants.*;
import static utilities.Utils.generatePermutations;
import static utilities.Utils.indexOf;


public class PandemicGameState extends AbstractGameState implements IObservation {

    public enum GamePhase {
        Main,
        DiscardReaction,
        RPReaction,
        EventReaction
    }

    private HashMap<Integer, Area> areas;
    private PandemicData _data;
    private Deck<Card> tempDeck;

    public Board world;
    private boolean quietNight;
    private boolean epidemic;
    private GamePhase gamePhase;
    private int nCardsDrawn = 0;

    private ArrayList<String> researchStationLocations;

    public PandemicGameState(GameParameters pp, ForwardModel model, int nPlayers) {
        super(pp, model, nPlayers, new PandemicTurnOrder(nPlayers, ((PandemicParameters)pp).n_actions_per_turn));
        researchStationLocations = new ArrayList<>();
        gamePhase = GamePhase.Main;
    }

    public void setComponents(String dataPath) {
        _data = new PandemicData();
        _data.load(dataPath);
        tempDeck = new Deck<>("Temp Deck");
        areas = new HashMap<>();

        // For each player, initialize their own areas: they get a player hand and a player card
        int capacity = ((PandemicParameters)gameParameters).n_cards_per_player.get(getNPlayers());
        for (int i = 0; i < getNPlayers(); i++) {
            Area playerArea = new Area(i);
            Deck<Card> playerHand = new Deck<>("Player Hand");
            playerHand.setCapacity(capacity);
            playerArea.addComponent(PandemicConstants.playerHandHash, playerHand);
            playerArea.addComponent(PandemicConstants.playerCardHash, new Card());
            areas.put(i, playerArea);
        }

        // Initialize the game area: board, player deck, player discard deck, infection deck, infection discard
        // infection rate counter, outbreak counter, diseases x 4
        Area gameArea = new Area(-1);
        areas.put(-1, gameArea);

        // load the board
        world = _data.findBoard("cities"); //world.getNode("name","Valencia");
        gameArea.addComponent(PandemicConstants.pandemicBoardHash, world);

        // Set up the counters
        Counter infection_rate = _data.findCounter("Infection Rate");
        Counter outbreaks = _data.findCounter("Outbreaks");
        gameArea.addComponent(PandemicConstants.infectionRateHash, infection_rate);
        gameArea.addComponent(PandemicConstants.outbreaksHash, outbreaks);

        for (String color : PandemicConstants.colors) {
            int hash = Hash.GetInstance().hash("Disease " + color);
            Counter diseaseC = _data.findCounter("Disease " + color);
            diseaseC.setValue(0);  // 0 - cure not discovered; 1 - cure discovered; 2 - eradicated
            gameArea.addComponent(hash, diseaseC);

            hash = Hash.GetInstance().hash("Disease Cube " + color);
            Counter diseaseCubeCounter = _data.findCounter("Disease Cube " + color);
            gameArea.addComponent(hash, diseaseCubeCounter);
        }

        // Set up decks
        Deck<Card> playerDeck = new Deck<>("Player Deck"); // contains city & event cards
        playerDeck.add(_data.findDeck("Cities"));
        playerDeck.add(_data.findDeck("Events"));

        gameArea.addComponent(PandemicConstants.playerDeckHash, playerDeck);
        gameArea.addComponent(PandemicConstants.playerDeckDiscardHash, new Deck<>("Player Deck Discard"));
        gameArea.addComponent(PandemicConstants.infectionDiscardHash, new Deck<>("Infection Discard"));
        gameArea.addComponent(PandemicConstants.plannerDeckHash, new Deck<>("Planner Deck")); // deck to store extra card for the contingency planner
        gameArea.addComponent(PandemicConstants.infectionHash, _data.findDeck("Infections"));
        gameArea.addComponent(PandemicConstants.playerRolesHash, _data.findDeck("Player Roles"));
        gameArea.addComponent(PandemicConstants.researchStationHash, _data.findCounter("Research Stations"));
    }

    void nextPlayer() {
        turnOrder.endPlayerTurn(this);
        nCardsDrawn = 0;
        gamePhase = GamePhase.Main;
    }

    @Override
    public IObservation getObservation(int player) {
        // TODO
        return this;
    }

    @Override
    public List<IAction> computeAvailableActions() {
        if (!((PandemicTurnOrder)turnOrder).reactionsRemaining()) gamePhase = GamePhase.Main;
        if (gamePhase == GamePhase.DiscardReaction)
                return getDiscardActions();
        else if (gamePhase == GamePhase.RPReaction)
                return getRPactions();
        else if (gamePhase == GamePhase.EventReaction)
                return getEventActions();
        else return getPlayerActions();
    }

    private List<IAction> getPlayerActions() {
        // Create a list for possible actions, including first move actions
        ArrayList<IAction> actions = new ArrayList<>(getMoveActions(turnOrder.getCurrentPlayer(this)));
        actions.addAll(getShareKnowledgeActions());
        actions.addAll(getBuildResearchActions());
        actions.addAll(getTreatDiseaseActions());
        actions.addAll(getDiscoverCureActions());

        // Special role actions
        actions.addAll(getSpecialRoleActions());

        // Done!
        this.numAvailableActions = actions.size();
        this.availableActions = actions;
        return actions;
    }

    private List<IAction> getBuildResearchActions() {
        String roleString = getActingPlayerRole();
        Deck<Card> playerHand = ((Deck<Card>)getComponentActingPlayer(PandemicConstants.playerHandHash));
        PropertyString playerLocationName = (PropertyString) getComponentActingPlayer(PandemicConstants.playerCardHash)
                .getProperty(PandemicConstants.playerLocationHash);
        BoardNode playerLocationNode = world.getNodeByProperty(nameHash, playerLocationName);

        ArrayList<IAction> actions = new ArrayList<>();
        // Build research station, discard card corresponding to current player location to build one, if not already there.
        if (!((PropertyBoolean) playerLocationNode.getProperty(PandemicConstants.researchStationHash)).value
                && ! roleString.equals("Operations Expert")) {
            Card card_in_hand = null;
            for (Card card : playerHand.getElements()) {
                Property cardName = card.getProperty(nameHash);
                if (cardName.equals(playerLocationName)) {
                    card_in_hand = card;
                    break;
                }
            }
            if (card_in_hand != null) {
                actions.addAll(getResearchStationActions(playerLocationName.value, card_in_hand));
            }
        }
        return actions;
    }

    private List<IAction> getTreatDiseaseActions() {
        PandemicParameters pp = (PandemicParameters)this.gameParameters;
        String roleString = getActingPlayerRole();
        PropertyString playerLocationName = (PropertyString) getComponentActingPlayer(PandemicConstants.playerCardHash)
                .getProperty(PandemicConstants.playerLocationHash);
        BoardNode playerLocationNode = world.getNodeByProperty(nameHash, playerLocationName);
        ArrayList<IAction> actions = new ArrayList<>();
        // Treat disease
        PropertyIntArray cityInfections = (PropertyIntArray)playerLocationNode.getProperty(PandemicConstants.infectionHash);
        for (int i = 0; i < cityInfections.getValues().length; i++){
            if (cityInfections.getValues()[i] > 0){
                boolean treatAll = false;
                if (roleString.equals("Medic")) treatAll = true;
                actions.add(new TreatDisease(pp.n_initial_disease_cubes, PandemicConstants.colors[i], playerLocationName.value, treatAll));
            }
        }
        return actions;
    }

    private List<IAction> getShareKnowledgeActions() {
        String roleString = getActingPlayerRole();
        PropertyString playerLocationName = (PropertyString) getComponentActingPlayer(PandemicConstants.playerCardHash)
                .getProperty(PandemicConstants.playerLocationHash);
        Deck<Card> playerHand = ((Deck<Card>)getComponentActingPlayer(PandemicConstants.playerHandHash));
        BoardNode playerLocationNode = world.getNodeByProperty(nameHash, playerLocationName);

        ArrayList<IAction> actions = new ArrayList<>();
        // Share knowledge, give or take card
        // Both players have to be at the same city
        List<Integer> players = ((PropertyIntArrayList)playerLocationNode.getProperty(PandemicConstants.playersBNHash)).getValues();
        for (int i : players) {
            if (i != turnOrder.getCurrentPlayer(this)) {
                // Give card
                for (Card card : playerHand.getElements()) {
                    // Researcher can give any card, others only the card that matches the city name
                    if (roleString.equals("Researcher") || (card.getProperty(PandemicConstants.nameHash)).equals(playerLocationName)) {
                        actions.add(new GiveCard(card, i));
                    }
                }

                // Take card
                Deck<Card>  otherDeck = (Deck<Card>) getComponent(PandemicConstants.playerHandHash, i);
                Card otherPlayerCard = ((Card) getComponent(PandemicConstants.playerCardHash, i));
                String otherRoleString = ((PropertyString) otherPlayerCard.getProperty(nameHash)).value;
                // Can take any card from the researcher or the card that matches the city if the player is in that city
                for (Card card : otherDeck.getElements()) {
                    if (otherRoleString.equals("Researcher") ||
                            (card.getProperty(PandemicConstants.nameHash)).equals(playerLocationName)) {
                        actions.add(new TakeCard(card, i));
                    }
                }
            }
        }
        return actions;
    }

    private List<IAction> getDiscoverCureActions() {
        PandemicParameters pp = (PandemicParameters) this.gameParameters;
        Deck<Card> playerHand = ((Deck<Card>)getComponentActingPlayer(PandemicConstants.playerHandHash));
        String roleString = getActingPlayerRole();

        ArrayList<IAction> actions = new ArrayList<>();
        // Discover a cure, cards of the same colour at a research station
        ArrayList<Card>[] colorCounter = new ArrayList[PandemicConstants.colors.length];
        for (Card card: playerHand.getElements()){
            Property p  = card.getProperty(PandemicConstants.colorHash);
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
                    actions.add(new CureDisease(PandemicConstants.colors[i], colorCounter[i]));
                } else if (colorCounter[i].size() >= pp.n_cards_for_cure){
                    actions.add(new CureDisease(PandemicConstants.colors[i], colorCounter[i]));
                }
            }
        }
        return actions;
    }

    /**
     * Calculate all special actions that can be performed by different player roles. Not included those that can
     * execute the same actions as other players but with different parameters.
     * @return - list of actions for the player role.
     */
    private List<IAction> getSpecialRoleActions() {
        ArrayList<IAction> actions = new ArrayList<>();
        PandemicParameters pp = (PandemicParameters) this.gameParameters;
        Deck<Card> playerHand = ((Deck<Card>)getComponentActingPlayer(PandemicConstants.playerHandHash));
        String roleString = getActingPlayerRole();
        int playerIdx = turnOrder.getCurrentPlayer(this);
        String playerLocation = ((PropertyString) getComponentActingPlayer(playerCardHash)
                .getProperty(playerLocationHash)).value;

        switch (roleString) {
            // Operations expert special actions
            case "Operations Expert":
                if (!(researchStationLocations.contains(playerLocation))) {
                    actions.addAll(getResearchStationActions(playerLocation, null));
                } else {
                    // List all the other nodes with combination of all the city cards in hand
                    for (BoardNode bn : this.world.getBoardNodes()) {
                        for (Card c : playerHand.getElements()) {
                            if (c.getProperty(PandemicConstants.colorHash) != null) {
                                actions.add(new MovePlayerWithCard(playerIdx, ((PropertyString) bn.getProperty(PandemicConstants.nameHash)).value, c));
                            }
                        }
                    }
                }
                break;
            // Dispatcher special actions
            case "Dispatcher":
                // Move any pawn, if its owner agrees, to any city containing another pawn.
                String[] locations = new String[pp.n_players];
                for (int i = 0; i < pp.n_players; i++) {
                    locations[i] = ((PropertyString) getComponent(PandemicConstants.playerCardHash, i)
                            .getProperty(PandemicConstants.playerLocationHash)).value;
                }
                for (int j = 0; j < pp.n_players; j++) {
                    for (int i = 0; i < pp.n_players; i++) {
                        if (i != j) {
                            actions.add(new MovePlayer(i, locations[j]));
                        }
                    }
                }

                // Move another player’s pawn, if its owner agrees, as if it were his own.
                for (int i = 0; i < pp.n_players; i++) {
                    if (i != playerIdx) {
                        actions.addAll(getMoveActions(i));
                    }
                }
                break;
            // Contingency Planner special actions
            case "Contingency Planner":
                Deck<Card> plannerDeck = (Deck<Card>) getComponent(PandemicConstants.plannerDeckHash);
                if (plannerDeck.getElements().size() == 0) {
                    // then can pick up an event card
                    Deck<Card> infectionDiscardDeck = (Deck<Card>) getComponent(PandemicConstants.infectionDiscardHash);
                    ArrayList<Card> infDiscard = infectionDiscardDeck.getElements();
                    for (int i = 0; i < infDiscard.size(); i++) {
                        Card card = infDiscard.get(i);
                        if (card.getProperty(PandemicConstants.colorHash) != null) {
                            actions.add(new DrawCard(infectionDiscardDeck, plannerDeck, i));
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
     * @return - list of AddResearchStation* actions
     */
    private List<IAction> getResearchStationActions(String playerLocation, Card card) {
        ArrayList<IAction> actions = new ArrayList<>();
        Counter rStationCounter = (Counter) getComponent(PandemicConstants.researchStationHash);

        // Check if any research station tokens left
        if (rStationCounter.getValue() == 0) {
            // If all research stations are used, then take one from board
            for (String station : researchStationLocations) {
                if (card == null) actions.add(new AddResearchStationFrom(station, playerLocation));
                else actions.add(new AddResearchStationWithCardFrom(station, playerLocation, card));
            }
        } else {
            // Otherwise can just build here
            if (card == null) actions.add(new AddResearchStation(playerLocation));
            else actions.add(new AddResearchStationWithCard(playerLocation, card));
        }
        return actions;
    }

    /**
     * Calculates all movement actions (drive/ferry, charter flight, direct flight, shuttle flight).
     * @return all movement actions
     */
    private List<IAction> getMoveActions(int playerId){
        ArrayList<IAction> actions = new ArrayList<>();

        // Drive / Ferry add core.actions for travelling immediate cities
        actions.addAll(getDriveFerryActions(playerId));

        // Direct Flight, discard city card and travel to that city
        actions.addAll(getDirectFlightActions(playerId));

        // Charter flight, discard card that matches your city and travel to any city
        actions.addAll(getCharterFlightActions(playerId));

        // Shuttle flight, move from city with research station to any other research station
        actions.addAll(getShuttleFlightActions(playerId));

        return actions;
    }

    private List<IAction> getDriveFerryActions(int playerId) {
        ArrayList<IAction> actions = new ArrayList<>();
        PropertyString playerLocationName = (PropertyString) getComponent(PandemicConstants.playerCardHash, playerId)
                .getProperty(PandemicConstants.playerLocationHash);
        BoardNode playerLocationNode = world.getNodeByProperty(nameHash, playerLocationName);
        for (BoardNode otherCity : playerLocationNode.getNeighbours()){
            actions.add(new MovePlayer(playerId, ((PropertyString)otherCity.getProperty(nameHash)).value));
        }
        return actions;
    }

    private List<IAction> getDirectFlightActions(int playerId) {
        ArrayList<IAction> actions = new ArrayList<>();
        Deck<Card> playerHand = ((Deck<Card>)getComponentActingPlayer(PandemicConstants.playerHandHash));
        for (Card card: playerHand.getElements()){
            //  check if card has country to determine if it is city card or not
            if ((card.getProperty(PandemicConstants.countryHash)) != null){
                actions.add(new MovePlayerWithCard(playerId, ((PropertyString)card.getProperty(nameHash)).value, card));
            }
        }
        return actions;
    }

    private List<IAction> getCharterFlightActions(int playerId) {
        ArrayList<IAction> actions = new ArrayList<>();
        PropertyString playerLocationName = (PropertyString) getComponent(PandemicConstants.playerCardHash, playerId)
                .getProperty(PandemicConstants.playerLocationHash);
        Deck<Card> playerHand = ((Deck<Card>)getComponentActingPlayer(PandemicConstants.playerHandHash));
        for (Card card: playerHand.getElements()){
            // Get the city from the card
            if (playerLocationName.equals(card.getProperty(nameHash))){
                // Add all the cities
                // Iterate over all the cities in the world
                for (BoardNode bn: this.world.getBoardNodes()) {
                    PropertyString destination = (PropertyString) bn.getProperty(nameHash);

                    // Only add the ones that are different from the current location
                    if (!destination.equals(playerLocationName)) {
                        actions.add(new MovePlayerWithCard(playerId, destination.value, card));
                    }
                }
            }
        }
        return actions;
    }

    private List<IAction> getShuttleFlightActions(int playerId) {
        ArrayList<IAction> actions = new ArrayList<>();
        PropertyString playerLocationName = (PropertyString) getComponent(PandemicConstants.playerCardHash, playerId)
                .getProperty(PandemicConstants.playerLocationHash);
        BoardNode playerLocationNode = world.getNodeByProperty(nameHash, playerLocationName);
        // If current city has research station, add every city that has research stations
        if (((PropertyBoolean)playerLocationNode.getProperty(PandemicConstants.researchStationHash)).value) {
            for (String station: researchStationLocations){
                actions.add(new MovePlayer(playerId, station));
            }
        }
        return actions;
    }

    private List<IAction> getDiscardActions() {
        Deck<Card> playerDeck = (Deck<Card>) getComponentActingPlayer(PandemicConstants.playerHandHash);
        Deck<Card> playerDiscardDeck = (Deck<Card>) getComponent(playerDeckDiscardHash);

        ArrayList<IAction> acts = new ArrayList<>();  // Only discard card actions available
        for (int i = 0; i < playerDeck.getElements().size(); i++) {
            acts.add(new DrawCard(playerDeck, playerDiscardDeck, i));  // adding card i from player deck to player discard deck
        }
        return acts;
    }

    // Set removing infection discarded cards (or do nothing) as the only options
    private List<IAction> getRPactions() {
        ArrayList<IAction> acts = new ArrayList<>();
        acts.add(new DoNothing());

        Deck<Card> infectionDiscard = (Deck<Card>) getComponent(infectionDiscardHash);
        int nInfectDiscards = infectionDiscard.getElements().size();
        Deck<Card> ph = (Deck<Card>) getComponentActingPlayer(playerHandHash);
        int nCards = ph.getElements().size();
        for (int cp = 0; cp < nCards; cp++) {
            Card card = ph.getElements().get(cp);
            if (((PropertyString)card.getProperty(nameHash)).value.equals("Resilient Population")) {
                for (int idx = 0; idx < nInfectDiscards; idx++) {
                    acts.add(new RemoveCardWithCard(infectionDiscard, idx, card));
                }
                break;
            }
        }
        return acts;
    }

    /**
     * Calculates all event actions available for the given player.
     * @return - list of all actions available based on event cards owned by the player.
     */
    private List<IAction> getEventActions() {
        PandemicParameters pp = (PandemicParameters) this.gameParameters;

        List<IAction> actions = new ArrayList<>();
        actions.add(new DoNothing());  // Can always do nothing

        Deck<Card> playerHand = ((Deck<Card>) getComponentActingPlayer(PandemicConstants.playerHandHash));
        for (Card card: playerHand.getElements()){
            Property p  = card.getProperty(PandemicConstants.colorHash);
            if (p == null){
                // Event cards don't have colour
                actions.addAll(actionsFromEventCard(card, pp));
            }
        }

        // Contingency planner gets also special deck card
        Card playerCard = ((Card) getComponentActingPlayer(PandemicConstants.playerCardHash));
        String roleString = ((PropertyString)playerCard.getProperty(nameHash)).value;
        if (roleString.equals("Contingency Planner")){
            Deck<Card> plannerDeck = (Deck<Card>) getComponent(PandemicConstants.plannerDeckHash);
            if (plannerDeck.getElements().size() > 0){
                // then can pick up an event card
                actions.addAll(actionsFromEventCard(plannerDeck.peek(), pp));
            }
        }

        return actions;
    }

    /**
     * Calculates action variations based on event card type.
     * @param card - event card to be played
     * @param pp - game parameters
     * @return list of actions corresponding to the event card.
     */
    private List<IAction> actionsFromEventCard(Card card, PandemicParameters pp){
        ArrayList<IAction> actions = new ArrayList<>();
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
                        actions.add(new MovePlayerWithCard(i, cityName, card));
                    }
                }

                Deck<Card> infDeck = (Deck<Card>) getComponent(PandemicConstants.infectionDiscardHash);
                Deck<Card> discardDeck = (Deck<Card>) getComponent(PandemicConstants.playerDeckDiscardHash);

                for (int i = 0; i < infDeck.getElements().size(); i++){
                    actions.add(new DrawCard(infDeck, discardDeck, i));
                }
                break;
            case "Government Grant":
                // "Add 1 research station to any city (no City card needed)."
                for (BoardNode bn: world.getBoardNodes()) {
                    if (!((PropertyBoolean) bn.getProperty(PandemicConstants.researchStationHash)).value) {
                        String cityName = ((PropertyString) bn.getProperty(nameHash)).value;
                        actions.addAll(getResearchStationActions(cityName, card));
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
                // TODO partial observability: leave the top 6 cards as in the real game to allow player to see them
                // generate all permutations
                Deck<Card> infectionDeck = (Deck<Card>) getComponent(infectionHash);
                int nInfectCards = infectionDeck.getElements().size();
                int n = Math.min(nInfectCards, pp.n_forecast_cards);
                ArrayList<int[]> permutations = new ArrayList<>();
                int[] order = new int[n];
                for (int i = 0; i < n; i++) {
                    order[i] = i;
                }
                generatePermutations(n, order, permutations);
                for (int[] perm: permutations) {
                    actions.add(new RearrangeCardsWithCard(infectionDeck, perm, card));
                }
                break;
        }

        return actions;
    }

    //Getters & setters
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

    public String getActingPlayerRole() {
        return getPlayerRole(turnOrder.getCurrentPlayer(this));
    }
    public String getPlayerRole(int player) {
        Card playerCard = ((Card) getComponent(PandemicConstants.playerCardHash, player));
        return ((PropertyString) playerCard.getProperty(nameHash)).value;
    }

    public void addResearchStation(String location) { researchStationLocations.add(location); }
    public void removeResearchStation(String location) { researchStationLocations.remove(location); }

    public GamePhase getGamePhase() {
        return gamePhase;
    }
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

    public PandemicData getData() {
        return _data;
    }

    public void clearTempDeck() {
        tempDeck.clear();
    }
    public Deck<Card> getTempDeck() {
        return tempDeck;
    }

    public void setGamePhase(GamePhase gamePhase) {
        this.gamePhase = gamePhase;
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
        gs.availableActions = new ArrayList<>(availableActions); // TODO: Deep?
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
