package core.components;

import core.AbstractGameState;
import core.CoreConstants.VisibilityMode;
import org.jetbrains.annotations.NotNull;
import utilities.DeterminisationUtilities;
import utilities.Pair;

import java.util.*;

public class PartialObservableDeck<T extends Component> extends Deck<T> {

    // Visibility of the deck, index of array corresponds to player ID
    // (true if player can see the deck, false otherwise)
    protected boolean[] deckVisibility;

    // Visibility of each component in the deck, order corresponds to order of elements in the deck;
    protected List<boolean[]> elementVisibility = new LinkedList<>();

    public boolean getVisibilityForPlayer(int elementIdx, int playerID) {
        return elementVisibility.get(elementIdx)[playerID];
    }

    public boolean[] getVisibilityOfComponent(int elementIdx) {
        return elementVisibility.get(elementIdx);
    }


    public PartialObservableDeck(String id, int ownerID, boolean[] defaultVisibility) {
        super(id, ownerID, VisibilityMode.MIXED_VISIBILITY);
        this.deckVisibility = defaultVisibility;
    }

    public PartialObservableDeck(String id, int ownerID, int nPlayers, VisibilityMode visibilityMode) {
        super(id, ownerID, visibilityMode);
        deckVisibility = new boolean[nPlayers];
        switch (visibilityMode) {
            case VISIBLE_TO_ALL:
                for (int i = 0; i < nPlayers; i++)
                    deckVisibility[i] = true;
                break;
            case VISIBLE_TO_OWNER:
                deckVisibility[ownerID] = true;
                break;
            default:
                // more complicated. Needs to be set elsewhere
                break;
        }
    }

    private PartialObservableDeck(String name, int ownerID, boolean[] defaultVisibility, int ID) {
        super(name, ownerID, ID, VisibilityMode.MIXED_VISIBILITY);
        this.deckVisibility = defaultVisibility;
    }

    /**
     * Retrieves the components in this deck visible by the given player.
     * <p>
     * It always returns a List of the correct length, but with a NULL entry for hidden components
     *
     * @param playerID - ID of player observing the deck.
     * @return - ArrayList of components observed by the player.
     */
    public List<T> getVisibleComponents(int playerID) {
        if (playerID < 0 || playerID >= deckVisibility.length)
            throw new IllegalArgumentException("playerID " + playerID + " needs to be in range [0," + (deckVisibility.length - 1) + "]");

        List<T> visibleComponents = new ArrayList<>(components.size());
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


    @Override
    public void setVisibility(VisibilityMode visibilityMode) {
        super.setVisibility(visibilityMode);
        applyVisibilityMode();
    }

    /**
     * Sets the components in this deck with associated visibility for each player.
     *
     * @param components          - list of components for the deck, overrides old content.
     * @param visibilityPerPlayer - list containing an array of booleans (index in array corresponds to player ID)
     *                            indicating which player can see the specific component (true if player can see it,
     *                            false otherwise).
     */
    public void setComponents(List<T> components, List<boolean[]> visibilityPerPlayer) {
        super.setComponents(components);
        this.elementVisibility = visibilityPerPlayer;
    }


    /**
     * Sets the visibility for each component in the deck.
     *
     * @param visibility - new list of component visibility.
     */
    public void setVisibility(List<boolean[]> visibility) {
        for (boolean[] b : visibility)
            if (b.length != this.deckVisibility.length)
                throw new IllegalArgumentException("All entries of visibility need to have length " + deckVisibility.length +
                        " but at least one entry is of length " + b.length);
        this.elementVisibility = visibility;
    }

    private void applyVisibilityMode() {
        if (getVisibilityMode() == VisibilityMode.TOP_VISIBLE_TO_ALL)
            for (int j = 0; j < deckVisibility.length; j++)
                elementVisibility.get(0)[j] = true;
        if (getVisibilityMode() == VisibilityMode.BOTTOM_VISIBLE_TO_ALL)
            for (int j = 0; j < deckVisibility.length; j++)
                elementVisibility.get(components.size() - 1)[j] = true;
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
                throw new IllegalArgumentException("playerID " + playerID + "needs to be in range [0," + (deckVisibility.length - 1) + "]");
        } else {
            throw new IllegalArgumentException("component index " + index + " needs to be in range [0," + (components.size() - 1) + "]");
        }
    }

    /**
     * Updates the visibility of one component for all players.
     *
     * @param index      - index of component to update visibility for.
     * @param visibility - true if player can see this component, false otherwise.
     */
    public void setVisibilityOfComponent(int index, boolean[] visibility) {
        if (index >= 0 && index < elementVisibility.size() && visibility.length == deckVisibility.length) {
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
        boolean retValue = super.add(c, index);
        applyVisibilityMode();
        return retValue;
    }

    /**
     * Adds a full other deck to this deck, ignoring capacity.
     *
     * @param d     - other deck to add to this deck.
     * @param index - the position in which the elements of d should be inserted in this deck.
     * @return true if not over capacity, false otherwise.
     */
    @Override
    public boolean add(Deck<T> d, int index) {
        if (d instanceof PartialObservableDeck<T> pod) {
            int length = d.components.size();
            for (int i = 0; i < length; i++) {
                // Add in reverse order to keep the order of the deck
                // this is to ties up with addAll() of components in super.add() a few lines down
                this.elementVisibility.add(index, pod.elementVisibility.get(length - i - 1).clone());
            }
        } else {
            for (int i = 0; i < d.components.size(); i++) {
                this.elementVisibility.add(index, deckVisibility.clone());
            }
        }
        boolean retValue = super.add(d, index);
        applyVisibilityMode();
        return retValue;
    }

    @Override
    public boolean add(Collection<T> d, int index) {
        for (int i = 0; i < d.size(); i++) {
            this.elementVisibility.add(index, deckVisibility.clone());
        }
        return super.add(d, index);
    }

    @Override
    public boolean add(Deck<T> d) {
        return add(d, 0);
    }

    @Override
    public void setComponents(List<T> components) {
        super.setComponents(components);
        elementVisibility.clear();
        for (int i = 0; i < components.size(); i++) {
            elementVisibility.add(deckVisibility.clone());
        }
        applyVisibilityMode();
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
        if (components.isEmpty())
            return add(c, 0, deckVisibility);
        return add(c, components.size(), deckVisibility);
    }

    @Override
    public boolean add(T c) {
        return add(c, deckVisibility);
    }

    @Override
    public void remove(int idx) {
        super.remove(idx);
        elementVisibility.remove(idx);
    }

    @Override
    public void clear() {
        super.clear();
        elementVisibility.clear();
    }

    /**
     * Shuffles the deck, and updates the visibility of the components accordingly.
     * After a shuffle players will no longer know the position of the cards they could see before the shuffle
     * (this resets the visibility of all components to the default deck visibility).
     *
     * @param rnd
     */
    @Override
    public void shuffle(Random rnd) {
        elementVisibility.replaceAll(ignored -> deckVisibility.clone());
        super.shuffle(rnd);
        applyVisibilityMode();
    }

    /**
     * Shuffles a deck in its entirety, but players can see any card that they could see before the shuffle (in the new position)
     *
     * @param rnd random number generator to be used in shuffling.
     */
    public void shuffleAndKeepVisibility(Random rnd) {
        Pair<List<T>, List<boolean[]>> shuffled = shuffleLists(components, elementVisibility, rnd);
        components = shuffled.a;
        elementVisibility = shuffled.b;
        applyVisibilityMode();
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
        List<T> tmp_components = new LinkedList<>();
        List<boolean[]> tmp_visibility = new LinkedList<>();

        List<Integer> indexList = new ArrayList<>(comps.size());
        for (int i = 0; i < comps.size(); i++)
            indexList.add(i);
        Collections.shuffle(indexList, rnd);

        for (int targetIndex = 0; targetIndex < indexList.size(); targetIndex++) {
            int sourceIndex = indexList.get(targetIndex);
            tmp_components.add(targetIndex, comps.get(sourceIndex));
            tmp_visibility.add(targetIndex, vis.get(sourceIndex));
        }

        return new Pair<>(tmp_components, tmp_visibility);
    }

    /**
     * Shuffles components based on visibility, leaving those with opposite visibility in the same place.
     *
     * @param rnd      - random object to use for shuffling.
     * @param playerId - player observing the deck.
     */
    public void redeterminiseUnknown(Random rnd, int playerId) {
        DeterminisationUtilities.reshuffle(playerId, List.of(this), c -> true, rnd);
    }

    public boolean[] getDeckVisibility() {
        return deckVisibility;
    }

    @Override
    public PartialObservableDeck<T> copy() {
        PartialObservableDeck<T> dp = new PartialObservableDeck<>(componentName, ownerId, deckVisibility, componentID);
        this.copyTo(dp); // Copy super

        return commonCopy(dp);
    }

    public PartialObservableDeck<T> copy(int playerId) {
        PartialObservableDeck<T> dp = new PartialObservableDeck<>(componentName, ownerId, deckVisibility, componentID);
        this.copyTo(dp, playerId); // Copy super

        return commonCopy(dp);
    }

    @NotNull
    private PartialObservableDeck<T> commonCopy(PartialObservableDeck<T> dp) {
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

        if (!sb.isEmpty())
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

