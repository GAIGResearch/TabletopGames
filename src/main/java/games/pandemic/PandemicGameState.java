package games.pandemic;

import core.actions.*;
import core.components.*;
import core.content.*;
import core.AbstractGameState;
import core.Area;
import core.GameParameters;
import core.observations.Observation;
import games.pandemic.actions.*;
import utilities.Hash;
import utilities.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static games.pandemic.Constants.*;
import static utilities.Utils.generatePermutations;
import static utilities.Utils.indexOf;


@SuppressWarnings("unchecked")
public class PandemicGameState extends AbstractGameState implements Observation {

    private HashMap<Integer, Area> areas;
    private PandemicData _data;
    private Deck<Card> tempDeck;

    protected ArrayList<Pair<Integer, List<IAction>>> reactivePlayers;

    public Board world;
    private boolean quietNight;
    private boolean epidemic;
    private int nCardsDrawn = 0;

    public PandemicGameState(GameParameters pp, int nPlayers) {
        super(pp, nPlayers);
        this.nPlayers = nPlayers;
        reactivePlayers = new ArrayList<>();
    }

    public void setComponents(String dataPath) {
        PandemicParameters pp = (PandemicParameters) this.gameParameters;
        _data = new PandemicData();
        _data.load(dataPath);
        tempDeck = new Deck<>();
        areas = new HashMap<>();

        // For each player, initialize their own areas: they get a player hand and a player card
        for (int i = 0; i < getNPlayers(); i++) {
            Area playerArea = new Area();
            playerArea.setOwner(i);
            playerArea.addComponent(Constants.playerHandHash, new Deck<>(pp.max_cards_per_player));
            playerArea.addComponent(Constants.playerCardHash, new Card());
            areas.put(i, playerArea);
        }

        // Initialize the game area: board, player deck, player discard deck, infection deck, infection discard
        // infection rate counter, outbreak counter, diseases x 4
        Area gameArea = new Area();
        gameArea.setOwner(-1);
        gameArea.addComponent(Constants.pandemicBoardHash, world);
        areas.put(-1, gameArea);

        // load the board
        world = _data.findBoard("cities"); //world.getNode("name","Valencia");

        // Set up the counters
        Counter infection_rate = _data.findCounter("Infection Rate");
        Counter outbreaks = _data.findCounter("Outbreaks");
        gameArea.addComponent(Constants.infectionRateHash, infection_rate);
        gameArea.addComponent(Constants.outbreaksHash, outbreaks);

        for (String color : Constants.colors) {
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

        Deck<Card>  playerDiscard = new Deck<> ("Player Deck Discard");
        Deck<Card>  infDiscard = new Deck<> ("Infection Discard");
        Deck<Card>  plannerDeck = new Deck<> ("plannerDeck"); // deck to store extra card for the contingency planner

        gameArea.addComponent(Constants.playerDeckHash, playerDeck);
        gameArea.addComponent(Constants.playerDeckDiscardHash, playerDiscard);
        gameArea.addComponent(Constants.infectionDiscardHash, infDiscard);
        gameArea.addComponent(Constants.plannerDeckHash, plannerDeck);
        gameArea.addComponent(Constants.infectionHash, _data.findDeck("Infections"));
        gameArea.addComponent(Constants.playerRolesHash, _data.findDeck("Player Roles"));
        gameArea.addComponent(Constants.researchStationHash, _data.findCounter("Research Stations"));
    }

    void nextPlayer() {
        activePlayer = (activePlayer + 1) % getNPlayers();
    }

    @Override
    public Observation getObservation(int player) {
        // TODO
        return null;
    }

    @Override
    public void endGame() {
        // TODO
    }

    @Override
    public List<IAction> computeAvailableActions(int player) {
        // Create a list for possible core.actions
        ArrayList<IAction> actions = new ArrayList<>();
        PandemicParameters pp = (PandemicParameters) this.gameParameters;

        // get player's hand and role card
        Deck<Card> playerHand = ((Deck<Card>)getComponent(Constants.playerHandHash, activePlayer));
        Card playerCard = ((Card) getComponent(Constants.playerCardHash, activePlayer));
        String roleString = ((PropertyString) playerCard.getProperty(nameHash)).value;

        PropertyString playerLocationName = (PropertyString) getComponent(Constants.playerCardHash,activePlayer)
                .getProperty(Constants.playerLocationHash);
        BoardNode playerLocationNode = world.getNodeByProperty(nameHash, playerLocationName);

        ArrayList<String> researchStations = new ArrayList<>();
        for (BoardNode bn: this.world.getBoardNodes()){
            if (((PropertyBoolean) bn.getProperty(Constants.researchStationHash)).value){
                researchStations.add(((PropertyString)bn.getProperty(nameHash)).value);
            }
        }

        // add do nothing action
        actions.add(new DoNothing());  // TODO: this should not be an option

        actions.addAll(getMoveActions(activePlayer, playerHand, researchStations));

        // Build research station, discard card with that city to build one,
        // Check if there is not already a research station there
        if (!((PropertyBoolean) playerLocationNode.getProperty(Constants.researchStationHash)).value) {
            // check if role is operations expert
            if (roleString.equals("Operations Expert")){
                // can be a research station in the current city
                Counter rStationCounter = (Counter) this.areas.get(-1).getComponent(Constants.researchStationHash);
                if (rStationCounter.getValue() == 0) {
                    // If all research stations are used, then take one from board
                    for (String station : researchStations) {
                        actions.add(new AddResearchStationFrom(station, playerLocationName.value));
                    }
                } else {
                    // Otherwise can just build here
                    actions.add(new AddResearchStation(playerLocationName.value));
                }
            }

            // normal build research station logic
            // Check player has card in hand
            Card card_in_hand = null;
            for (Card card: playerHand.getCards()){
                Property cardName = card.getProperty(nameHash);
                if (cardName.equals(playerLocationName)){
                    card_in_hand = card;
                    break;
                }
            }
            if (card_in_hand != null) {
                // Check if any research station tokens left
                Counter rStationCounter = (Counter) this.areas.get(-1).getComponent(Constants.researchStationHash);
                if (rStationCounter.getValue() == 0) {
                    // If all research stations are used, then take one from board
                    for (String station : researchStations) {
                        actions.add(new AddResearchStationWithCardFrom(station, playerLocationName.value, card_in_hand));
                    }
                } else {
                    // Otherwise can just build here
                    actions.add(new AddResearchStationWithCard(playerLocationName.value, card_in_hand));
                }
            }
        } else {
            // Operations Expert can travel from any city with research station to any city by discarding any card
            if (roleString.equals("Operations Expert")){
                // list all the other nodes with combination of all the city cards in hand
                for (BoardNode bn: this.world.getBoardNodes()) {
                    for (Card c : playerHand.getCards()) {
                        if (c.getProperty(Constants.colorHash) != null) {
                            new MovePlayerWithCard(activePlayer, ((PropertyString) bn.getProperty(Constants.nameHash)).value, c);
                        }
                    }
                }
            }
        }

        // Treat disease
        PropertyIntArray cityInfections = (PropertyIntArray)playerLocationNode.getProperty(Constants.infectionHash);
        for (int i = 0; i < cityInfections.getValues().length; i++){
            if (cityInfections.getValues()[i] > 0){
                boolean treatAll = false;
                if (roleString.equals("Medic")) treatAll = true;
                actions.add(new TreatDisease(pp.n_initial_disease_cubes, Constants.colors[i], playerLocationName.value, treatAll));
            }
        }

        // Share knowledge, give or take card, player can only have 7 cards
        // both players have to be at the same city
        List<Integer> players = ((PropertyIntArrayList)playerLocationNode.getProperty(Constants.playersBNHash)).getValues();
        for (int i : players) {
            if (i != activePlayer) {
                // give card
                for (Card card : playerHand.getCards()) {
                    // researcher can give any card, others only the card that matches the city name
                    if (roleString.equals("Researcher") || (card.getProperty(Constants.nameHash)).equals(playerLocationName)) {
                        actions.add(new GiveCard(card, i));
                    }
                }

                // take card
                Deck<Card>  otherDeck = (Deck<Card>) this.areas.get(i).getComponent(Constants.playerHandHash);
                Card otherPlayerCard = ((Card)this.areas.get(i).getComponent(Constants.playerCardHash));
                String otherRoleString = ((PropertyString)otherPlayerCard.getProperty(nameHash)).value;
                // can take any card from the researcher or the card that matches the city if the player is in that city
                for (Card card : otherDeck.getCards()) {
                    if (otherRoleString.equals("Researcher") || (card.getProperty(Constants.nameHash)).equals(playerLocationName)) {
                        actions.add(new TakeCard(card, i));
                    }
                }
            }
        }

        // Discover a cure, cards of the same colour at a research station
        ArrayList<Card>[] colorCounter = new ArrayList[Constants.colors.length];
        for (Card card: playerHand.getCards()){
            Property p  = card.getProperty(Constants.colorHash);
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
                    actions.add(new CureDisease(Constants.colors[i], colorCounter[i]));
                } else if (colorCounter[i].size() >= pp.n_cards_for_cure){
                    actions.add(new CureDisease(Constants.colors[i], colorCounter[i]));
                }
            }
        }

        if (roleString.equals("Dispatcher")){
            // move any pawn, if its owner agrees, to any city containing another pawn.
            String[] locations = new String[pp.n_players];
            for (int i = 0; i < pp.n_players; i++){
                locations[i] = ((PropertyString) getComponent(Constants.playerCardHash, i).getProperty(Constants.playerLocationHash)).value;
            }
            for (int j = 0; j < pp.n_players; j++){
                for (int i = 0; i < pp.n_players; i++) {
                    if (i != j) {
                        actions.add(new MovePlayer(i, locations[j]));
                    }
                }
            }

            // move another player’s pawn, if its owner agrees, as if it were his own.
            for (int i = 0; i < pp.n_players; i++){
                if (i != activePlayer){
                    actions.addAll(getMoveActions(i, playerHand, researchStations));
                }
            }
        }

        if (roleString.equals("Contingency Planner")){
            Deck<Card> plannerDeck = (Deck<Card>) getComponent(Constants.plannerDeckHash);
            if (plannerDeck.getCards().size() == 0){
                // then can pick up an event card
                Deck<Card> infectionDiscardDeck = (Deck<Card>) getComponent(Constants.infectionDiscardHash);
                ArrayList<Card> infDiscard = infectionDiscardDeck.getCards();
                for (int i = 0; i < infDiscard.size(); i++){
                    Card card = infDiscard.get(i);
                    if (card.getProperty(Constants.colorHash) != null){
                        actions.add(new DrawCard(infectionDiscardDeck, plannerDeck, i));
                    }
                }
            }
        }

        this.numAvailableActions = actions.size();
        this.availableActions = actions;
        return actions;
    }

    private List<IAction> getMoveActions(int playerId, Deck<Card> playerHand, List<String> researchStations){
        // playerID - for the player we want to move
        // playerHand - the deck that we use for the movement
        // researchStations - a list of String locations where research stations are present
        ArrayList<IAction> actions = new ArrayList<>();

        PropertyString playerLocationName = (PropertyString) this.areas.get(playerId).getComponent(Constants.playerCardHash).getProperty(Constants.playerLocationHash);
        BoardNode playerLocationNode = world.getNodeByProperty(nameHash, playerLocationName);

        // Drive / Ferry add core.actions for travelling immediate cities
        for (BoardNode otherCity : playerLocationNode.getNeighbours()){
            actions.add(new MovePlayer(playerId, ((PropertyString)otherCity.getProperty(nameHash)).value));
        }

        // Direct Flight, discard city card and travel to that city
        for (Card card: playerHand.getCards()){
            //  check if card has country to determine if it is city card or not
            if ((card.getProperty(Constants.countryHash)) != null){
                actions.add(new MovePlayerWithCard(playerId, ((PropertyString)card.getProperty(nameHash)).value, card));
            }
        }

        // charter flight, discard card that matches your city and travel to any city
        for (Card card: playerHand.getCards()){
            // get the city from the card
            if (playerLocationName.equals(card.getProperty(nameHash))){
                // add all the cities
                // iterate over all the cities in the world
                for (BoardNode bn: this.world.getBoardNodes()) {
                    PropertyString destination = (PropertyString) bn.getProperty(nameHash);

                    // only add the ones that are different from the current location
                    if (!destination.equals(playerLocationName)) {
                        actions.add(new MovePlayerWithCard(playerId, destination.value, card));
                    }
                }
            }
        }

        // shuttle flight, move from city with research station to any other research station

        // if current city has research station, add every city that has research stations
        if (((PropertyBoolean)playerLocationNode.getProperty(Constants.researchStationHash)).value) {
            for (String station: researchStations){
                actions.add(new MovePlayer(playerId, station));
            }
        }

        return actions;
    }

    List<IAction> getEventActions(int playerId) {
        PandemicParameters pp = (PandemicParameters) this.gameParameters;

        List<IAction> actions = new ArrayList<>();
        actions.add(new DoNothing());  // Can always do nothing

        Deck<Card> playerHand = ((Deck<Card>) getComponent(Constants.playerHandHash, playerId));
        for (Card card: playerHand.getCards()){
            Property p  = card.getProperty(Constants.colorHash);
            if (p == null){
                // Event cards don't have colour
                actions.addAll(actionsFromEventCard(card, pp));
            }
        }

        // Contingency planner gets also special deck card
        Card playerCard = ((Card) getComponent(Constants.playerCardHash, playerId));
        String roleString = ((PropertyString)playerCard.getProperty(nameHash)).value;
        if (roleString.equals("Contingency Planner")){
            Deck<Card> plannerDeck = (Deck<Card>) getComponent(Constants.plannerDeckHash);
            if (plannerDeck.getCards().size() > 0){
                // then can pick up an event card
                actions.addAll(actionsFromEventCard(plannerDeck.draw(), pp));
            }
        }

        return actions;
    }

    private List<IAction> actionsFromEventCard(Card card, PandemicParameters pp){
        ArrayList<IAction> actions = new ArrayList<>();
        String cardString = ((PropertyString)card.getProperty(nameHash)).value;

        switch (cardString) {
            case "Airlift":
//                System.out.println("Airlift");
//            System.out.println("Move any 1 pawn to any city. Get permission before moving another player's pawn.");
                for (BoardNode bn: world.getBoardNodes()) {
                    String cityName = ((PropertyString) bn.getProperty(nameHash)).value;
                    for (int i = 0; i < nPlayers; i++) {
                        // Check if player is already there
                        String pLocation = ((PropertyString) getComponent(playerCardHash, i).getProperty(playerLocationHash)).value;
                        if (pLocation.equals(cityName)) continue;
                        actions.add(new MovePlayerWithCard(i, cityName, card));
                    }
                }

                Deck<Card> infDeck = (Deck<Card>) getComponent(Constants.infectionDiscardHash);
                Deck<Card> discardDeck = (Deck<Card>) getComponent(Constants.playerDeckDiscardHash);

                for (int i = 0; i < infDeck.getCards().size(); i++){
                    actions.add(new DrawCard(infDeck, discardDeck, i));
                }
                break;
            case "Government Grant":

                // TODO: this could be more efficient, save this list in a PandemicBoard component and update when
                // research stations are added/removed from the board, then just retrieve the list from here
                ArrayList<String> researchStations = new ArrayList<>();
                for (BoardNode bn: this.world.getBoardNodes()){
                    if (((PropertyBoolean) bn.getProperty(Constants.researchStationHash)).value){
                        researchStations.add(((PropertyString)bn.getProperty(nameHash)).value);
                    }
                }

                // "Add 1 research station to any city (no City card needed)."
                for (BoardNode bn: world.getBoardNodes()) {
                    if (!((PropertyBoolean) bn.getProperty(Constants.researchStationHash)).value) {
                        String cityName = ((PropertyString) bn.getProperty(nameHash)).value;
                        Counter rStationCounter = (Counter) getComponent(Constants.researchStationHash);
                        if (rStationCounter.getValue() == 0) {
                            // If all research stations are used, then take one from board
                            for (String stations : researchStations) {
                                actions.add(new AddResearchStationWithCardFrom(stations, cityName, card));
                            }
                        } else {
                            // Otherwise can just build here
                            actions.add(new AddResearchStationWithCard(cityName, card));
                        }
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
                Deck<Card> infectionDiscard = (Deck<Card>) getComponent(infectionDiscardHash);
                int nInfectDiscards = infectionDiscard.getCards().size();
                int n = Math.max(nInfectDiscards, pp.n_forecast_cards);
                ArrayList<int[]> permutations = new ArrayList<>();
                int[] order = new int[n];
                for (int i = 0; i < n; i++) {
                    order[i] = i;
                }
                generatePermutations(n, order, permutations);
                for (int[] perm: permutations) {
                    actions.add(new RearrangeCardsWithCard(infectionDiscard, perm, card));
                }
                break;
        }

        return actions;
    }

    //Getters & setters
    public HashMap<Integer, Area> getAreas() { return areas; }
    public Component getComponent(int componentId, int playerId) {
        return areas.get(playerId).getComponent(componentId);
    }
    public Component getComponent(int componentId) {
        return getComponent(componentId, -1);
    }
    Area getArea(int playerId) {
        return areas.get(playerId);
    }

    void setActivePlayer(int p) {
        activePlayer = p;
    }
    public int getActivePlayer() { return activePlayer; }  // Returns the player whose turn it is, might not be active player
    public ArrayList<Pair<Integer,List<IAction>>> getReactivePlayers() { return reactivePlayers; }  // Returns players queued to react
    public Pair<Integer, List<IAction>> getActingPlayer() {  // Returns player taking an action (or possibly a reaction) next
        if (reactivePlayers.size() == 0)
            return new Pair<>(activePlayer, null);
        else return reactivePlayers.get(0);
    }
    public int getActingPlayerID() {  // Returns player taking an action (or possibly a reaction) next
        if (reactivePlayers.size() == 0)
            return activePlayer;
        else return reactivePlayers.get(0).a;
    }
    public List<IAction> getActingPlayerActions() {
        if (reactivePlayers.size() == 0)
            return availableActions;
        else return reactivePlayers.get(0).b;
    }
    public void addReactivePlayer(int player, List<IAction> actionList) {
        reactivePlayers.add(new Pair<>(player, actionList));
    }
    public boolean removeReactivePlayer() {
        if (reactivePlayers.size() > 0) {
            reactivePlayers.remove(0);
            return true;
        }
        return false;
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
