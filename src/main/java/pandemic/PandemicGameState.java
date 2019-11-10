package pandemic;

import components.*;
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
    static int infectionCounterHash = Hash.GetInstance().hash("infectionRate");
    static int outbreaksHash = Hash.GetInstance().hash("outbreaks");
    static int playerDeckHash = Hash.GetInstance().hash("playerDeck");
    static int playerDeckDiscardHash = Hash.GetInstance().hash("playerDeckDiscard");
    static int playerCardDeckHash = Hash.GetInstance().hash("playerCardDeck");
    static int infectionDeckHash = Hash.GetInstance().hash("infectionDeck");
    static int infectionDeckDiscardHash = Hash.GetInstance().hash("infectionDeckDiscard");
    public static List<Integer> diseaseHash;
    public static List<Integer> diseaseCubeHash;
    static List<Integer> tokensHash;

    public static String[] colors = new String[]{"yellow", "red", "blue", "black"};

    public Board world;

    public void setupAreas()
    {
        //1. Library of area setups: game.setAreas();
        //2. Like this:

        // For each player, initialize their own areas: they get a player hand and a player card
        for (int i = 0; i < nPlayers; i++) {
            Area playerArea = new Area();
            playerArea.setOwner(i);
            playerArea.addComponent(playerHandHash, new Deck());
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
        diseaseHash = new ArrayList<>();
        tokensHash = new ArrayList<>();
        diseaseCubeHash = new ArrayList<>();

        // load the board
        world = game.findBoard("cities"); //world.getNode("name","Valencia");

        setupAreas(); // TODO: This should be called from GameState.java

        Area gameArea = areas.get(-1);

        // Set up the counters
        Counter infection_rate = game.findCounter("Infection Rate");
        Counter outbreaks = game.findCounter("Outbreaks");
        gameArea.addComponent(infectionCounterHash, infection_rate);
        gameArea.addComponent(outbreaksHash, outbreaks);

        for (String color : colors) {
            int hash = Hash.GetInstance().hash("Disease " + color);
            Counter diseaseC = game.findCounter("Disease " + color);
            diseaseHash.add(hash);
            gameArea.addComponent(hash, diseaseC);

            hash = Hash.GetInstance().hash("Disease Cube " + color);
            Counter diseaseCubeCounter = game.findCounter("Disease Cube " + color);
            diseaseCubeHash.add(hash);
            gameArea.addComponent(hash, diseaseCubeCounter);
        }

        // Set up decks
        gameArea.addComponent(playerDeckHash, new Deck());
        gameArea.addComponent(playerDeckDiscardHash, new Deck());
        gameArea.addComponent(infectionDeckHash, new Deck());
        gameArea.addComponent(infectionDeckDiscardHash, new Deck());
        gameArea.addComponent(playerCardDeckHash, new Deck());

        // Set up tokens
        Token research_stations = game.findToken("Research Stations");
        int hash = Hash.GetInstance().hash("Research Stations");
        tokensHash.add(hash);
        gameArea.addComponent(hash, research_stations);

        //TODO: add pawn tokens

    }

    @Override
    public GameState copy() {
        //TODO: copy pandemic game state
        return this;
    }

    void setActivePlayer(int activePlayer) {
        this.activePlayer = activePlayer;
    }
}
