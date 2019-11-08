package pandemic;

import components.*;
import content.PropertyString;
import core.Area;
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
    public static int researchStationCounterHash = Hash.GetInstance().hash("researchStationCounter");
    public static List<Integer> diseaseHash;
    public static List<Integer> diseaseCubeHash;
    static List<Integer> tokensHash;

    public static String[] colors = new String[]{"yellow", "red", "blue", "black"};

    PandemicGameState(int nPlayers) {
        areas = new HashMap<>();  // Game State has areas! Initialize.
        diseaseHash = new ArrayList<>();
        tokensHash = new ArrayList<>();
        diseaseCubeHash = new ArrayList<>();

        // load the board
        Board pb = new Board();
        String dataPath = "data/boards.json";
        pb.loadBoards(dataPath);

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

        for (String color : colors) {
            Counter diseaseC = new Counter(0, 2, 0);  // TODO json
            int hash = Hash.GetInstance().hash("diseaseCounter" + color);  // TODO json
            diseaseHash.add(hash);
            gameArea.addComponent(hash, diseaseC);

            Counter diseaseCubeCounter = new Counter(0, 24, 24);  // TODO json
            hash = Hash.GetInstance().hash("diseaseCubeCounter" + color);  // TODO json
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
        List<Token> tokens = new ArrayList<>();  // TODO read from json, assuming list is already made
        for (Token t: tokens) {
            int hash = Hash.GetInstance().hash(t.getTokenType());
            tokensHash.add(hash);
            gameArea.addComponent(hash, t);
        }

        areas.put(-1, gameArea);
    }

    public BoardNode findBoardNode(String city) {
        Board pb = (Board) getAreas().get(-1).getComponent(pandemicBoardHash);
        for (BoardNode bn: pb.getBoardNodes()) {
            PropertyString name = (PropertyString) bn.getProperty(Hash.GetInstance().hash("name"));
            if (name.value.equals(city)) {
                // It's this city!
                return bn;
            }
        }
        return null;
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
