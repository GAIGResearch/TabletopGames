package core.components;

import games.loveletter.cards.LoveLetterCard;

import java.util.*;


public class PartialObservableDeck<T> extends Deck<T> implements IPartialObservableDeck<T> {

    protected ArrayList<boolean[]> visibilityPerPlayer = new ArrayList<>();
    protected final boolean[] defaultVisibility;
    protected final int nPlayers;

    public PartialObservableDeck(String id, boolean[] defaultVisibility) {
        super(id);
        this.defaultVisibility = defaultVisibility;
        this.nPlayers = defaultVisibility.length;
    }

    public PartialObservableDeck(String id, int nPlayers) {
        this(id, new boolean[nPlayers]);
    }

    public ArrayList<T> getVisibleCards(int playerID) {
        if (playerID >= 0 && playerID > nPlayers)
            throw new IllegalArgumentException("playerID "+ playerID + "needs to be in range [0," + nPlayers +"]");

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

    @Override
    public void setCards(ArrayList<T> cards) {
        super.setCards(cards);

        visibilityPerPlayer.clear();
        for (int i = 0; i < cards.size(); i++)
            visibilityPerPlayer.add(defaultVisibility.clone());
    }

    @Override
    public void setCards(ArrayList<T> cards, ArrayList<boolean[]> visibilityPerPlayer ) {
        super.setCards(cards);
        this.visibilityPerPlayer = visibilityPerPlayer;
    }

    public void setVisibility(ArrayList<boolean[]> visibility) {
        for (boolean[] b : visibility)
            if (b.length != this.nPlayers)
                throw new IllegalArgumentException("All entries of visibility need to have length " + nPlayers +
                        " but at least one entry is of length " + b.length);
        this.visibilityPerPlayer = visibility;
    }

    @Override
    public void setVisibilityOfCard(int cardInd, int playerID, boolean visibility) {
        if (cardInd >= 0 && cardInd < visibilityPerPlayer.size()) {
            if (playerID >= 0 && playerID < defaultVisibility.length)
                this.visibilityPerPlayer.get(cardInd)[playerID] = visibility;
            else
                throw new IllegalArgumentException("playerID "+ playerID + "needs to be in range [0," + nPlayers +"]");
        } else {
            throw new IllegalArgumentException("cardInd "+ cardInd + " needs to be in range [0," + cards.size() +"]");
        }
    }

    @Override
    public T pick(int idx) {
        T el = super.pick(idx);
        if(el != null) {
            visibilityPerPlayer.remove(idx);
            return el;
        }
        return null;
    }

    @Override
    public boolean add(T c, int index) {
        return add(c, index, defaultVisibility);
    }

    @Override
    public boolean add(T c) {
        return add(c, defaultVisibility);
    }

    public boolean add(T c, boolean[] visibilityPerPlayer) {
        return add(c, 0, visibilityPerPlayer);
    }

    public boolean add(T c, int index, boolean[] visibilityPerPlayer) {
        this.visibilityPerPlayer.add(index, visibilityPerPlayer.clone());
        return super.add(c, index);
    }

    public boolean add(PartialObservableDeck<T> d){
        if (d == null)
            throw new IllegalArgumentException("d cannot be null");
        visibilityPerPlayer.addAll(d.visibilityPerPlayer);
        return super.add(d);
    }

    @Override
    public boolean remove(T el)
    {
        int indexOfCard = cards.indexOf(el);
        if (indexOfCard != -1){
            return remove(indexOfCard);
        }
        return false;
    }

    @Override
    public boolean remove(int idx){
        if (super.remove(idx)){
            visibilityPerPlayer.remove(idx);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        super.clear();
        visibilityPerPlayer.clear();
    }

    @Override
    public void shuffle(Random rnd) {
        ArrayList<T> tmp_cards = new ArrayList<>();
        ArrayList<boolean[]> tmp_visibility = new ArrayList<>();

        List<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++)
            indexList.add(i);
        Collections.shuffle(indexList, rnd);

        for (int targetIndex = 0; targetIndex < indexList.size(); targetIndex++){
            int sourceIndex = indexList.get(targetIndex);
            tmp_cards.add(targetIndex, cards.get(sourceIndex));
            tmp_visibility.add(targetIndex, visibilityPerPlayer.get(sourceIndex));
        }

        cards = tmp_cards;
        visibilityPerPlayer = tmp_visibility;
    }

    @Override
    public void shuffle() {
        this.shuffle(new Random());
    }

    @Override
    public PartialObservableDeck<T> copy()
    {
        PartialObservableDeck<T> dp = new PartialObservableDeck<>(this.id, this.defaultVisibility);
        this.copyTo(dp);
        return dp;
    }

    public void copyTo(PartialObservableDeck<T> target)
    {
        super.copyTo(target); // copies the cards and the capacity

        ArrayList<boolean[]> newVisibility = new ArrayList<>();
        for (boolean[] visibility : visibilityPerPlayer)
        {
            try {
                newVisibility.add(visibility.clone());
            } catch (Exception e) {
                throw new RuntimeException("Objects in deck target do not implement the method 'clone'", e);
            }
        }
        target.visibilityPerPlayer = newVisibility;
    }

    public String toString(int playerID){
        StringBuilder sb = new StringBuilder();
        for (T el : getVisibleCards(playerID)){
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

