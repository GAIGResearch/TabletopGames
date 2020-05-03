package components;

import java.io.Console;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import content.PropertyString;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Hash;
import utilities.Utils.ComponentType;

// TODO: update IDeck interface from methods in this class
public class Deck<T> extends Component implements IDeck<T> {

    protected int capacity = -1;

    protected ArrayList<T> cards;

    private String id;

    public Deck()
    {
        super.type = ComponentType.DECK;
        cards = new ArrayList<>();
        properties = new HashMap<>();
    }

    public Deck(String name)
    {
        super.type = ComponentType.DECK;
        this.id = name;
        cards = new ArrayList<>();
        properties = new HashMap<>();
    }

    protected Deck(Random rnd, int capacity)
    {
        super.type = ComponentType.DECK;
        cards = new ArrayList<>();
        this.capacity = capacity;
        properties = new HashMap<>();
    }

    public Deck(int capacity)
    {
        super.type = ComponentType.DECK;
        cards = new ArrayList<>();
        this.capacity = capacity;
        properties = new HashMap<>();
    }

    protected void setCards(ArrayList<T> cards) {this.cards = cards;}

    @Override
    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isOverCapacity(){
        return capacity != -1 && cards.size() > capacity;
    }

    public void shuffle(Random rnd) {
        Collections.shuffle(cards, rnd);
    }

    public void shuffle() {
        Collections.shuffle(cards, new Random());
    }

    public T draw() {
        return pick(0);
    }

    public T pick() {
        return pick(0);
    }

    public T pick(int idx) {
        if(cards.size() > 0 && idx < cards.size()) {
            T c = cards.get(idx);
            cards.remove(idx);
            return c;
        }
        return null;
    }

    public T pickLast() {
        return pick(cards.size()-1);
    }

    @Override
    public T peek() {
        return peek(0);
    }

    @Override
    public T[] peek(int idx, int amount) {
        ArrayList<T> cards = new ArrayList<>();
        for(int i = idx; i < idx+amount; ++i)
        {
            T c = peek(i);
            if(c != null)
                cards.add(c);
        }
        return (T[]) cards.toArray();
    }

    private T peek(int idx)
    {
        if(cards.size() > 0 && idx < cards.size()) {
            return cards.get(idx);
        }
        return null;
    }

    public boolean remove(T el)
    {
        return cards.remove(el);
    }


    public boolean add(T c) {
        return add(c, 0);
    }

    public boolean add(T c, int index) {
        cards.add(index, c);
        return capacity == -1 || cards.size() <= capacity;
    }

    public boolean add(Deck<T> d){
        cards.addAll(d.cards);
        return true;
    }


    @Override
    public Deck<T> copy()
    {
        Deck<T> dp = new Deck<>();
        this.copyTo(dp);
        return dp;
    }

    public void copyTo(IDeck<T> target)
    {
        Deck<T> dp = (Deck<T>) target;
        ArrayList<T> newCards = new ArrayList<>();
        for (T c : dp.cards)
        {
            try {
                newCards.add((T) c.getClass().getMethod("clone").invoke(c));
            } catch (Exception e) {
                throw new RuntimeException("Objects in deck target do not implement the method 'clone'", e);
            }
        }
        dp.setCards(newCards);
        dp.capacity = capacity;

        //copy type and component.
        copyComponentTo(dp);
    }

    // TODO: check for visibility?
    public ArrayList<T> getCards() {
        return cards;
    }

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public void clear() {
        cards.clear();
    }

    /**
     * Loads cards for a deck from a JSON file.
     * @param deck - deck to load in JSON format
     */
    public static Deck<Card> loadDeck(JSONObject deck) {
        Deck<Card> newDeck = new Deck<>();
        newDeck.id = (String) ( (JSONArray) deck.get("name")).get(1);
        newDeck.properties.put(Hash.GetInstance().hash("name"), new PropertyString(newDeck.id));

        JSONArray deckCards = (JSONArray) deck.get("cards");

        for(Object o : deckCards)
        {
            // Add nodes to board nodes
            JSONObject jsonCard = (JSONObject) o;
            Card newCard = (Card) parseComponent(new Card(), jsonCard);
            newDeck.cards.add(newCard);
        }
        return newDeck;
    }

    public static List<Deck<Card>> loadDecks(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Deck<Card>> decks = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {
                Deck<Card> newDeck = loadDeck((JSONObject) o);
                decks.add(newDeck);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return decks;
    }

    public void discard(T card) {
        cards.remove(card);
    }
}
