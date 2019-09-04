package components;

import java.util.*;
import utilities.Utils.ComponentType;

public class Deck extends Component implements IDeck {

    protected int capacity;

    protected ArrayList<Card> cards;

    protected Random rnd;

    protected Deck()
    {
        super.type = ComponentType.DECK;
        cards = new ArrayList<>();
        rnd = new Random();
    }

    protected Deck(Random rnd, int capacity)
    {
        super.type = ComponentType.DECK;
        cards = new ArrayList<>();
        this.rnd = rnd;
        this.capacity = capacity;
    }

    protected void setCards(ArrayList<Card> cards) {this.cards = cards;}

    @Override
    public int getCapacity() {
        return capacity;
    }

    public void shuffle() {
        Collections.shuffle(cards, rnd);
    }

    public Card draw() {
        return pick(0);
    }

    public Card pick() {
        return pick(0);
    }

    public Card pick(int idx) {
        if(cards.size() > 0 && idx < cards.size()) {
            Card c = cards.get(idx);
            cards.remove(idx);
            return c;
        }
        return null;
    }

    public Card pickLast() {
        return pick(cards.size()-1);
    }

    @Override
    public Card peek() {
        return peek(0);
    }

    @Override
    public Card[] peek(int idx, int amount) {
        ArrayList<Card> cards = new ArrayList<>();
        for(int i = idx; i < idx+amount; ++i)
        {
            Card c = peek(i);
            if(c != null)
                cards.add(c);
        }
        return (Card[]) cards.toArray();
    }

    private Card peek(int idx)
    {
        if(cards.size() > 0 && idx < cards.size()) {
            Card c = cards.get(idx);
            return c;
        }
        return null;
    }

    public boolean add(Card c) {
        if (cards.size() < capacity) {
            cards.add(0, c);
            return true;
        }
        return false;
    }


    @Override
    public IDeck copy()
    {
        Deck dp = new Deck();
        this.copyTo(dp);
        return dp;
    }

    public void copyTo(IDeck target)
    {
        Deck dp = (Deck) target;
        ArrayList<Card> newCards = new ArrayList<>();
        for (Card c : dp.cards)
        {
            newCards.add(c.copy());
        }
        dp.setCards(newCards);
        dp.capacity = capacity;
        dp.rnd = rnd;
    }

    // TODO: check for visibility?
    public ArrayList<Card> getCards() {
        return cards;
    }
}
