package core.components;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import utilities.Utils.ComponentType;

/**
 * Class for a deck of components.
 * A deck is defined as a "group of components". Examples of decks are:
 *   * A hand of components
 *   * A deck to draw from
 *   * components played on the player's area
 *   * Discomponent pile
 */
public class Deck<T extends Component> extends Component {

    protected int capacity;  // Capacity of the deck (maximum number of elements)
    protected ArrayList<T> components;  // List of components in this deck

    public Deck(String name)
    {
        super(ComponentType.DECK, name);
        this.components = new ArrayList<>();
        this.capacity = -1;
    }
    
    /**
     * Draws the first component of the deck
     * @return the first component of the deck
     */
    public T draw() {
        return pick(0);
    }
    
    /**
     * Picks a random component from the Deck with specific random object.
     * @param rnd - Random object to use to choose component.
     * @return a random component from the Deck
     */
    public T pick(Random rnd) {
        return pick(rnd.nextInt(components.size()));
    }
    
    /**
     * Picks a random component from the Deck with a new random object.
     * @return a random component from the Deck
     */
    public T pick() {
        return pick(new Random().nextInt(components.size()));
    }

    /**
     * Picks the component in position idx from the deck
     * @param idx the index of the component in the deck
     * @return the component in position idx from the deck
     */
    public T pick(int idx) {
        if(components.size() > 0 && idx < components.size() && idx >= 0) {
            T c = components.get(idx);
            components.remove(idx);
            return c;
        }
        return null;
    }

    /**
     * Draws the last component of the deck
     * @return the last component of the deck
     */
    public T pickLast() {
        return pick(components.size()-1);
    }

    /**
     * Peeks (without drawing) the first component of the deck
     * @return The component peeked.
     */
    public T peek() {
        return peek(0);
    }
    
    /**
     * Peeks (without drawing) amount components of the deck starting from component idx
     * @return The components peeked, following the order of the deck. If there are not
     * enough components to be picked, the array will only contain those available. If no
     * components are available, returns an empty array.
     */
    public T[] peek(int idx, int amount) {
        ArrayList<T> components = new ArrayList<>();
        for(int i = idx; i < idx+amount; ++i)
        {
            T c = peek(i);
            if(c != null)
                components.add(c);
        }
        return (T[]) components.toArray();
    }

    /**
     * Peeks (without drawing) the component of the deck at the given index.
     * @param idx - index of component to look at.
     * @return The component peeked.
     */
    public T peek(int idx)
    {
        if(components.size() > 0 && idx < components.size()) {
            return components.get(idx);
        }
        return null;
    }
    
    /**
     * Adds a component to a deck.
     * @param c component to add
     * @return true if within capacity, false otherwise.
     */
    public boolean add(T c) {
        return add(c, 0 );
    }
    
    /**
     * Adds a component to a deck on the given index.
     * @param c component to add
     * @param index where to add it
     * @return true if within capacity, false otherwise.
     */
    public boolean add(T c, int index) {
        if (c==null)
            throw new IllegalArgumentException("null cannot be added to a Deck");
        components.add(index, c);
        return capacity == -1 || components.size() <= capacity;
    }

    /**
     * Adds a full other deck to this deck, ignoring capacity.
     * @param d - other deck to add to this deck.
     * @return true if not over capacity, false otherwise.
     */
    public boolean add(Deck<T> d){
        components.addAll(d.components);
        return capacity == -1 || components.size() <= capacity;
    }

    /**
     * Remove the given component.
     * @param component - component to remove.
     * @return true if successfully removed, false otherwise.
     */
    public boolean remove(T component) {
        int index = components.indexOf(component);
        if (index != -1){
            return remove(index);
        }
        return false;
    }

    /**
     * Remove the component at the given index.
     * @param idx - index of component to remove.
     * @return true if successfully removed, false otherwise.
     */
    public boolean remove(int idx) {
        if (idx >= 0 && idx < components.size()) {
            components.remove(idx);
            return true;
        }
        return false;
    }

    /**
     * Removes all the components from the deck.
     */
    public void clear() {
        components.clear();
    }
    
    /**
     * Shuffles the deck with a specific random object.
     */
    public void shuffle(Random rnd) {
        Collections.shuffle(components, rnd);
    }
    
    /**
     * Shuffles the deck with a new random object.
     */
    public void shuffle() {
        this.shuffle(new Random());
    }

    // Getters, Setters

    /**
     * @return all the components in this deck.
     */
    public ArrayList<T> getComponents() {
        return components;
    }

    /**
     * @return the size of this deck (number of components in it).
     */
    public int getSize() {
        return components.size();
    }
    
    /**
     * Maximum number of components this deck may contain.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Checks if this game is over capacity.
     * @return true if over capacity, false otherwise.
     */
    public boolean isOverCapacity(){
        return capacity != -1 && components.size() > capacity;
    }

    /**
     * Maximum number of components this deck may contain.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Set the components in this deck.
     * @param components - new components for the deck, overrides old content.
     */
    public void setComponents(ArrayList<T> components) {
        this.components = components;
    }

    /**
     * Creates a copy of this deck.
     * @return - a new Deck with the same properties.
     */
    public Deck<T> copy()
    {
        Deck<T> dp = new Deck<>(componentName);
        ArrayList<T> newComponents = new ArrayList<>();
        for (T c : components)
        {
            newComponents.add((T)c.copy());
        }
        dp.components = newComponents;
        dp.capacity = capacity;

        //copy type and component.
        copyComponentTo(dp);
        return dp;
    }

    /**
     * Loads all decks of cards from a given JSON file.
     * @param filename - path to file.
     * @return List of Deck objects.
     */
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
    
    /**
     * Loads cards for a deck from a JSON file.
     * @param deck - deck to load in JSON format
     */
    public static Deck<Card> loadDeckOfCards(JSONObject deck) {
        Deck<Card> newDeck = new Deck<>((String) ( (JSONArray) deck.get("name")).get(1));
        JSONArray deckcards = (JSONArray) deck.get("cards");

        for(Object o : deckcards)
        {
            // Add nodes to board nodes
            JSONObject jsoncard = (JSONObject) o;
            Card newcard = (Card) parseComponent(new Card(), jsoncard);
            newDeck.add(newcard);
        }
        return newDeck;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (T el : getComponents()){
            sb.append(el.toString());
            sb.append(",");
        }

        if (sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        else
            sb.append("EmptyDeck");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Deck<?> deck = (Deck<?>) o;
        return capacity == deck.capacity &&
                Objects.equals(components, deck.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), capacity, components);
    }
}
