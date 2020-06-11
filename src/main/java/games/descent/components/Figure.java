package games.descent.components;

import core.components.Token;

// TODO: figure out how to do ability/heroic-feat + multi-dice for defence
public class Figure extends Token {
    int xp;

    public Figure(String name) {
        super(name);
        xp = 0;
    }

    protected Figure(String name, int ID) {
        super(name, ID);
    }
}
