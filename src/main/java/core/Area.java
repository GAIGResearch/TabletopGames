package core;

import java.util.HashSet;

public class Area {
    protected HashSet<Component> components;


    public Area() {
        components = new HashSet<Component>();
    }

    public Area(HashSet<Component> components) {
        this.components = new HashSet<Component>();
        for (Component old_component:components)
            this.components.add(old_component);
    }

    public Area copy() {
        Area new_area = new Area();
        new_area.components = new HashSet<Component>();
        for (Component old_component:this.components)
            new_area.components.add(old_component);
        return new_area;
    }

    /**
     * get all components
     */
    public HashSet<Component> getComponents() { return components; }


    /**
     * get only one type
     */
    public HashSet<Component> getComponentsOfType(ComponentType type) {
        HashSet<Component> new_components = new HashSet<Component>();
        for (Component component:components) {
            if (component.getType() == type)
                new_components.add(component);
        }
        return new_components;
    }

    /**
     * get only boards
     */
    public HashSet<Board> getBoard() {
        HashSet<Board> boards = new HashSet<Board>();
        for (Component component:components) {
            if (component.getType() == ComponentType.BOARD)
                boards.add((Board) component);
        }
        return boards;
    }

    /**
     * get only decks
     */
    public HashSet<Deck> getDecks() {
        HashSet<Deck> decks = new HashSet<Deck>();
        for (Component component:components) {
            if (component.getType() == ComponentType.DECK)
                decks.add((Deck) component);
        }
        return decks;
    }

    /**
     * get only tokens
     */
    public HashSet<Token> getTokens()
    {
        HashSet<Token> tokens = new HashSet<Token>();
        for (Component component:components) {
            if (component.getType() == ComponentType.TOKEN)
                tokens.add((Token) component);
        }
        return tokens;
    }

    /**
     * get only counters
     */
    public HashSet<Counter> getCounters() {
        HashSet<Counter> counters = new HashSet<Counter>();
        for (Component component:components) {
            if (component.getType() == ComponentType.COUNTER)
                counters.add((Counter) component);
        }
        return counters;
    }

    /**
     * add new token
     */
    public void addToken(Token token) {
        token.setType(ComponentType.TOKEN);
        components.add(token);
    }

    /**
     * add new counter
     */
    public void addCounter(Counter counter) {
        counter.setType(ComponentType.COUNTER);
        components.add(counter);
    }

    /**
     * add new deck
     */
    public void addDeck(Deck deck) {
        deck.setType(ComponentType.DECK);
        components.add(deck);
    }

    public void addBoard(Deck board) {
        board.setType(ComponentType.BOARD);
        components.add(board);
    }
}
