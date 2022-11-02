package core.components;

import core.interfaces.IComponentContainer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Utils.ComponentType;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static core.CoreConstants.VisibilityMode;

/**
 * Class for a deck of components.
 * A deck is defined as a "group of components". Examples of decks are:
 * * A hand of components
 * * A deck to draw from
 * * components played on the player's area
 * * Discomponent pile
 */
public class Deck<T extends Component> extends Component implements IComponentContainer<T> {

    protected int capacity;  // Capacity of the deck (maximum number of elements)
    protected List<T> components;  // List of components in this deck
    protected VisibilityMode visibility;

    public Deck(String name, VisibilityMode visibility) {
        this(name, -1, visibility);
    }

    public Deck(String name, int ownerId, VisibilityMode visibility) {
        super(ComponentType.DECK, name);
        this.components = new ArrayList<>();
        this.ownerId = ownerId;
        this.capacity = -1;
        this.visibility = visibility;
    }

    protected Deck(String name, int ownerId, int ID, VisibilityMode visibility) {
        super(ComponentType.DECK, name, ID);
        this.components = new ArrayList<>();
        this.capacity = -1;
        this.ownerId = ownerId;
        this.visibility = visibility;
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
     * enough components to be picked, the List will only contain those available. If no
     * components are available, returns an empty List.
     */
    public List<T> peek(int idx, int amount) {
        ArrayList<T> components = new ArrayList<>();
        for (int i = idx; i < idx + amount; ++i) {
            T c = peek(i);
            if (c != null)
                components.add(c);
        }
        return components;
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
        if (c != null) {
            c.setOwnerId(ownerId);
        }
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
        c.setOwnerId(ownerId);
        components.add(index, c);
        return capacity == -1 || components.size() <= capacity;
    }

    /**
     * Adds a full other deck to this deck, ignoring capacity.
     * @param d - other deck to add to this deck.
     * @return true if not over capacity, false otherwise.
     */
    public boolean add(Deck<T> d){
        return this.add(d, 0);
    }

    /**
     * Adds a full other deck to this deck, ignoring capacity.
     * @param d - other deck to add to this deck.
     * @param index - the position in which the elements of d should be inserted in this deck.
     * @return true if not over capacity, false otherwise.
     */
    public boolean add(Deck<T> d, int index){
        components.addAll(index, d.components);
        for (T comp: d.components) {
            comp.setOwnerId(ownerId);
        }
        return capacity == -1 || components.size() <= capacity;
    }

    public boolean add(Collection<T> d){
        return this.add(d, 0);
    }

    public boolean add(Collection<T> d, int index){
        components.addAll(index, d);
        for (T comp: d) {
            comp.setOwnerId(ownerId);
        }
        return capacity == -1 || components.size() <= capacity;
    }


    /**
     * Remove the given component.
     * @param component - component to remove.
     * @return true if successfully removed, false otherwise.
     */
    public boolean remove(T component) {
        component.setOwnerId(-1);
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
            components.get(idx).setOwnerId(-1);
            components.remove(idx);
            return true;
        }
        return false;
    }

    /**
     * Removes all the components from the deck.
     */
    public void clear() {
        for (T comp: components) {
            comp.setOwnerId(-1);
        }
        components.clear();
    }
    
    /**
     * Shuffles the deck with a specific random object.
     */
    public void shuffle(Random rnd) {
        Collections.shuffle(components, rnd);
    }

    /**
     * Shuffles part of the deck, given by range [fromIndex, toIndex), leaving the rest the same.
     * @param fromIndex - index from where to start shuffling, inclusive
     * @param toIndex - index where to stop shuffling, exclusive
     * @param rnd - random number generator used for shuffling
     */
    public void shuffle(int fromIndex, int toIndex, Random rnd) {
        List<T> subList = components.subList(fromIndex, toIndex);
        Collections.shuffle(subList, rnd);
        int i = 0;
        for (T component: subList) {
            components.set(fromIndex + i, component);
            i++;
        }
    }

    // Getters, Setters

    /**
     * @return all the components in this deck.
     */
    @Override
    public List<T> getComponents() {
        return components;
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
        for (T comp: components) {
            comp.setOwnerId(ownerId);
        }
    }

    /**
     * Sets the index to the given component.
     * @param idx - index of component to replace.
     * @param component - new component.
     */
    public void setComponent(int idx, T component) {
        component.setOwnerId(ownerId);
        components.set(idx, component);
    }

    /**
     * Shortcut for retrieving a specific component.
     * @param idx - index of component queried
     * @return - component at given index.
     */
    public T get(int idx) {
        return components.get(idx);
    }

    @Override
    public VisibilityMode getVisibilityMode() {
        return visibility;
    }

    public void setVisibility(VisibilityMode mode) {
        visibility = mode;
    }

    /**
     * Creates a copy of this deck.
     *
     * @return - a new Deck with the same properties.
     */
    public Deck<T> copy() {
        Deck<T> dp = new Deck<>(componentName, ownerId, componentID, visibility);
        copyTo(dp);
        return dp;
    }

    protected void copyTo(Deck<T> deck) {
        List<T> newComponents = new ArrayList<>();
        for (T c : components)
        {
            newComponents.add((T)c.copy());
        }
        deck.components = newComponents;
        deck.capacity = capacity;

        //copy type and component.
        copyComponentTo(deck);
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
        Deck<Card> newDeck = new Deck<>((String) ((JSONArray) deck.get("name")).get(1), VisibilityMode.HIDDEN_TO_ALL);
        newDeck.setVisibility(VisibilityMode.valueOf((String) deck.get("visibility")));
        JSONArray deckcards = (JSONArray) deck.get("cards");

        for (Object o : deckcards) {
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
        for (T el : components){
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
        if (!(o instanceof Deck)) return false;
        if (!super.equals(o)) return false;
        Deck<?> deck = (Deck<?>) o;
        return capacity == deck.capacity &&
                Objects.equals(components, deck.components);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(capacity, ownerId, componentID, components);
    }

}
