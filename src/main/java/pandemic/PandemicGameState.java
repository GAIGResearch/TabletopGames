package pandemic;

import components.*;
import core.Area;
import core.GameState;
import utilities.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PandemicGameState extends GameState {
    static int playerHandHash = Hash.GetInstance().hash("playerHand");
    static int playerCardHash = Hash.GetInstance().hash("playerCard");
    static int pandemicBoardHash = Hash.GetInstance().hash("pandemicBoard");
    static int infectionCounterHash = Hash.GetInstance().hash("infectionRate");
    static int outbreaksHash = Hash.GetInstance().hash("outbreaks");
    static int playerDeckHash = Hash.GetInstance().hash("playerDeck");
    static int playerDeckDiscardHash = Hash.GetInstance().hash("playerDeckDiscard");
    static int infectionDeckHash = Hash.GetInstance().hash("infectionDeck");
    static int infectionDeckDiscardHash = Hash.GetInstance().hash("infectionDeckDiscard");
    static int researchStationCounterHash = Hash.GetInstance().hash("researchStationCounter");
    static List<Integer> diseaseHash;
    static List<Integer> tokensHash;

    PandemicGameState(int nPlayers) {
        areas = new HashMap<>();  // Game State has areas! Initialize.
        diseaseHash = new ArrayList<>();
        tokensHash = new ArrayList<>();

        // load the board
        Board pb = new Board();
        String dataPath = "data/pandemicBoard.json";
        pb.loadBoard(dataPath);

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
        gameArea.addComponent(pandemicBoardHash, pb);

        // Set up the counters TODO: read from JSON
        Counter infection_rate = new Counter(0, 6, 0);
        Counter outbreaks = new Counter(0, 8, 0);
        gameArea.addComponent(infectionCounterHash, infection_rate);
        gameArea.addComponent(outbreaksHash, outbreaks);
        gameArea.addComponent(researchStationCounterHash, new Counter(0, 7, 7));

        int nDisease = 4;  // TODO json
        for (int i = 0; i < nDisease; i++) {
            Counter diseaseC = new Counter(0, 2, 0);  // TODO json
            int hash = Hash.GetInstance().hash("jsonDiseaseName");  // TODO json
            diseaseHash.add(hash);
            gameArea.addComponent(hash, diseaseC);
        }

        // Set up decks
        gameArea.addComponent(playerDeckHash, new Deck());
        gameArea.addComponent(playerDeckDiscardHash, new Deck());
        gameArea.addComponent(infectionDeckHash, new Deck());
        gameArea.addComponent(infectionDeckDiscardHash, new Deck());

        // Set up tokens
        List<Token> tokens = new ArrayList<>();  // TODO read from json, assuming list is already made
        for (Token t: tokens) {
            int hash = Hash.GetInstance().hash(t.getTokenType());
            tokensHash.add(hash);
            gameArea.addComponent(hash, t);
        }

        areas.put(-1, gameArea);
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
