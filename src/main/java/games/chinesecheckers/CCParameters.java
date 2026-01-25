package games.chinesecheckers;

import core.AbstractParameters;
import games.chinesecheckers.components.Peg;
import gametemplate.GTParameters;

import java.util.*;

public class CCParameters extends AbstractParameters {

    public CCParameters() {
        setTimeoutRounds(200);
    }

    // These are the colours of the base/target nodes for each player (where they need to get to)
    // To find the starting nodes, look at the colour from boardOpposites
    public Map<Peg.Colour, int[]> colourIndices = new HashMap<>();
    {
        colourIndices.put(Peg.Colour.red, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        colourIndices.put(Peg.Colour.purple, new int[]{111, 112, 113, 114, 115, 116, 117, 118, 119, 120});
        colourIndices.put(Peg.Colour.orange, new int[]{65, 75, 76, 86, 87, 88, 98, 99, 100, 101});
        colourIndices.put(Peg.Colour.green, new int[]{10, 11, 12, 13, 23, 24, 25, 35, 36, 46});
        colourIndices.put(Peg.Colour.blue, new int[]{19, 20, 21, 22, 32, 33, 34, 44, 45, 55});
        colourIndices.put(Peg.Colour.yellow, new int[]{74, 84, 85, 95, 96, 97, 107, 108, 109, 110});
    }

    public Map<Peg.Colour, Peg.Colour> boardOpposites = new HashMap<>();

    {
        boardOpposites.put(Peg.Colour.red, Peg.Colour.purple);
        boardOpposites.put(Peg.Colour.purple, Peg.Colour.red);
        boardOpposites.put(Peg.Colour.yellow, Peg.Colour.green);
        boardOpposites.put(Peg.Colour.green, Peg.Colour.yellow);
        boardOpposites.put(Peg.Colour.orange, Peg.Colour.blue);
        boardOpposites.put(Peg.Colour.blue, Peg.Colour.orange);
    }

    public List<Peg.Colour[]> playerColours = Arrays.asList(
            new Peg.Colour[0], new Peg.Colour[0],
            new Peg.Colour[]{Peg.Colour.purple, Peg.Colour.red},
            new Peg.Colour[]{Peg.Colour.purple, Peg.Colour.yellow, Peg.Colour.orange},
            new Peg.Colour[]{Peg.Colour.purple, Peg.Colour.yellow, Peg.Colour.red, Peg.Colour.green},
            new Peg.Colour[0],
            new Peg.Colour[]{Peg.Colour.purple, Peg.Colour.red, Peg.Colour.blue, Peg.Colour.yellow, Peg.Colour.orange, Peg.Colour.green}
    );

    @Override
    protected AbstractParameters _copy() {
        return new CCParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof CCParameters;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
