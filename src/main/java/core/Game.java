package core;

import components.Board;
import components.Card;
import components.Component;
import components.Deck;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class Game {
    protected List<AIPlayer> players;
    protected GameState gameState;
    protected ForwardModel forwardModel;

    public void setPlayers(List<AIPlayer> players) {
        this.players = players;
    }

    public List<AIPlayer> getPlayers() {
        return players;
    }

    public abstract void run();
    public abstract boolean isEnded();
    public abstract HashSet<Integer> winners();

    public static List<Component> loadBoards(String filename)
    {
        return Board.loadBoards(filename);
    }

    public static List<Component> loadDecks(String filename)
    {
        return Deck.loadDecks(filename);
    }

    public static void main(String[] args)
    {
        List<Component> boards = Game.loadBoards("data/boards.json");
        List<Component> decks = Game.loadDecks("data/decks.json");
        System.out.println("Done.");
    }

}
