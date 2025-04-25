package games.saboteur.components;

import java.util.Arrays;
import java.util.Objects;

public class PathCard extends SaboteurCard {
    final private boolean[] directions;  // The array is final - but the contents are not!
    final public PathCardType type;
    final boolean hasTreasure;

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
        this.directions = direction;
        this.hasTreasure = hasTreasure;
    }

    public PathCard(PathCardType type, boolean[] direction, boolean hasTreasure, int componentID) {
        super(SaboteurCardType.Path, componentID);
        this.type = type;
        this.directions = direction;
        this.hasTreasure = hasTreasure;
    }

    public void rotate()
    {
        //up down
        swap(0,1);
        //left right
        swap(2,3);
    }

    private void swap(int indexA, int indexB)
    {
        boolean temp = directions[indexA];
        directions[indexA] = directions[indexB];
        directions[indexB] = temp;
    }

    public boolean[] getDirections() {return directions;}
    public int getOppositeDirection(int direction)
    {
        return switch (direction) {
            case 0 -> 1;
            case 1 -> 0;
            case 2 -> 3;
            case 3 -> 2;
            default -> -1;
        };
    }


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
        return new PathCard(type, directions.clone(), hasTreasure, componentID);
    }

    public String getString()
    {
        if(type == PathCard.PathCardType.Path)
        {
            if(Arrays.equals(directions, new boolean[]{false, false, true, true}))
            {
                return "─";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, false, false}))
            {
                return "│";
            }
            else if(Arrays.equals(directions, new boolean[]{false, true, false, true}))
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
            else if(Arrays.equals(directions, new boolean[]{true, false, true, false}))
            {
                return "┘";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, true, false}))
            {
                return "┤";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, false, true}))
            {
                return "├";
            }
            else if(Arrays.equals(directions, new boolean[]{false, true, true, true}))
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
            if(Arrays.equals(directions, new boolean[]{false, false, true, true}))
            {
                return "═";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, false, false}))
            {
                return "║";
            }
            else if(Arrays.equals(directions, new boolean[]{false, true, false, true}))
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
            else if(Arrays.equals(directions, new boolean[]{true, false, true, false}))
            {
                return "╝";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, true, false}))
            {
                return "╣";
            }
            else if(Arrays.equals(directions, new boolean[]{true, true, false, true}))
            {
                return "╠";
            }
            else if(Arrays.equals(directions, new boolean[]{false, true, true, true}))
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
            else if(Arrays.equals(directions, new boolean[]{false, false, false, true}))
            {
                return "<";
            }
            else if(Arrays.equals(directions, new boolean[]{false, false, true, false}))
            {
                return ">";
            }
            else if(Arrays.equals(directions, new boolean[]{true, false, false, false}))
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
