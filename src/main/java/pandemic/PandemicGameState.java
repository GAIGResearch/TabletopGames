package pandemic;

import actions.Action;
import actions.DoNothing;
import actions.MovePlayer;
import components.*;
import content.Property;
import content.PropertyString;
import core.Area;
import core.Game;
import core.GameState;
import utilities.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PandemicGameState extends GameState {

    //TODO: remove 'static' from these?
    static int playerHandHash = Hash.GetInstance().hash("playerHand");
    public static int playerCardHash = Hash.GetInstance().hash("playerCard");
    public static int pandemicBoardHash = Hash.GetInstance().hash("pandemicBoard");

    public static String[] colors = new String[]{"yellow", "red", "blue", "black"};

    public Board world;
    public int numAvailableActions = 0;

    public void setupAreas()
    {
        //1. Library of area setups: game.setAreas();
        //2. Like this:

        // For each player, initialize their own areas: they get a player hand and a player card
        for (int i = 0; i < nPlayers; i++) {
            Area playerArea = new Area();
            playerArea.setOwner(i);
            playerArea.addComponent(playerHandHash, new Deck(7));
            playerArea.addComponent(playerCardHash, new Card());  // TODO: properties?
            areas.put(i, playerArea);
        }

        // Initialize the game area: board, player deck, player discard deck, infection deck, infection discard
        // infection rate counter, outbreak counter, diseases x 4
        Area gameArea = new Area();
        gameArea.setOwner(-1);
        gameArea.addComponent(pandemicBoardHash, world);
        areas.put(-1, gameArea);
    }


    public void setup(Game game)
    {
        // load the board
        world = game.findBoard("cities"); //world.getNode("name","Valencia");

        setupAreas(); // TODO: This should be called from GameState.java

        Area gameArea = areas.get(-1);

        // Set up the counters
        Counter infection_rate = game.findCounter("Infection Rate");
        Counter outbreaks = game.findCounter("Outbreaks");
        gameArea.addComponent(Hash.GetInstance().hash("Infection Rate"), infection_rate);
        gameArea.addComponent(Hash.GetInstance().hash("Outbreaks"), outbreaks);

        for (String color : colors) {
            int hash = Hash.GetInstance().hash("Disease " + color);
            Counter diseaseC = game.findCounter("Disease " + color);
            gameArea.addComponent(hash, diseaseC);

            hash = Hash.GetInstance().hash("Disease Cube " + color);
            Counter diseaseCubeCounter = game.findCounter("Disease Cube " + color);
            gameArea.addComponent(hash, diseaseCubeCounter);
        }

        // Set up decks
        gameArea.addComponent(Hash.GetInstance().hash("Player Deck"), new Deck());  // TODO: combine cities+events
        gameArea.addComponent(Hash.GetInstance().hash("Player Deck Discard"), new Deck());
        gameArea.addComponent(Hash.GetInstance().hash("Infection Discard"), new Deck());
        gameArea.addComponent(Hash.GetInstance().hash("Infections"), game.findDeck("Infections"));
        gameArea.addComponent(Hash.GetInstance().hash("Player Roles"), game.findDeck("Player Roles"));

        // Set up tokens
        Token research_stations = game.findToken("Research Stations");
        int hash = Hash.GetInstance().hash("Research Stations");
        gameArea.addComponent(hash, research_stations);

        //TODO: add pawn tokens

    }

    @Override
    public GameState copy() {
        //TODO: copy pandemic game state
        return this;
    }

    @Override
    public int nInputActions() {
        return 4;  // Pandemic requires up to 4 actions per player per turn.
    }

    @Override
    public int nPossibleActions() {
        return this.numAvailableActions;
    }

    @Override
    public List<Action> possibleActions() {
        // Create a list for possible actions
        ArrayList<Action> actions = new ArrayList<>();

        // add do nothing action
        actions.add(new DoNothing());
        this.numAvailableActions = actions.size();

        // Drive / Ferry add actions for travelling immediate cities
        Property playerLocation = this.areas.get(activePlayer).getComponent(playerCardHash).getProperty(Hash.GetInstance().hash("playerLocation"));
        BoardNode cityNode = world.getNodeByProperty(Hash.GetInstance().hash("name"), playerLocation);
        for (BoardNode otherCity : cityNode.getNeighbours()){
            actions.add(new MovePlayer(activePlayer, ((PropertyString)otherCity.getProperty(Hash.GetInstance().hash("name"))).value));
        }

        // Direct Flight, discard city card and travel to that city
        // get player's cards and create actions that take from current location to those location
        Deck playerDeck = ((Deck)this.areas.get(activePlayer).getComponent(playerHandHash));
        for (Card card: playerDeck.getCards()){
            // get the city from the card

            // next line is the full way to get the first card in the player's hand
//            ((Deck)this.areas.get(activePlayer).getComponent(playerHandHash)).getCards().get(0).getProperty(Hash.GetInstance().hash("name"));
            actions.add(new MovePlayer(activePlayer, ((PropertyString)card.getProperty(Hash.GetInstance().hash("name"))).value));
        }

        // todo charter flight, discard city that matches your card and travel to any city
        for (Card card: playerDeck.getCards()){
            // get the city from the card
            if (playerLocation.equals(((PropertyString)card.getProperty(Hash.GetInstance().hash("name"))).value)){
                // add all the cities
                System.out.println("need to add all the cities");
            }
//            actions.add(new MovePlayer(activePlayer, ((PropertyString)card.getProperty(Hash.GetInstance().hash("name"))).value));
        }


        // todo shuttle flight, move from city with research station to any other research station

        // todo build research station, discard card with that city to build one, if 6 are used, then take one from board

        // todo treat disease

        // todo share knowledge, give or take card, player can only have 7 cards

        // todo discover a cure, 5 cards of the same colour at a research station





        return actions;  // TODO
    }

    void setActivePlayer(int activePlayer) {
        this.activePlayer = activePlayer;
    }
}
