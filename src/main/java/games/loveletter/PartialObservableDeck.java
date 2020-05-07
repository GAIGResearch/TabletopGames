package games.loveletter;

import core.components.Deck;
import utilities.Utils;

import java.util.*;

// TODO: update IDeck interface from methods in this class
public class PartialObservableDeck<T> extends Deck<T> implements IPartialObservableDeck<T> {

    protected ArrayList<boolean[]> visibilityPerPlayer;
    protected boolean[] defaultVisbility;

    private String id;

    public PartialObservableDeck(int nPlayers)
    {
        super();
        super.type = Utils.ComponentType.DECK;
        cards = new ArrayList<>();
        visibilityPerPlayer = new ArrayList<>();
        properties = new HashMap<>();
        defaultVisbility = new boolean[nPlayers];
        Arrays.fill(defaultVisbility, false);
    }

    public PartialObservableDeck(boolean[] defaultVisbility)
    {
        super.type = Utils.ComponentType.DECK;
        cards = new ArrayList<>();
        visibilityPerPlayer = new ArrayList<>();
        properties = new HashMap<>();
        this.defaultVisbility = defaultVisbility;
    }

    public PartialObservableDeck(boolean[] defaultVisbility, int capacity)
    {
        this(defaultVisbility);
        this.capacity = capacity;
    }

    protected void setCards(ArrayList<T> cards, ArrayList<boolean[]> visibilityPerPlayer ) {
        this.cards = cards;
        this.visibilityPerPlayer = visibilityPerPlayer;
    }

    public boolean setVisibility(int index, int player, boolean visibility) {
        if (index < visibilityPerPlayer.size()) {
            this.visibilityPerPlayer.get(index)[player] = visibility;
            return true;
        }
        return false;
    }

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
        for (boolean[] b : visibilityPerPlayer){
            System.arraycopy(defaultVisbility, 0, b, 0, b.length);
        }
    }

    public void shuffle() {
        Collections.shuffle(cards, new Random());
        for (boolean[] b : visibilityPerPlayer){
            System.arraycopy(defaultVisbility, 0, b, 0, b.length);
        }
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
            visibilityPerPlayer.remove(idx);
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

    @Override
    public boolean remove(T el)
    {
        int indexOfCard = cards.indexOf(el);
        if (indexOfCard == -1)
            System.out.println("what");
        cards.remove(indexOfCard);
        visibilityPerPlayer.remove(indexOfCard);
        return true;
    }


    @Override
    public boolean add(T c, int index) {
        return add(c, index, defaultVisbility);
    }

    @Override
    public boolean add(T c) {
        return add(c, defaultVisbility);
    }

    public boolean add(T c, boolean[] visibilityPerPlayer) {
        return add(c, 0, visibilityPerPlayer);
    }

    public boolean add(T c, int index, boolean[] visibilityPerPlayer) {
        cards.add(index, c);
        this.visibilityPerPlayer.add(index, visibilityPerPlayer.clone());
        return capacity == -1 || cards.size() <= capacity;
    }

    public boolean add(PartialObservableDeck<T> d){
        cards.addAll(d.cards);
        visibilityPerPlayer.addAll(d.visibilityPerPlayer);
        return true;
    }


    @Override
    public PartialObservableDeck<T> copy()
    {
        PartialObservableDeck<T> dp = new PartialObservableDeck<T>(this.defaultVisbility);
        this.copyTo(dp);
        return dp;
    }

    public void copyTo(IPartialObservableDeck<T> target)
    {
        PartialObservableDeck<T> dp = (PartialObservableDeck<T>) target;
        ArrayList<T> newCards = new ArrayList<>();
        for (T c : dp.cards)
        {
            try {
                newCards.add((T) c.getClass().getMethod("clone").invoke(c));
            } catch (Exception e) {
                throw new RuntimeException("Objects in deck target do not implement the method 'clone'", e);
            }
        }

        ArrayList<boolean[]> newVisibility = new ArrayList<>(dp.visibilityPerPlayer);

        dp.setCards(newCards, newVisibility);
        dp.capacity = capacity;

        //copy type and component.
        copyComponentTo(dp);
    }

    // TODO: check for visibility?
    @Override
    public ArrayList<T> getCards() {
        return cards;
    }

    @Override
    public void setCards(ArrayList<T> cards) {
        this.cards = cards;
    }

    public ArrayList<T> getVisibleCards(int playerID) {
        ArrayList<T> visibleCards = new ArrayList<>(cards.size());
        for (int i = 0; i < cards.size(); i++) {
            boolean[] b = visibilityPerPlayer.get(i);
            if (b[playerID])
                visibleCards.add(i, cards.get(i));
            else
                visibleCards.add(i, null);
        }
        return visibleCards;
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

    public void discard(T card) {
        int indexOfCard = cards.indexOf(card);
        cards.remove(indexOfCard);
        visibilityPerPlayer.remove(indexOfCard);
    }
}

