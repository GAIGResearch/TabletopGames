package pandemic;

import actions.*;
import components.*;
import content.*;
import core.Area;
import core.GameState;
import pandemic.actions.*;
import utilities.Hash;

import java.util.ArrayList;
import java.util.List;

import static pandemic.Constants.*;
import static utilities.Utils.generatePermutations;

public class PandemicGameState extends GameState {

    public Board world;
    private int numAvailableActions = 0;
    private boolean quietNight;
    private String modelInterrupted = "";  // Flag notifying where a reaction request interrupted the state update, so it'd continue from there

    public void setComponents()
    {
        PandemicParameters pp = (PandemicParameters) this.gameParameters;
      
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
        world = findBoard("cities"); //world.getNode("name","Valencia");

        // Set up the counters
        Counter infection_rate = findCounter("Infection Rate");
        Counter outbreaks = findCounter("Outbreaks");
        gameArea.addComponent(Constants.infectionRateHash, infection_rate);
        gameArea.addComponent(Constants.outbreaksHash, outbreaks);

        for (String color : Constants.colors) {
            int hash = Hash.GetInstance().hash("Disease " + color);
            Counter diseaseC = findCounter("Disease " + color);
            diseaseC.setValue(0);  // 0 - cure not discovered; 1 - cure discovered; 2 - eradicated
            gameArea.addComponent(hash, diseaseC);

            hash = Hash.GetInstance().hash("Disease Cube " + color);
            Counter diseaseCubeCounter = findCounter("Disease Cube " + color);
            gameArea.addComponent(hash, diseaseCubeCounter);
        }

        // Set up decks
        Deck playerDeck = new Deck("Player Deck"); // contains city & event cards
        playerDeck.add((Deck) findDeck("Cities"));
        playerDeck.add((Deck) findDeck("Events"));

        Deck playerDiscard = new Deck("Player Deck Discard");
        Deck infDiscard = new Deck("Infection Discard");
        Deck plannerDeck = new Deck("plannerDeck"); // deck to store extra card for the contingency planner

        gameArea.addComponent(Constants.playerDeckHash, playerDeck);
        gameArea.addComponent(Constants.playerDeckDiscardHash, playerDiscard);
        gameArea.addComponent(Constants.infectionDiscardHash, infDiscard);
        gameArea.addComponent(Constants.plannerDeckHash, plannerDeck);
        gameArea.addComponent(Constants.infectionHash, (Deck) findDeck("Infections"));
        gameArea.addComponent(Constants.playerRolesHash, (Deck) findDeck("Player Roles"));

        // add them to the list of decks, so they are accessible by the findDeck() function
        addDeckToList(playerDeck);
        addDeckToList(playerDiscard);
        addDeckToList(infDiscard);
        addDeckToList(plannerDeck);
    }

    @Override
    public GameState createNewGameState() {
        return new PandemicGameState();
    }

    /**
     * Creates a copy of the game state. Overwriting this method changes the
     * way GameState copies the fields of the super GameState object.
     * This method is called before copyTo().
     * @param playerId id of the player the copy is being prepared for
     * @return a copy of the game state.
     */
    protected GameState _copy(int playerId)
    {
        //Insert code here to change the way super.decks, etc are copied (i.e. for PO).

        //This line below is the same as doing nothing, just here for demonstration purposes.
        return super._copy(playerId);
    }


    @Override
    public void copyTo(GameState dest, int playerId)
    {
        PandemicGameState gs = (PandemicGameState)dest;

        gs.world = this.world.copy();
        gs.numAvailableActions = numAvailableActions;
        gs.quietNight = quietNight;
    }

    public int nInputActions() {
        return ((PandemicParameters) this.gameParameters).n_actions_per_turn;  // Pandemic requires up to 4 actions per player per turn.
    }


    public int nPossibleActions() {
        return this.numAvailableActions;
    }

    @Override
    public List<Action> possibleActions(List<Action> preDetermined) {
        if (preDetermined != null && preDetermined.size() > 0) {
            numAvailableActions = preDetermined.size();
            return preDetermined;
        }

        // Create a list for possible actions
        ArrayList<Action> actions = new ArrayList<>();
        PandemicParameters pp = (PandemicParameters) this.gameParameters;

        // get player's hand and role card
        Deck playerHand = ((Deck)this.areas.get(activePlayer).getComponent(Constants.playerHandHash));
        Card playerCard = ((Card)this.areas.get(activePlayer).getComponent(Constants.playerCardHash));
        String roleString = ((PropertyString)playerCard.getProperty(nameHash)).value;
        Deck playerDiscardDeck = (Deck) findDeck("Player Deck Discard");

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
                actions.add(new DrawCard(playerHand, playerDiscardDeck, i));  // moving card from player hand to player discard
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
                if (findCounter("Research Stations").getValue() == 0) {
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
                if (findCounter("Research Stations").getValue() == 0) {
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
                Deck otherDeck = (Deck) this.areas.get(i).getComponent(Constants.playerHandHash);
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
            if (findDeck("plannerDeck").getCards().size() != 0){
                // then can pick up an event card
                List<Card> infDiscard = findDeck("InfectionDiscard").getCards();
                for (int i = 0; i < infDiscard.size(); i++){
                    Card card = infDiscard.get(i);
                    if (card.getProperty(Constants.colorHash) != null){
                        actions.add(new DrawCard("InfectionDiscard", "plannerDeck", i));
                    }
                }
            }
            else {
                Deck deck = (Deck) findDeck("plannerDeck");
                if (deck.getCards().size() > 0) {
                    actions.addAll(actionsFromEventCard(deck.draw(), researchStations, pp));
                }
            }
        }

        // TODO event cards don't count as action and can be played anytime
        for (Card card: playerHand.getCards()){
            Property p  = card.getProperty(Constants.colorHash);
            if (p == null){
                // Event cards don't have colour
                actions.addAll(actionsFromEventCard(card, researchStations, pp));
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


    private List<Action> getMoveActions(int playerId, Deck playerHand, List<String> researchStations){
        // playerID - for the player we want to move
        // playerHand - the deck that we use for the movement
        // researchStations - a list of String locations where research stations are present
        ArrayList<Action> actions = new ArrayList<>();

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

    private List<Action> actionsFromEventCard(Card card, ArrayList<String> researchStations, PandemicParameters gp){
        ArrayList<Action> actions = new ArrayList<>();
        String cardString = ((PropertyString)card.getProperty(nameHash)).value;

        switch (cardString) {
            case "Resilient Population":
                // Remove any 1 card in the Infection Discard Pile from the game. You may play this between the Infect and Intensify steps of an epidemic.
                Deck infectionDiscardDeck = (Deck) findDeck("Infection Discard");
                for (int i = 0; i < infectionDiscardDeck.getCards().size(); i++){
                    actions.add(new RemoveCardWithCard(infectionDiscardDeck, i, card));
                }
                break;
            case "Airlift":
//                System.out.println("Airlift");
//            System.out.println("Move any 1 pawn to any city. Get permission before moving another player's pawn.");
                for (BoardNode bn: world.getBoardNodes()) {
                    String cityName = ((PropertyString)bn.getProperty(nameHash)).value;
                    for (int i = 0; i < nPlayers; i++) {
                        // Check if player is already there
                        String pLocation = ((PropertyString)areas.get(i).getComponent(playerCardHash).getProperty(playerLocationHash)).value;
                        if (pLocation.equals(cityName)) continue;
                        actions.add(new MovePlayerWithCard(i, cityName, card));
                    }
                }
                break;
            case "Government Grant":
                // "Add 1 research station to any city (no City card needed)."
                for (BoardNode bn: world.getBoardNodes()) {
                    if (!((PropertyBoolean) bn.getProperty(Constants.researchStationHash)).value) {
                        String cityName = ((PropertyString) bn.getProperty(nameHash)).value;
                        if (findCounter("Research Stations").getValue() == 0) {
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
                IDeck infectionDiscard = findDeck("Infection Discard");
                int nInfectDiscards = infectionDiscard.getCards().size();
                int n = Math.max(nInfectDiscards, gp.n_forecast_cards);
                ArrayList<int[]> permutations = new ArrayList<>();
                int[] order = new int[n];
                for (int i = 0; i < n; i++) {
                    order[i] = i;
                }
                generatePermutations(n, order, permutations);
                for (int[] perm: permutations) {
                    actions.add(new RearrangeCardsWithCard("Infection Discard", perm, card));
                }
                break;
        }

        return actions;
    }

    
    protected void setQuietNight(boolean qn) {
        quietNight = qn;
    }

    public boolean isQuietNight() {
        return quietNight;
    }

    public String whereModelInterrupted() { return modelInterrupted; }
    public void setModelInterrupted(String b) { modelInterrupted = b; }
}
