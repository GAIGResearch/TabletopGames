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
    protected ArrayList<boolean[]> cardVisibility;
    protected boolean[] deckVisibility;

    private String id;

    public Deck()
    {
        this.id = "";
        super.type = ComponentType.DECK;
        cards = new ArrayList<>();
        properties = new HashMap<>();
        cardVisibility = new ArrayList<>();
        deckVisibility = new boolean[0];
    }

    public void setCards(ArrayList<T> cards) {
        this.cards = cards;
        setCardVisibility(deckVisibility);
    }
    public void setCardsVisibility(ArrayList<boolean[]> cardVisibility) {
        this.cardVisibility = cardVisibility;
    }
    public void setCardVisibility(boolean[] visibility) {
        this.cardVisibility = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            if (visibility != null) cardVisibility.add(visibility.clone());
            else cardVisibility.add(null);
        }
    }
    public void setCardVisibility(int cardIdx, int player, boolean visibility) {
        this.cardVisibility.get(cardIdx)[player] = visibility;
    }
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    public void setID(String id) {
        this.id = id;
    }
    public void setDeckVisibility(int nPlayers, boolean v) {
        deckVisibility = new boolean[nPlayers];
        Arrays.fill(deckVisibility, v);
    }
    public void setDeckVisibility(boolean[] visibility) {
        if (visibility != null) this.deckVisibility = visibility.clone();
        else this.deckVisibility = null;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    public boolean isOverCapacity(){
        return capacity != -1 && cards.size() > capacity;
    }

    public void shuffle(Random rnd) {
        // TODO: this messes up visibility
        Collections.shuffle(cards, rnd);
    }

    public void shuffle() {
        // TODO: this messes up visibility
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
            cardVisibility.remove(idx);
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
        return add(c, 0, null);
    }
    public boolean add(T c, boolean[] visibilityPerPlayer) {
        return add(c, 0, visibilityPerPlayer);
    }
    public boolean add(T c, int index) {
        return add(c, index, null);
    }
    public boolean add(T c, int index, boolean[] visibilityPerPlayer) {
        cards.add(index, c);
        cardVisibility.add(index, visibilityPerPlayer);
        return capacity == -1 || cards.size() <= capacity;
    }

    public boolean add(Deck<T> d){
        cards.addAll(d.cards);
        cardVisibility.addAll(d.cardVisibility);
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

        ArrayList<boolean[]> visibility = new ArrayList<>(dp.cardVisibility);
        dp.setCardsVisibility(visibility);

        //copy type and component.
        copyComponentTo(dp);
    }

    public ArrayList<T> getCards() {
        return cards;
    }

    public ArrayList<T> getCards(int playerID) {
        ArrayList<T> visibleCards = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            boolean[] b = cardVisibility.get(i);
            if (b[playerID]) visibleCards.add(i, cards.get(i));
            else visibleCards.add(i, null);
        }
        return visibleCards;
    }

    public String getID() {
        return id;
    }

    public void clear() {
        cards.clear();
    }

    /**
     * Loads cards for a deck from a JSON file.
     * @param deck - deck to load in JSON format
     */
    public static Deck<Card> loadDeckOfCards(JSONObject deck) {
        Deck<Card> newDeck = new Deck<>();
        newDeck.id = (String) ( (JSONArray) deck.get("name")).get(1);
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

    public boolean remove(T card) {
        if (cards.contains(card)) {
            cardVisibility.remove(cards.indexOf(card));
            cards.remove(card);
            return true;
        }
        return false;
    }

    public boolean remove(int idx) {
        if (idx >= 0 && idx < cards.size()) {
            cards.remove(idx);
            cardVisibility.remove(idx);
            return true;
        }
        return false;
    }

    public int getSize() {
        return cards.size();
    }
}
