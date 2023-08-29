package core.components;

import core.AbstractGameState;
import core.CoreConstants.VisibilityMode;
import utilities.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PartialObservableDeck<T extends Component> extends Deck<T> {

    // Visibility of the deck, index of array corresponds to player ID
    // (true if player can see the deck, false otherwise)
    protected boolean[] deckVisibility;

    // Visibility of each component in the deck, order corresponds to order of elements in the deck;
    protected List<boolean[]> elementVisibility = new ArrayList<>();

    public boolean getVisibilityForPlayer(int elementIdx, int playerID) {
        return elementVisibility.get(elementIdx)[playerID];
    }

    public boolean[] getVisibilityOfComponent(int elementIdx) {
        return elementVisibility.get(elementIdx);
    }

    public PartialObservableDeck(String id, boolean[] defaultVisibility) {
        this(id, -1, defaultVisibility);
    }

    public PartialObservableDeck(String id, int ownerID, boolean[] defaultVisibility) {
        super(id, ownerID, VisibilityMode.MIXED_VISIBILITY);
        this.deckVisibility = defaultVisibility;
    }

    public PartialObservableDeck(String id, int nPlayers) {
        this(id, -1, new boolean[nPlayers]);
    }

    public PartialObservableDeck(String id, int ownerID, VisibilityMode visibilityMode) {
        super(id, ownerID, visibilityMode);
    }

    public PartialObservableDeck(String id, int ownerID, int nPlayers) {
        this(id, ownerID, new boolean[nPlayers]);
    }

    private PartialObservableDeck(String name, int ownerID, boolean[] defaultVisibility, int ID) {
        super(name, ownerID, ID, VisibilityMode.MIXED_VISIBILITY);
        this.deckVisibility = defaultVisibility;
    }

    /**
     * Retrieves the components in this deck visible by the given player.
     *
     * It always returns a List of the correct length, but with a NULL entry for hidden components
     *
     * @param playerID - ID of player observing the deck.
     * @return - ArrayList of components observed by the player.
     */
    public ArrayList<T> getVisibleComponents(int playerID) {
        if (playerID < 0 || playerID >= deckVisibility.length)
            throw new IllegalArgumentException("playerID " + playerID + " needs to be in range [0," + (deckVisibility.length - 1) + "]");

        ArrayList<T> visibleComponents = new ArrayList<>(components.size());
        for (int i = 0; i < components.size(); i++) {
            boolean[] b = elementVisibility.get(i);
            if (b[playerID])
                visibleComponents.add(i, components.get(i));
            else
                visibleComponents.add(i, null);
        }
        return visibleComponents;
    }

    /**
     * Returns true if the component queried is visible to the given player, otherwise false.
     *
     * @param idx      - index of component to check visibility for.
     * @param playerID - ID of player.
     * @return - true if visible, false otherwise.
     */
    public boolean isComponentVisible(int idx, int playerID) {
        if (playerID < 0 || playerID >= deckVisibility.length)
            throw new IllegalArgumentException("playerID " + playerID + " needs to be in range [0," + (deckVisibility.length - 1) + "]");
        return elementVisibility.get(idx)[playerID];
    }

    /**
     * Sets the components in this deck with associated visibility for each player.
     *
     * @param components          - list of components for the deck, overrides old content.
     * @param visibilityPerPlayer - list containing an array of booleans (index in array corresponds to player ID)
     *                            indicating which player can see the specific component (true if player can see it,
     *                            false otherwise).
     */
    public void setComponents(ArrayList<T> components, ArrayList<boolean[]> visibilityPerPlayer) {
        super.setComponents(components);
        this.elementVisibility = visibilityPerPlayer;
    }

    /**
     * Sets the visibility for each component in the deck.
     *
     * @param visibility - new list of component visibility.
     */
    public void setVisibility(ArrayList<boolean[]> visibility) {
        for (boolean[] b : visibility)
            if (b.length != this.deckVisibility.length)
                throw new IllegalArgumentException("All entries of visibility need to have length " + deckVisibility.length +
                        " but at least one entry is of length " + b.length);
        this.elementVisibility = visibility;
    }

    /**
     * Updates the visibility of one component for one player.
     *
     * @param index      - index of component to update visibility for.
     * @param playerID   - ID of player observing the component.
     * @param visibility - true if player can see this component, false otherwise.
     */
    public void setVisibilityOfComponent(int index, int playerID, boolean visibility) {
        if (index >= 0 && index < elementVisibility.size()) {
            if (playerID >= 0 && playerID < deckVisibility.length)
                this.elementVisibility.get(index)[playerID] = visibility;
            else
                throw new IllegalArgumentException("playerID " + playerID + "needs to be in range [0," + (deckVisibility.length-1) + "]");
        } else {
            throw new IllegalArgumentException("component index " + index + " needs to be in range [0," + (components.size()-1) + "]");
        }
    }

    /**
     * Updates the visibility of one component for all players.
     *
     * @param index      - index of component to update visibility for.
     * @param visibility - true if player can see this component, false otherwise.
     */
    public void setVisibilityOfComponent(int index, boolean[] visibility) {
        if (index >= 0 && index < elementVisibility.size()) {
            this.elementVisibility.set(index, visibility.clone());
        } else {
            throw new IllegalArgumentException("component index " + index + " needs to be in range [0," + components.size() + "]");
        }
    }

    /**
     * Adds a new component with associated player visibility array.
     *
     * @param c                   - component to add.
     * @param visibilityPerPlayer - array of booleans where each index corresponds to player ID and indicates of the
     *                            respective player can see the component (true) or not (false).
     * @return true if not over capacity, false otherwise.
     */
    public boolean add(T c, boolean[] visibilityPerPlayer) {
        return add(c, 0, visibilityPerPlayer);
    }

    /**
     * Adds a new component at the given index, with associated player visibility array.
     *
     * @param c                   - component to add.
     * @param index               - where to add the component.
     * @param visibilityPerPlayer - array of booleans where each index corresponds to player ID and indicates of the
     *                            respective player can see the component (true) or not (false).
     * @return true if not over capacity, false otherwise.
     */
    public boolean add(T c, int index, boolean[] visibilityPerPlayer) {
        this.elementVisibility.add(index, visibilityPerPlayer.clone());
        return super.add(c, index);
    }

    /**
     * Adds a full other deck to this deck, ignoring capacity.
     *
     * @param d     - other deck to add to this deck.
     * @param index - the position in which the elements of d should be inserted in this deck.
     * @return true if not over capacity, false otherwise.
     */
    public boolean add(Deck<T> d, int index) {
        for (int i = 0; i < d.components.size(); i++) {
            this.elementVisibility.add(index, deckVisibility.clone());
        }
        return super.add(d, index);
    }

    /**
     * Adds a full other deck to this deck, ignoring capacity, and copies visibility as well.
     *
     * @param d - other deck to add to this deck.
     * @return true if not over capacity, false otherwise.
     */
    public boolean add(PartialObservableDeck<T> d) {
        if (d == null)
            throw new IllegalArgumentException("d cannot be null");
        elementVisibility.addAll(d.elementVisibility);
        for (int i = 0; i < deckVisibility.length; i++) {
            deckVisibility[i] &= d.deckVisibility[i];
        }
        return super.add(d);
    }

    @Override
    public boolean add(Deck<T> d) {
        if (d == null)
            throw new IllegalArgumentException("d cannot be null");
        for (int i = 0; i < d.getSize(); i++) {
            elementVisibility.add(deckVisibility.clone());
        }
        return super.add(d);
    }

    @Override
    public void setComponents(ArrayList<T> components) {
        super.setComponents(components);

        elementVisibility.clear();
        for (int i = 0; i < components.size(); i++) {
            elementVisibility.add(deckVisibility.clone());
        }
    }

    @Override
    public T pick(int idx) {
        T el = super.pick(idx);
        if (el != null) {
            elementVisibility.remove(idx);
            return el;
        }
        return null;
    }

    @Override
    public boolean add(T c, int index) {
        return add(c, index, deckVisibility);
    }

    @Override
    public boolean addToBottom(T c) {
        if(components.size() == 0)
            return add(c, 0, deckVisibility);
        else return add(c, components.size() -1, deckVisibility);
    }

    @Override
    public boolean add(T c) {
        return add(c, deckVisibility);
    }

    @Override
    public boolean remove(int idx) {
        if (super.remove(idx)) {
            elementVisibility.remove(idx);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        super.clear();
        elementVisibility.clear();
    }

    @Override
    public void shuffle(Random rnd) {
        Pair<List<T>, List<boolean[]>> shuffled = shuffleLists(components, elementVisibility, rnd);
        components = shuffled.a;
        elementVisibility = shuffled.b;
    }

    /**
     * Shuffles a deck in its entirety, and resets the visibility of all components to the default visibility of the deck.
     * @param rnd random number generator to be used in shuffling.
     */
    public void shuffleAndResetVisibility(Random rnd)
    {
        shuffle(rnd);
        elementVisibility.replaceAll(ignored -> deckVisibility.clone());
    }


    /**
     * Shuffles a list of components and associated visibility
     *
     * @param comps - list of components
     * @param vis   - associated visibility
     * @param rnd   - random number generator to be used in shuffling.
     * @return - both lists shuffled, keeping the mapping from component to visibility at the same index.
     */
    private Pair<List<T>, List<boolean[]>> shuffleLists(List<T> comps, List<boolean[]> vis, Random rnd) {
        List<T> tmp_components = new ArrayList<>();
        List<boolean[]> tmp_visibility = new ArrayList<>();

        List<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < comps.size(); i++)
            indexList.add(i);
        Collections.shuffle(indexList, rnd);

        for (int targetIndex = 0; targetIndex < indexList.size(); targetIndex++) {
            int sourceIndex = indexList.get(targetIndex);
            tmp_components.add(targetIndex, comps.get(sourceIndex));
            tmp_visibility.add(targetIndex, vis.get(sourceIndex));
        }

        return new Pair(tmp_components, tmp_visibility);
    }

    /**
     * Shuffles components based on visibility, leaving those with opposite visibility in the same place.
     *
     * @param rnd      - random object to use for shuffling.
     * @param playerId - player observing the deck.
     * @param visible  - if true, shuffles only visible cards; otherwise, shuffles only hidden cards.
     */
    public void shuffleVisible(Random rnd, int playerId, boolean visible) {
        ArrayList<T> visibleComponents = new ArrayList<>();
        ArrayList<boolean[]> visibility = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            boolean[] b = elementVisibility.get(i);
            if (b[playerId] == visible) {
                visibleComponents.add(components.get(i));
                visibility.add(b);
            }
        }
        Pair<List<T>, List<boolean[]>> shuffled = shuffleLists(visibleComponents, visibility, rnd);

        int n = 0;
        for (int i = 0; i < components.size(); i++) {
            boolean[] b = elementVisibility.get(i);
            if (b[playerId] == visible) {
                // Draw element from shuffled lists
                components.set(i, shuffled.a.get(n));
                /*
                if other players can see a card, we know which card position they can see, but
                not the actual card (otherwise, it would by definition be visible to us). Therefore
                we do *not* shuffle element visibility, and keep this in the same order
                */
                if (visible)
                    elementVisibility.set(i, shuffled.b.get(n));
                n++;
            }
        }
    }

    public boolean[] getDeckVisibility() {
        return deckVisibility;
    }

    @Override
    public PartialObservableDeck<T> copy() {
        PartialObservableDeck<T> dp = new PartialObservableDeck<>(componentName, ownerId, deckVisibility, componentID);
        this.copyTo(dp); // Copy super

        dp.deckVisibility = deckVisibility.clone();

        ArrayList<boolean[]> newVisibility = new ArrayList<>();
        for (boolean[] visibility : elementVisibility) {
            newVisibility.add(visibility.clone());
        }
        dp.elementVisibility = newVisibility;

        return dp;
    }

    public PartialObservableDeck<T> copy(int playerId) {
        PartialObservableDeck<T> dp = new PartialObservableDeck<>(componentName, ownerId, deckVisibility, componentID);
        this.copyTo(dp, playerId); // Copy super

        dp.deckVisibility = deckVisibility.clone();

        ArrayList<boolean[]> newVisibility = new ArrayList<>();
        for (boolean[] visibility : elementVisibility) {
            newVisibility.add(visibility.clone());
        }
        dp.elementVisibility = newVisibility;

        return dp;
    }

    public String toString(AbstractGameState gs, int playerID) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < components.size(); i++) {
            if (!isComponentVisible(i, playerID) && gs.getCoreGameParameters().partialObservable)
                sb.append("UNKNOWN");
            else
                sb.append(components.get(i).toString());
            sb.append(",");
        }

        if (sb.length() > 0)
            sb.deleteCharAt(sb.length() - 1);
        else
            sb.append("EmptyDeck");

        return sb.toString();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

