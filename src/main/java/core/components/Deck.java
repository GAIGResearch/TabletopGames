package core.components;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import core.content.PropertyString;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Hash;
import utilities.Utils.ComponentType;

public class Deck<T> extends Component implements IDeck<T> {

    protected int capacity = -1;
    protected ArrayList<T> cards;
    protected String id;

    public Deck(String id)
    {
        this.id = id;
        super.type = ComponentType.DECK;
        cards = new ArrayList<>();
        properties = new HashMap<>();
    }

    public ArrayList<T> getCards() {
        return cards;
    }

    public void setCards(ArrayList<T> cards) {
        this.cards = cards;
    }

    public int getSize() {
        return cards.size();
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isOverCapacity(){
        return capacity != -1 && cards.size() > capacity;
    }

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public T draw() {
        return pick(0);
    }

    public T pick() {
        return pick(0);
    }

    public T pick(int idx) {
        if(cards.size() > 0 && idx < cards.size() && idx >= 0) {
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

    public T peek(int idx)
    {
        if(cards.size() > 0 && idx < cards.size()) {
            return cards.get(idx);
        }
        return null;
    }

    public boolean add(T c) {
        return add(c, 0 );
    }

    public boolean add(T c, int index) {
        if (c==null)
            throw new IllegalArgumentException("null cannot be added to a Deck");
        cards.add(index, c);
        return capacity == -1 || cards.size() <= capacity;
    }

    public boolean add(Deck<T> d){
        cards.addAll(d.cards);
        return capacity == -1 || cards.size() <= capacity;
    }

    public boolean remove(T card) {
        if (cards.contains(card)) {
            cards.remove(card);
            return true;
        }
        return false;
    }

    public boolean remove(int idx) {
        if (idx >= 0 && idx < cards.size()) {
            cards.remove(idx);
            return true;
        }
        return false;
    }

    public void clear() {
        cards.clear();
    }

    public void shuffle(Random rnd) {
        Collections.shuffle(cards, rnd);
    }

    public void shuffle() {
        this.shuffle(new Random());
    }

    @Override
    public Deck<T> copy()
    {
        Deck<T> dp = new Deck<>(id);
        this.copyTo(dp);
        return dp;
    }

    public void copyTo(Deck<T> target)
    {
        ArrayList<T> newCards = new ArrayList<>();
        for (T c : cards)
        {
            try {
                newCards.add((T) c.getClass().getMethod("clone").invoke(c));
            } catch (Exception e) {
                throw new RuntimeException("Objects in deck target do not implement the method 'clone'", e);
            }
        }
        target.cards = newCards;
        target.capacity = capacity;

        //copy type and component.
        copyComponentTo(target);
    }

    /**
     * Loads cards for a deck from a JSON file.
     * @param deck - deck to load in JSON format
     */
    public static Deck<Card> loadDeckOfCards(JSONObject deck) {
        Deck<Card> newDeck = new Deck<>((String) ( (JSONArray) deck.get("name")).get(1));
        newDeck.properties.put(Hash.GetInstance().hash("name"), new PropertyString(newDeck.id));

        JSONArray deckCards = (JSONArray) deck.get("cards");

        for(Object o : deckCards)
        {
            // Add nodes to board nodes
            JSONObject jsonCard = (JSONObject) o;
            Card newCard = (Card) parseComponent(new Card(), jsonCard);
            newDeck.add(newCard);
        }
        return newDeck;
    }

    public static List<Deck<Card>> loadDecksOfCards(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Deck<Card>> decks = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {
                Deck<Card> newDeck = loadDeckOfCards((JSONObject) o);
                decks.add(newDeck);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return decks;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (T el : getCards()){
            sb.append(el.toString());
            sb.append(",");
        }

        if (sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        else
            sb.append("EmptyDeck");

        return sb.toString();
    }
}
