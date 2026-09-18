package core.components;

import core.CoreConstants;
import core.interfaces.IComponentContainer;
import core.interfaces.IToJSON;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.JSONUtils;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static core.CoreConstants.VisibilityMode;

/**
 * Class for a deck of components.
 * A deck is defined as a "group of components". Examples of decks are:
 * * A hand of components
 * * A deck to draw from
 * * Components played on the player's area
 * * Discard pile
 * <p>
 * Index 0 is the <b>top</b> of the deck: {@link #draw()}, {@link #peek()} and {@link #add(Component)} all work on
 * index 0, and {@link #addToBottom(Component)} / {@link #pickLast()} on the last index. So when cards are added one
 * by one with add(), the first card added ends up at the <b>last</b> index, and get(0) is the most recent - read a
 * first-dealt card (an up card, a starter) with get(getSize() - 1), not peek() or get(0).
 */
public class Deck<T extends Component> extends Component implements IComponentContainer<T>, Iterable<T>, IToJSON {

    protected int capacity;  // Capacity of the deck (maximum number of elements)
    protected List<T> components;  // List of components in this deck
    protected VisibilityMode visibility;

    public Deck(String name, VisibilityMode visibility) {
        this(name, -1, visibility);
    }

    public Deck(String name, int ownerId, VisibilityMode visibility) {
        super(CoreConstants.ComponentType.DECK, name);
        this.components = new LinkedList<>();   // we always add new components to element 0...so an ArrayList is inefficient
        this.ownerId = ownerId;
        this.capacity = -1;
        this.visibility = visibility;
    }

    protected Deck(String name, int ownerId, int ID, VisibilityMode visibility) {
        super(CoreConstants.ComponentType.DECK, name, ID);
        this.components = new LinkedList<>();
        this.capacity = -1;
        this.ownerId = ownerId;
        this.visibility = visibility;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new DeckIterator();
    }


    // Iterator implementation
    private class DeckIterator implements Iterator<T> {
        private int currentIndex = 0;

        @Override
        public boolean hasNext() {
            return currentIndex < getSize();
        }

        @Override
        public T next() {
            return components.get(currentIndex++);
        }
    }


    /**
     * Loads all decks of cards from a given JSON file.
     *
     * @param filename - path to file.
     * @return List of Deck objects.
     */
    public static List<Deck<Card>> loadDecksOfCards(String filename) {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Deck<Card>> decks = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for (Object o : data) {
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
     *
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

    /**
     * Serializes the runtime state of this deck to JSON, so that it can be reconstructed via
     * {@link #loadDeck}. Every component in the deck must itself implement {@link IToJSON} <em>and</em>
     * expose a constructor taking a {@link JSONObject} (so it can be re-created on load) - this is
     * the implicit requirement of putting a component into a serializable deck. Each component's JSON
     * is tagged with its concrete class so it can be reconstructed reflectively. The deck's identity
     * (componentID), name, ownerId, capacity, visibility mode and the order of its components are
     * all preserved, so that the reloaded deck is {@code equals()} to the original.
     *
     * @throws IllegalStateException if any component in the deck does not implement {@link IToJSON}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", componentName);
        json.put("id", componentID);
        json.put("ownerId", ownerId);
        json.put("capacity", capacity);
        json.put("visibility", visibility.name());
        JSONArray cards = new JSONArray();
        for (T component : components) {
            if (component instanceof IToJSON serializable) {
                JSONObject cardJSON = serializable.toJSON();
                // tag with the concrete class so loadDeck can reconstruct it via its JSONObject constructor
                cardJSON.putIfAbsent("class", component.getClass().getName());
                cards.add(cardJSON);
            } else
                throw new IllegalStateException("Cannot serialize deck '" + componentName
                        + "': component does not implement IToJSON : " + component.getClass().getName());
        }
        json.put("cards", cards);
        return json;
    }

    /**
     * Reconstructs a Deck from JSON produced by {@link #toJSON}. Each component is recreated by
     * recursing into its JSON via {@link JSONUtils#loadClassFromJSON}, which finds the component's
     * {@link JSONObject} constructor - so every component type in a serializable deck must provide one.
     *
     * @param json - the JSON produced by {@link #toJSON}.
     */
    public static <T extends Component> Deck<T> loadDeck(JSONObject json) {
        Deck<T> deck = new Deck<>((String) json.get("name"),
                ((Number) json.get("ownerId")).intValue(),
                ((Number) json.get("id")).intValue(),
                VisibilityMode.valueOf((String) json.get("visibility")));
        deck.capacity = ((Number) json.get("capacity")).intValue();
        populateComponents(deck, json);
        return deck;
    }

    /**
     * Appends the serialized components (in their stored order) to the deck's component list, each
     * reconstructed from its JSON via its {@link JSONObject} constructor. Adds directly to the
     * underlying list rather than via {@link #add}, so that the original order is preserved exactly
     * and no subclass add-side-effects (e.g. visibility handling) are triggered.
     */
    protected static <T extends Component> void populateComponents(Deck<T> deck, JSONObject json) {
        JSONArray cards = (JSONArray) json.get("cards");
        for (Object o : cards)
            deck.components.add(JSONUtils.loadClassFromJSON((JSONObject) o));
    }

    /**
     * Draws (removes and returns) the top component of the deck, at index 0: the one most recently added with
     * {@link #add(Component)}.
     *
     * @return the top component of the deck, or null if the deck is empty
     */
    public T draw() {
        return pick(0);
    }

    /**
     * Picks a random component from the Deck with specific random object.
     *
     * @param rnd - Random object to use to choose component.
     * @return a random component from the Deck
     */
    public T pick(Random rnd) {
        return pick(rnd.nextInt(components.size()));
    }

    /**
     * Picks the component in position idx from the deck
     *
     * @param idx the index of the component in the deck
     * @return the component in position idx from the deck
     */
    public T pick(int idx) {
        if (!components.isEmpty() && idx < components.size() && idx >= 0) {
            T c = components.get(idx);
            components.remove(idx);
            return c;
        }
        return null;
    }

    /**
     * Draws the last component of the deck
     *
     * @return the last component of the deck
     */
    public T pickLast() {
        return pick(components.size() - 1);
    }

    /**
     * Peeks (without drawing) the top component of the deck, at index 0
     *
     * @return The component peeked.
     */
    public T peek() {
        return peek(0);
    }

    /**
     * Peeks (without drawing) amount components of the deck starting from component idx
     *
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
     *
     * @param idx - index of component to look at.
     * @return The component peeked.
     */
    public T peek(int idx) {
        if (!components.isEmpty() && idx < components.size()) {
            return components.get(idx);
        }
        return null;
    }

    /**
     * Adds a component to the top of a deck (index 0), so it is the next one drawn.
     *
     * @param c component to add
     * @return true if within capacity, false otherwise.
     */
    public boolean add(T c) {
        if (c != null) {
            c.setOwnerId(ownerId);
        }
        return add(c, 0);
    }

    /**
     * Adds a component to a deck on the given index.
     *
     * @param c     component to add
     * @param index where to add it
     * @return true if within capacity, false otherwise.
     */
    public boolean add(T c, int index) {
        if (c == null)
            throw new IllegalArgumentException("null cannot be added to a Deck");
        c.setOwnerId(ownerId);
        components.add(index, c);
        return capacity == -1 || components.size() <= capacity;
    }

    /**
     * Adds a component to a deck at its bottom. Manages the case that the deck is empty.
     *
     * @param c component to add
     * @return true if within capacity, false otherwise.
     */
    public boolean addToBottom(T c) {
        if (components.isEmpty())
            return add(c, 0);
        else return add(c, components.size());
    }

    /**
     * Adds a full other deck to this deck, ignoring capacity.
     *
     * @param d - other deck to add to this deck.
     * @return true if not over capacity, false otherwise.
     */
    public boolean add(Deck<T> d) {
        return this.add(d, 0);
    }

    /**
     * Adds a full other deck to this deck, ignoring capacity.
     *
     * @param d     - other deck to add to this deck.
     * @param index - the position in which the elements of d should be inserted in this deck.
     * @return true if not over capacity, false otherwise.
     */
    public boolean add(Deck<T> d, int index) {
        components.addAll(index, d.components);
        for (T comp : d.components) {
            comp.setOwnerId(ownerId);
        }
        return capacity == -1 || components.size() <= capacity;
    }

    public boolean add(Collection<T> d) {
        return this.add(d, 0);
    }

    public boolean add(Collection<T> d, int index) {
        components.addAll(index, d);
        for (T comp : d) {
            comp.setOwnerId(ownerId);
        }
        return capacity == -1 || components.size() <= capacity;
    }

    /**
     * Remove the given component.
     *
     * @param component - component to remove.
     */
    public void remove(T component) {
        // implementation note. We deliberately do not call components.remove(component)
        // because for PartialObservableDecks we need to remove the element visibility at the correct index
        // hence we *always* only remove from a deck by index
        int index = components.indexOf(component);
        component.setOwnerId(-1);
        if (index != -1) {
            remove(index);
            return;
        }
        throw new IllegalArgumentException(component + " not found in " + this);
    }


    /**
     * Remove the component at the given index.
     *
     * @param idx - index of component to remove.
     */
    public void remove(int idx) {
        if (idx >= 0 && idx < components.size()) {
            components.get(idx).setOwnerId(-1);
            components.remove(idx);
        } else {
            throw new IndexOutOfBoundsException("Index " + idx + " is out of bounds for deck of size " + components.size());
        }
    }

    public void removeAll(List<T> items) {
        for (T component : items)
            remove(component);
    }

    public boolean contains(T card) {
        return components.contains(card);
    }

    /**
     * Removes all the components from the deck.
     */
    public void clear() {
        for (T comp : components) {
            comp.setOwnerId(-1);
        }
        components.clear();
    }

    // Getters, Setters

    /**
     * Shuffles the deck with a specific random object.
     */
    public void shuffle(Random rnd) {
        Collections.shuffle(components, rnd);
    }

    /**
     * Shuffles part of the deck, given by range [fromIndex, toIndex), leaving the rest the same.
     *
     * @param fromIndex - index from where to start shuffling, inclusive
     * @param toIndex   - index where to stop shuffling, exclusive
     * @param rnd       - random number generator used for shuffling
     */
    public void shuffle(int fromIndex, int toIndex, Random rnd) {
        List<T> subList = components.subList(fromIndex, toIndex);
        Collections.shuffle(subList, rnd);
        int i = 0;
        for (T component : subList) {
            components.set(fromIndex + i, component);
            i++;
        }
    }

    /**
     * @return all the components in this deck.
     */
    @Override
    public List<T> getComponents() {
        return components;
    }

    /**
     * Set the components in this deck.
     *
     * @param components - new components for the deck, overrides old content.
     */
    public void setComponents(List<T> components) {
        this.components = components;
        for (T comp : components) {
            comp.setOwnerId(ownerId);
        }
    }

    /**
     * Maximum number of components this deck may contain.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Maximum number of components this deck may contain.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Checks if this game is over capacity.
     *
     * @return true if over capacity, false otherwise.
     */
    public boolean isOverCapacity() {
        return capacity != -1 && components.size() > capacity;
    }

    /**
     * Sets the index to the given component.
     *
     * @param idx       - index of component to replace.
     * @param component - new component.
     */
    public void setComponent(int idx, T component) {
        component.setOwnerId(ownerId);
        components.set(idx, component);
    }

    /**
     * Shortcut for retrieving a specific component.
     *
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

    @SuppressWarnings("unchecked")
    protected void copyTo(Deck<T> deck) {
        List<T> newComponents = new LinkedList<>();
        for (T c : components) {
            newComponents.add((T) c.copy());
        }
        deck.components = newComponents;
        deck.capacity = capacity;

        //copy type and component.
        copyComponentTo(deck);
    }


    @SuppressWarnings("unchecked")
    protected void copyTo(Deck<T> deck, int playerId) {
        List<T> newComponents = new LinkedList<>();
        for (T c : components) {
            newComponents.add((T) c.copy(playerId));
        }
        deck.components = newComponents;
        deck.capacity = capacity;

        //copy type and component.
        copyComponentTo(deck);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (T el : components) {
            sb.append(el.toString());
            sb.append(",");
        }

        if (!sb.isEmpty())
            sb.deleteCharAt(sb.length() - 1);
        else
            sb.append("EmptyDeck");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Deck<?> deck)) return false;
        if (!super.equals(o)) return false;
        return capacity == deck.capacity &&
                Objects.equals(components, deck.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(capacity, ownerId, componentID, components);
    }

}
