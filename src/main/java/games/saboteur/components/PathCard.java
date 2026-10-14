package games.saboteur.components;

import core.components.grid.SquareCoord;

import java.util.Arrays;
import java.util.Objects;

public class PathCard extends SaboteurCard {
    // whether the path leads out of each side, in SquareCoord's direction order (E, S, W, N)
    final private boolean[] directions;
    final public PathCardType type;
    final boolean hasTreasure;
    final boolean symmetric;
    // the card turned round, made when first asked for, and turned round again back to this
    private PathCard turned;

    public enum PathCardType {
        Edge,
        Path,
        Goal,
        Start,
    }

    public PathCard(PathCardType type, boolean[] direction) {
        this(type, direction, false);
    }

    public PathCard(PathCardType type, boolean[] direction, boolean hasTreasure) {
        super(SaboteurCardType.Path);
        this.type = type;
        this.directions = direction.clone();
        this.hasTreasure = hasTreasure;
        this.symmetric = direction[SquareCoord.E] == direction[SquareCoord.W] && direction[SquareCoord.S] == direction[SquareCoord.N];
    }

    public PathCard(PathCardType type, boolean[] direction, boolean hasTreasure, int componentID) {
        super(SaboteurCardType.Path, componentID);
        this.type = type;
        this.directions = direction.clone();
        this.hasTreasure = hasTreasure;
        this.symmetric = direction[SquareCoord.E] == direction[SquareCoord.W] && direction[SquareCoord.S] == direction[SquareCoord.N];
    }

    /**
     * The card turned round (half a turn): the same card, so with the same component id.
     */
    public PathCard rotated()
    {
        if (turned == null) {
            boolean[] sides = new boolean[directions.length];
            for (int d = 0; d < directions.length; d++)
                sides[(d + 2) % directions.length] = directions[d];
            turned = new PathCard(type, sides, hasTreasure, componentID);
            turned.turned = this;
        }
        return turned;
    }

    /**
     * Whether the path leads out of the side facing the direction.
     */
    public boolean opensTo(int direction) {return directions[direction];}
    public boolean isSymmetric() {return symmetric;}


    public boolean hasTreasure() {
        return hasTreasure;
    }

    @Override
    public String toString()
    {
        return switch (type) {
            case Edge, Path -> type + Arrays.toString(directions);
            case Start -> "S";
            case Goal -> "G" + hasTreasure;
        };
    }

    @Override
    public PathCard copy()
    {
        return this;
    }

    public String getString()
    {
        if(type == PathCard.PathCardType.Path)
        {
            if(Arrays.equals(directions, new boolean[]{true, false, true, false}))
            {
                return "─";
            }
            else if(Arrays.equals(directions, new boolean[]{false, true, false, true}))
            {
                return "│";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, false, false}))
            {
                return "┌";
            }
            else if(Arrays.equals(directions, new boolean[]{false, true, true, false}))
            {
                return "┐";
            }
            else if(Arrays.equals(directions, new boolean[]{true, false, false, true}))
            {
                return "└";
            }
            else if(Arrays.equals(directions, new boolean[]{false, false, true, true}))
            {
                return "┘";
            }
            else if(Arrays.equals(directions, new boolean[]{false, true, true, true}))
            {
                return "┤";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, false, true}))
            {
                return "├";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, true, false}))
            {
                return "┬";
            }
            else if(Arrays.equals(directions, new boolean[]{true, false, true, true}))
            {
                return "┴";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, true, true}))
            {
                return "┼";
            }
        }
        else if(type == PathCard.PathCardType.Edge)
        {
            if(Arrays.equals(directions, new boolean[]{true, false, true, false}))
            {
                return "═";
            }
            else if(Arrays.equals(directions, new boolean[]{false, true, false, true}))
            {
                return "║";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, false, false}))
            {
                return "╔";
            }
            else if(Arrays.equals(directions, new boolean[]{false, true, true, false}))
            {
                return "╗";
            }
            else if(Arrays.equals(directions, new boolean[]{true, false, false, true}))
            {
                return "╚";
            }
            else if(Arrays.equals(directions, new boolean[]{false, false, true, true}))
            {
                return "╝";
            }
            else if(Arrays.equals(directions, new boolean[]{false, true, true, true}))
            {
                return "╣";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, false, true}))
            {
                return "╠";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, true, false}))
            {
                return "╦";
            }
            else if(Arrays.equals(directions, new boolean[]{true, false, true, true}))
            {
                return "╩";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, true, true}))
            {
                return "╬";
            }
            else if(Arrays.equals(directions, new boolean[]{true, false, false, false}))
            {
                return "<";
            }
            else if(Arrays.equals(directions, new boolean[]{false, false, true, false}))
            {
                return ">";
            }
            else if(Arrays.equals(directions, new boolean[]{false, false, false, true}))
            {
                return "v";
            }
            else if(Arrays.equals(directions, new boolean[]{false, true, false, false}))
            {
                return "^";
            }
        } else if (type == PathCard.PathCardType.Goal) {
            return "G";
        } else if (type == PathCard.PathCardType.Start) {
            return "S";

        }
        return " ";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PathCard pathCard)) return false;
        if (!super.equals(o)) return false;
        return hasTreasure == pathCard.hasTreasure && Arrays.equals(directions, pathCard.directions) && type == pathCard.type;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), type, hasTreasure);
        result = 31 * result + Arrays.hashCode(directions);
        return result;
    }
}
