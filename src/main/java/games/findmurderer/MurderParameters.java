package games.findmurderer;

import core.AbstractParameters;
import core.AbstractPlayer;
import players.simple.RandomPlayer;
import utilities.Distance;
import utilities.Vector2D;

import java.util.Objects;
import java.util.Random;
import java.util.function.BiFunction;

public class MurderParameters extends AbstractParameters {
    public enum Direction {
        Left(-1,0),
        Right(1,0),
        Up(0, -1),
        Down(0,1),
        UpLeft(-1,-1),
        UpRight(1,-1),
        DownRight(1,1),
        DownLeft(-1,1);
        public final int xDiff;
        public final int yDiff;
        Direction(int x, int y) {
            this.xDiff = x;
            this.yDiff = y;
        }
        public int getxDiff() {
            return xDiff;
        }
        public int getyDiff() {
            return yDiff;
        }
        public boolean equals(int x, int y) {
            return x == xDiff && y == yDiff;
        }

        public static Direction getDirection(int x, int y) {
            for (Direction d: values()) {
                if (d.equals(x, y)) return d;
            }
            return null;
        }
    }

    public int gridWidth = 10;  // Width of grid world
    public int gridHeight = 10;  // Height of grid world
    public double percPeopleOnGrid = 0.5;  // Percentage of the world that is filled randomly with people
    public double percCivilianDeadWinKiller = 0.8;  // Percentage of the people in the world that have to be dead in order for the killer to win
    public int killerMaxRange = 1;  // Killer can only kill people as far as this away from them, and loses when no more people are left in range
    public int maxTicks = 100;  // Maximum number of ticks
    public int detectiveVisionRange = 5;  // Detective will see this many cells around their current focus
    public BiFunction<Vector2D, Vector2D, Double> distanceFunction = Distance::manhattan_distance;  // What distance function to use

    // How do civilians behave? Random default.
    public AbstractPlayer civilianPolicy;

    public MurderParameters(long seed) {
        super(seed);
        civilianPolicy = new RandomPlayer(new Random(getRandomSeed()));
    }

    @Override
    protected MurderParameters _copy() {
        MurderParameters mp = new MurderParameters(0);
        mp.gridHeight = gridHeight;
        mp.gridWidth = gridWidth;
        mp.percCivilianDeadWinKiller = percCivilianDeadWinKiller;
        mp.percPeopleOnGrid = percPeopleOnGrid;
        mp.maxTicks = maxTicks;
        mp.distanceFunction = distanceFunction;
        return mp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MurderParameters)) return false;
        if (!super.equals(o)) return false;
        MurderParameters that = (MurderParameters) o;
        return gridWidth == that.gridWidth && gridHeight == that.gridHeight && Double.compare(that.percPeopleOnGrid, percPeopleOnGrid) == 0 && Double.compare(that.percCivilianDeadWinKiller, percCivilianDeadWinKiller) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridWidth, gridHeight, percPeopleOnGrid, percCivilianDeadWinKiller);
    }
}
