package pandemic;

import actions.*;
import components.*;
import content.*;
import core.Area;
import core.Game;
import core.GameParameters;
import core.GameState;
import pandemic.actions.*;
import utilities.Hash;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;

import static pandemic.Constants.nameHash;

public class PandemicGameState extends GameState {

    public Board world;
    private int numAvailableActions = 0;
    private boolean quietNight;

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
        playerDeck.add(findDeck("Cities"));
        playerDeck.add(findDeck("Events"));

        Deck playerDiscard = new Deck("Player Deck Discard");
        Deck infDiscard = new Deck("Infection Discard");
        Deck plannerDeck = new Deck("plannerDeck"); // deck to store extra card for the contingency planner

        gameArea.addComponent(Constants.playerDeckHash, playerDeck);
        gameArea.addComponent(Constants.playerDeckDiscardHash, playerDiscard);
        gameArea.addComponent(Constants.infectionDiscardHash, infDiscard);
        gameArea.addComponent(Constants.plannerDeckHash, plannerDeck);
        gameArea.addComponent(Constants.infectionHash, findDeck("Infections"));
        gameArea.addComponent(Constants.playerRolesHash, findDeck("Player Roles"));

        // add them to the list of decks, so they are accessible by the findDeck() function
        addDeckToList(playerDeck);
        addDeckToList(playerDiscard);
        addDeckToList(infDiscard);
        addDeckToList(plannerDeck);
    }

    @Override
    public GameState copy() {
        //TODO: copy pandemic game state
        return this;
    }

    @Override
    public int nInputActions() {
        return gp.n_actions_per_turn;  // Pandemic requires up to 4 actions per player per turn.
    }

    public int nPossibleActions() {
        return this.numAvailableActions;
    }

    @Override
    public List<Action> possibleActions() {

        // Create a list for possible actions
        ArrayList<Action> actions = new ArrayList<>();
        PandemicParameters pp = (PandemicParameters) this.gameParameters;

        // get player's hand and role card
        Deck playerHand = ((Deck)this.areas.get(activePlayer).getComponent(Constants.playerHandHash));
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
                actions.add(new DiscardCard(playerHand, i));
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
                if (game.findCounter("Research Stations").getValue() == 0) {
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
                if (game.findCounter("Research Stations").getValue() == 0) {
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
                actions.add(new TreatDisease(gp, Constants.colors[i], playerLocationName.value, treatAll));
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
                if (roleString.equals("Scientist") && colourCounter[i].size() >= gp.n_cards_for_cure_reduced){
                    actions.add(new CureDisease(Constants.colors[i], colourCounter[i]));

                } else if (colourCounter[i].size() >= gp.n_cards_for_cure){
                    actions.add(new CureDisease(Constants.colors[i], colourCounter[i]));
                }
            }
        }

        if (roleString.equals("Dispatcher")){
            //  move any pawn, if its owner agrees, to any city
            //containing another pawn,
            String[] locations = new String[gp.n_players];
            for (int i = 0; i < gp.n_players; i++){
                locations[i] = ((PropertyString) this.areas.get(activePlayer).getComponent(Constants.playerCardHash).getProperty(Constants.playerLocationHash)).value;
            }
            for (String loc: locations){
                for (int i = 0; i < gp.n_players; i++) {
                    // todo there are duplicates here
                    actions.add(new MovePlayer(i, loc));
                }
            }

            //  move another player’s pawn, if its owner agrees,
            //as if it were his own.
            for (int i = 0; i < gp.n_players; i++){
                if (i != activePlayer){
                    actions.addAll(getMoveActions(i, playerHand, researchStations));
                }
            }
        }

        if (roleString.equals("Contingency Planner")){
            if (findDeck("plannerDeck").getCards().size() != 0){
                // then can pick up an event card
                ArrayList<Card> infDiscard = findDeck("InfectionDiscard").getCards();
                for (int i = 0; i < infDiscard.size(); i++){
                    Card card = infDiscard.get(i);
                    if (card.getProperty(Constants.colorHash) != null){
                        actions.add(new DrawCard("InfectionDiscard", "plannerDeck", i));
                    }
                }
            }
            else {
                Deck deck = findDeck("plannerDeck");
                if (deck.getCards().size() > 0) {
                    actions.addAll(actionsFromEventCard(deck.draw(), researchStations));
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

    void setActivePlayer(int activePlayer) {
        this.activePlayer = activePlayer;
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

    private List<Action> actionsFromEventCard(Card card, ArrayList<String> researchStations){
        ArrayList<Action> actions = new ArrayList<>();
        String cardString = ((PropertyString)card.getProperty(nameHash)).value;

        switch (cardString) {
            case "Resilient Population":
                // Remove any 1 card in the Infection Discard Pile from the game. You may play this between the Infect and Intensify steps of an epidemic.
                Deck infectionDiscardDeck = game.findDeck("Infection Discard");
                for (int i = 0; i < infectionDiscardDeck.getCards().size(); i++){
                    actions.add(new DiscardCard(infectionDiscardDeck, i));
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
                        if (game.findCounter("Research Stations").getValue() == 0) {
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

    
    protected void setQuietNight(boolean qn) {
        quietNight = qn;
    }

    public boolean isQuietNight() {
        return quietNight;
    }
}
