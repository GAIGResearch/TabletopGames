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

    protected ArrayList<T> elements;
    protected ArrayList<boolean[]> elementVisibility;
    protected boolean[] deckVisibility;

    private String id;

    public Deck(String id)
    {
        this.id = id;
        super.type = ComponentType.DECK;
        elements = new ArrayList<>();
        properties = new HashMap<>();
        elementVisibility = new ArrayList<>();
        deckVisibility = new boolean[0];
    }

    public void setElements(ArrayList<T> elements) {
        this.elements = elements;
        setElementVisibility(deckVisibility);
    }
    public void setElementsVisibility(ArrayList<boolean[]> cardVisibility) {
        this.elementVisibility = cardVisibility;
    }
    public void setElementVisibility(boolean[] visibility) {
        this.elementVisibility = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            if (visibility != null) elementVisibility.add(visibility.clone());
            else elementVisibility.add(null);
        }
    }
    public void setElementVisibility(int cardIdx, int player, boolean visibility) {
        this.elementVisibility.get(cardIdx)[player] = visibility;
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
        return capacity != -1 && elements.size() > capacity;
    }

    public void shuffle(Random rnd) {
        // TODO: this messes up visibility
        Collections.shuffle(elements, rnd);
    }

    public void shuffle() {
        // TODO: this messes up visibility
        Collections.shuffle(elements, new Random());
    }

    public T draw() {
        return pick(0);
    }

    public T pick() {
        return pick(0);
    }

    public T pick(int idx) {
        if(elements.size() > 0 && idx < elements.size()) {
            T c = elements.get(idx);
            elementVisibility.remove(idx);
            elements.remove(idx);
            return c;
        }
        return null;
    }

    public T pickLast() {
        return pick(elements.size()-1);
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
        if(elements.size() > 0 && idx < elements.size()) {
            return elements.get(idx);
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
        elements.add(index, c);
        elementVisibility.add(index, visibilityPerPlayer);
        return capacity == -1 || elements.size() <= capacity;
    }

    public boolean add(Deck<T> d){
        elements.addAll(d.elements);
        elementVisibility.addAll(d.elementVisibility);
        return true;
    }


    @Override
    public Deck<T> copy()
    {
        Deck<T> dp = new Deck<>(id);
        this.copyTo(dp);
        return dp;
    }

    public void copyTo(IDeck<T> target)
    {
        Deck<T> dp = (Deck<T>) target;
        ArrayList<T> newCards = new ArrayList<>();
        for (T c : dp.elements)
        {
            try {
                newCards.add((T) c.getClass().getMethod("clone").invoke(c));
            } catch (Exception e) {
                throw new RuntimeException("Objects in deck target do not implement the method 'clone'", e);
            }
        }
        dp.setElements(newCards);
        dp.capacity = capacity;

        ArrayList<boolean[]> visibility = new ArrayList<>(dp.elementVisibility);
        dp.setElementsVisibility(visibility);

        //copy type and component.
        copyComponentTo(dp);
    }

    public ArrayList<T> getElements() {
        return elements;
    }

    public ArrayList<T> getCards(int playerID) {
        ArrayList<T> visibleCards = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            boolean[] b = elementVisibility.get(i);
            if (b[playerID]) visibleCards.add(i, elements.get(i));
            else visibleCards.add(i, null);
        }
        return visibleCards;
    }

    public String getID() {
        return id;
    }

    public void clear() {
        elements.clear();
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

    public boolean remove(T card) {
        if (elements.contains(card)) {
            elementVisibility.remove(elements.indexOf(card));
            elements.remove(card);
            return true;
        }
        return false;
    }

    public boolean remove(int idx) {
        if (idx >= 0 && idx < elements.size()) {
            elements.remove(idx);
            elementVisibility.remove(idx);
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Deck<?> deck = (Deck<?>) o;
        return capacity == deck.capacity &&
                Objects.equals(elements, deck.elements) &&
                Objects.equals(elementVisibility, deck.elementVisibility) &&
                Arrays.equals(deckVisibility, deck.deckVisibility) &&
                Objects.equals(id, deck.id);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(capacity, elements, elementVisibility, id);
        result = 31 * result + Arrays.hashCode(deckVisibility);
        return result;
    }
}
