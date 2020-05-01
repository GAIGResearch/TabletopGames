package pandemic;

import actions.*;
import components.*;
import content.*;
import core.AbstractGameState;
import core.Area;
import core.ForwardModel;
import core.GameParameters;
import observations.Observation;
import pandemic.actions.*;
import players.AbstractPlayer;
import utilities.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static pandemic.Constants.nameHash;


public class PandemicGameState extends AbstractGameState {

    private HashMap<Integer, Area> areas;
    public Board world;
    private int numAvailableActions = 0;
    private boolean quietNight;
    private boolean modelInterrupted;  // Flag notifying if a reaction request interrupted the state update, so it'd continue from there

    private PandemicData _data;
    private Deck tempDeck;

    public PandemicGameState(PandemicParameters gameParameters) {
        super(gameParameters);
        setComponents(gameParameters.getDataPath());
    }

    public void setComponents(String dataPath)
    {
        PandemicParameters pp = (PandemicParameters) this.gameParameters;
        _data = new PandemicData();
        _data.load(dataPath);
        tempDeck = new Deck();
        areas = new HashMap<>();
      
        // For each player, initialize their own areas: they get a player hand and a player card
        for (int i = 0; i < nPlayers; i++) {
            Area playerArea = new Area();
            playerArea.setOwner(i);
            playerArea.addComponent(Constants.playerHandHash, new Deck(pp.max_cards_per_player));
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
        Deck playerDeck = new Deck("Player Deck"); // contains city & event cards
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

    public AbstractGameState createNewGameState() {
        return new PandemicGameState((PandemicParameters) this.gameParameters);
    }

    /**
     * Creates a copy of the game state. Overwriting this method changes the
     * way GameState copies the fields of the super GameState object.
     * This method is called before copyTo().
     * @param playerId id of the player the copy is being prepared for
     * @return a copy of the game state.
     */
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
        gs.quietNight = quietNight;

        gs.areas = new HashMap<>();
        for(int key : areas.keySet())
        {
            Area a = areas.get(key);
            gs.areas.put(key, a.copy());
        }

        gs._data = _data.copy();
    }


    public int nInputActions() {
        return ((PandemicParameters) this.gameParameters).n_actions_per_turn;  // Pandemic requires up to 4 actions per player per turn.
    }

    public int nPossibleActions() {
        return this.numAvailableActions;
    }

    @Override
    public List<IAction> getActions(AbstractPlayer player) {

        // Create a list for possible actions
        ArrayList<IAction> actions = new ArrayList<>();
        PandemicParameters pp = (PandemicParameters) this.gameParameters;

        // get player's hand and role card
        Deck<Card> playerHand = ((Deck<Card>)this.areas.get(activePlayer).getComponent(Constants.playerHandHash));
        Deck discardDeck = (Deck) areas.get(-1).getComponent(Constants.playerDeckDiscardHash);
        Card playerCard = ((Card)this.areas.get(activePlayer).getComponent(Constants.playerCardHash));
        String roleString = ((PropertyString)playerCard.getProperty(nameHash)).value;

        PropertyString playerLocationName = (PropertyString) this.areas.get(activePlayer).getComponent(Constants.playerCardHash).getProperty(Constants.playerLocationHash);
        BoardNode playerLocationNode = world.getNodeByProperty(nameHash, playerLocationName);

        // get research stations from board
        ArrayList<String> researchStations = new ArrayList<>();
        for (BoardNode bn: this.world.getBoardNodes()){
            if (((PropertyBoolean) bn.getProperty(Constants.researchStationHash)).value){
                researchStations.add(((PropertyString)bn.getProperty(nameHash)).value);
            }
        }

        if (playerHand.isOverCapacity()){
            // need to discard a card
            for (int i = 0; i < playerHand.getCards().size(); i++){
                actions.add(new DrawCard(playerHand, discardDeck, i));
            }
            this.numAvailableActions = actions.size();
            return actions;
        }

        // add do nothing action
        actions.add(new DoNothing());

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
        ArrayList<Card>[] colourCounter = new ArrayList[Constants.colors.length];
        for (Card card: playerHand.getCards()){
            Property p  = card.getProperty(Constants.colorHash);
            if (p != null){
                // Only city cards have colours, events don't
                String color = ((PropertyColor)p).valueStr;
            }
        }
                      
        for (int i = 0 ; i < colourCounter.length; i++){
            if (colourCounter[i] != null){
                if (roleString.equals("Scientist") && colourCounter[i].size() >= pp.n_cards_for_cure_reduced){
                    actions.add(new CureDisease(Constants.colors[i], colourCounter[i]));

                } else if (colourCounter[i].size() >= pp.n_cards_for_cure){
                    actions.add(new CureDisease(Constants.colors[i], colourCounter[i]));
                }
            }
        }

        if (roleString.equals("Dispatcher")){
            //  move any pawn, if its owner agrees, to any city
            //containing another pawn,
            String[] locations = new String[pp.n_players];
            for (int i = 0; i < pp.n_players; i++){
                locations[i] = ((PropertyString) this.areas.get(activePlayer).getComponent(Constants.playerCardHash).getProperty(Constants.playerLocationHash)).value;
            }
            for (String loc: locations){
                for (int i = 0; i < pp.n_players; i++) {
                    // todo there are duplicates here
                    actions.add(new MovePlayer(i, loc));
                }
            }

            //  move another player’s pawn, if its owner agrees,
            //as if it were his own.
            for (int i = 0; i < pp.n_players; i++){
                if (i != activePlayer){
                    actions.addAll(getMoveActions(i, playerHand, researchStations));
                }
            }
        }

        if (roleString.equals("Contingency Planner")){
            Deck plannerDeck = (Deck) this.areas.get(-1).getComponent(Constants.plannerDeckHash);
            if (plannerDeck.getCards().size() != 0){
                // then can pick up an event card
                Deck infectionDiscarcDeck = (Deck) this.areas.get(-1).getComponent(Constants.infectionDiscardHash);
                ArrayList<Card> infDiscard = infectionDiscarcDeck.getCards();
                for (int i = 0; i < infDiscard.size(); i++){
                    Card card = infDiscard.get(i);
                    if (card.getProperty(Constants.colorHash) != null){
                        actions.add(new DrawCard(infectionDiscarcDeck, plannerDeck, i));
                    }
                }
            }
            else {
                if (plannerDeck.getCards().size() > 0) {
                    actions.addAll(actionsFromEventCard((Card) plannerDeck.draw(), researchStations));
                }
            }
        }

        // TODO event cards don't count as action and can be played anytime
        for (Card card: playerHand.getCards()){
            Property p  = card.getProperty(Constants.colorHash);
            if (p == null){
                // Event cards don't have colour
                actions.addAll(actionsFromEventCard(card, researchStations));
            }
        }

        this.numAvailableActions = actions.size();

        return actions;
    }

    void nextPlayer() {
        activePlayer = (activePlayer + 1) % nPlayers;
    }
    void setActivePlayer(int p) {
        activePlayer = p;
    }

    public void clearTempDeck() {
        tempDeck.clear();
    }

    public Deck getTempDeck() {
        return tempDeck;
    }

    private List<IAction> getMoveActions(int playerId, Deck<Card> playerHand, List<String> researchStations){
        // playerID - for the player we want to move
        // playerHand - the deck that we use for the movement
        // researchStations - a list of String locations where research stations are present
        ArrayList<IAction> actions = new ArrayList<>();

        PropertyString playerLocationName = (PropertyString) this.areas.get(playerId).getComponent(Constants.playerCardHash).getProperty(Constants.playerLocationHash);
        BoardNode playerLocationNode = world.getNodeByProperty(nameHash, playerLocationName);

        // Drive / Ferry add actions for travelling immediate cities
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

    private List<IAction> actionsFromEventCard(Card card, ArrayList<String> researchStations){
        ArrayList<IAction> actions = new ArrayList<>();
        String cardString = ((PropertyString)card.getProperty(nameHash)).value;

        switch (cardString) {
            case "Resilient Population":
                // Remove any 1 card in the Infection Discard Pile from the game. You may play this between the Infect and Intensify steps of an epidemic.
                Deck infDeck = (Deck) this.areas.get(-1).getComponent(Constants.infectionDiscardHash);
                Deck discardDeck = (Deck) areas.get(-1).getComponent(Constants.playerDeckDiscardHash);

                for (int i = 0; i < infDeck.getCards().size(); i++){
                    actions.add(new DrawCard(infDeck, discardDeck, i));
                }
                break;
            case "Airlift":
                // Move any 1 pawn to any city. Get permission before moving another player's pawn.
                for (int i = 0; i < nPlayers; i++){
                    for (BoardNode bn: world.getBoardNodes())
                    actions.add(new MovePlayer(i, ((PropertyString)bn.getProperty(Constants.nameHash)).value));
                }
                break;
            case "Government Grant":
                // "Add 1 research station to any city (no City card needed)."
                for (BoardNode bn: world.getBoardNodes()) {
                    if (!((PropertyBoolean) bn.getProperty(Constants.researchStationHash)).value) {
                        String cityName = ((PropertyString) bn.getProperty(nameHash)).value;
                        Counter rStationCounter = (Counter) this.areas.get(-1).getComponent(Constants.researchStationHash);
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
                // Skip the next Infect Cities step (do not flip over any Infection cards)
                actions.add(new QuietNight());
                break;
            case "Forecast":
                //"Draw, look at, and rearrange the top 6 cards of the Infection Deck. Put them back on top."
//                Deck infDiscardDeck = game.findDeck("Infection Discard");
//                Deck tmpDeck = game.findDeck(game.tempDeck());
//                for (int i = 0; i < 6; i++){
//                    new DrawCard(infDiscardDeck, tmpDeck).execute();
//                }
                // remove up to 6 cards and put them back on top in random order
                // todo list all combinations?
//
//                for (int i = 0; i < infDiscardDeck.getCards().size(); i++){
//                    actions.add(new DiscardCard(infDiscardDeck, i));
//                }
                break;
        }

        return actions;
    }

    public Component getComponent(int componentId, int playerId)
    {
        return areas.get(playerId).getComponent(componentId);
    }

    public Component getComponent(int componentId)
    {
        return getComponent(componentId, -1);
    }
    
    protected void setQuietNight(boolean qn) {
        quietNight = qn;
    }

    public boolean isQuietNight() {
        return quietNight;
    }

    public boolean wasModelInterrupted() { return modelInterrupted; }
    public void setModelInterrupted(boolean b) { modelInterrupted = b; }


    Area getArea(int playerId) {
        return areas.get(playerId);
    }

    public PandemicData getData() {
        return _data;
    }


    protected int activePlayer;  // Player who's currently taking a turn, index from player list, N+1 is game master, -1 is game
    protected ArrayList<Integer> reactivePlayers;
    protected int nPlayers;

    protected ForwardModel forwardModel;

    //Getters & setters
    public Constants.GameResult getGameStatus() {  return gameStatus; }
    void setForwardModel(ForwardModel fm) { this.forwardModel = fm; }
    ForwardModel getModel() {return this.forwardModel;}
    public GameParameters getGameParameters() { return gameParameters; }

    public void setGameOver(Constants.GameResult status){  this.gameStatus = status; }

    @Override
    public Observation getObservation(AbstractPlayer player) {
        return null;
    }

    @Override
    public void endGame() {

    }

    public int getActingPlayer() {  // Returns player taking an action (or possibly a reaction) next
        if (reactivePlayers.size() == 0)
            return activePlayer;
        else return reactivePlayers.get(0);
    }
    public int getActivePlayer() { return activePlayer; }  // Returns the player whose turn it is, might not be active player
    public ArrayList<Integer> getReactivePlayers() { return reactivePlayers; }  // Returns players queued to react
    public void addReactivePlayer(int player) { reactivePlayers.add(player); }
    public boolean removeReactivePlayer() {
        if (reactivePlayers.size() > 0) {
            reactivePlayers.remove(0);
            return true;
        }
        return false;
    }
    public HashMap<Integer, Area> getAreas() { return areas; }
    public int getNPlayers() { return nPlayers; }
    public void setNPlayers(int nPlayers) { this.nPlayers = nPlayers; }

}
