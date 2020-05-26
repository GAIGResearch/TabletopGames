package core.components;

import java.util.*;


public class PartialObservableDeck<T extends Component> extends Deck<T> {

    // Number of players in the game
    protected final int nPlayers;

    // Visibility of the deck, index of array corresponds to player ID
    // (true if player can see the deck, false otherwise)
    protected boolean[] deckVisibility;

    // Visibility of each component in the deck, order corresponds to order of elements in the deck;
    protected ArrayList<boolean[]> elementVisibility = new ArrayList<>();

    public PartialObservableDeck(String id, boolean[] defaultVisibility) {
        super(id);
        this.deckVisibility = defaultVisibility;
        this.nPlayers = defaultVisibility.length;
    }

    public PartialObservableDeck(String id, int nPlayers) {
        this(id, new boolean[nPlayers]);
    }

    /**
     * Retrieves the components in this deck visible by the given player.
     * @param playerID - ID of player observing the deck.
     * @return - ArrayList of components observed by the player.
     */
    public ArrayList<T> getVisibleComponents(int playerID) {
        if (playerID >= 0 && playerID > nPlayers)
            throw new IllegalArgumentException("playerID "+ playerID + "needs to be in range [0," + nPlayers +"]");

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
     * Sets the components in this deck with associated visibility for each player.
     * @param components - list of components for the deck, overrides old content.
     * @param visibilityPerPlayer - list containing an array of booleans (index in array corresponds to player ID)
     *                            indicating which player can see the specific component (true if player can see it,
     *                            false otherwise).
     */
    public void setComponents(ArrayList<T> components, ArrayList<boolean[]> visibilityPerPlayer ) {
        super.setComponents(components);
        this.elementVisibility = visibilityPerPlayer;
    }

    /**
     * Sets the visibility for each component in the deck.
     * @param visibility - new list of component visibility.
     */
    public void setVisibility(ArrayList<boolean[]> visibility) {
        for (boolean[] b : visibility)
            if (b.length != this.nPlayers)
                throw new IllegalArgumentException("All entries of visibility need to have length " + nPlayers +
                        " but at least one entry is of length " + b.length);
        this.elementVisibility = visibility;
    }

    /**
     * Updates the visibility of one component for one player.
     * @param index - index of component to update visibility for.
     * @param playerID - ID of player observing the component.
     * @param visibility - true if player can see this component, false otherwise.
     */
    public void setVisibilityOfComponent(int index, int playerID, boolean visibility) {
        if (index >= 0 && index < elementVisibility.size()) {
            if (playerID >= 0 && playerID < nPlayers)
                this.elementVisibility.get(index)[playerID] = visibility;
            else
                throw new IllegalArgumentException("playerID "+ playerID + "needs to be in range [0," + nPlayers +"]");
        } else {
            throw new IllegalArgumentException("component index "+ index + " needs to be in range [0," + components.size() +"]");
        }
    }

    /**
     * Adds a new component with associated player visibility array.
     * @param c - component to add.
     * @param visibilityPerPlayer - array of booleans where each index corresponds to player ID and indicates of the
     *                            respective player can see the component (true) or not (false).
     * @return true if not over capacity, false otherwise.
     */
    public boolean add(T c, boolean[] visibilityPerPlayer) {
        return add(c, 0, visibilityPerPlayer);
    }

    /**
     * Adds a new component at the given index, with associated player visibility array.
     * @param c - component to add.
     * @param index - where to add the component.
     * @param visibilityPerPlayer - array of booleans where each index corresponds to player ID and indicates of the
     *                            respective player can see the component (true) or not (false).
     * @return true if not over capacity, false otherwise.
     */
    public boolean add(T c, int index, boolean[] visibilityPerPlayer) {
        this.elementVisibility.add(index, visibilityPerPlayer.clone());
        return super.add(c, index);
    }

    /**
     * Adds a full other deck to this deck, ignoring capacity, and copies visibility as well.
     * @param d - other deck to add to this deck.
     * @return true if not over capacity, false otherwise.
     */
    public boolean add(PartialObservableDeck<T> d){
        if (d == null)
            throw new IllegalArgumentException("d cannot be null");
        elementVisibility.addAll(d.elementVisibility);
        for (int i = 0; i < nPlayers; i++) {
            deckVisibility[i] &= d.deckVisibility[i];
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
        if(el != null) {
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
    public boolean add(T c) {
        return add(c, deckVisibility);
    }

    @Override
    public boolean remove(int idx){
        if (super.remove(idx)){
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
        ArrayList<T> tmp_components = new ArrayList<>();
        ArrayList<boolean[]> tmp_visibility = new ArrayList<>();

        List<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < components.size(); i++)
            indexList.add(i);
        Collections.shuffle(indexList, rnd);

        for (int targetIndex = 0; targetIndex < indexList.size(); targetIndex++){
            int sourceIndex = indexList.get(targetIndex);
            tmp_components.add(targetIndex, components.get(sourceIndex));
            tmp_visibility.add(targetIndex, elementVisibility.get(sourceIndex));
        }

        components = tmp_components;
        elementVisibility = tmp_visibility;
    }

    @Override
    public void shuffle() {
        this.shuffle(new Random());
    }

    @Override
    public PartialObservableDeck<T> copy()
    {
        PartialObservableDeck<T> dp = (PartialObservableDeck<T>)super.copy();
        dp.deckVisibility = deckVisibility.clone();

        ArrayList<boolean[]> newVisibility = new ArrayList<>();
        for (boolean[] visibility : elementVisibility)
        {
            newVisibility.add(visibility.clone());
        }
        dp.elementVisibility = newVisibility;

        return dp;
    }

    public String toString(int playerID){
        StringBuilder sb = new StringBuilder();
        for (T el : getVisibleComponents(playerID)){
            if (el==null)
                sb.append("UNKNOWN");
            else
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

