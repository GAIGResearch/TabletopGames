package games.descent2e.pcg;

import java.util.List;

public class FitnessFunction {
    public static final int W_CONNECTED = 1;
    public static final int W_GEOMETRY = 1;
    public static final int W_REPEATS = 1;
    public static final int W_SPAWNING = 1;
    public static final int W_CONSISTENCY = 1;
    public static final int W_SIZE = 1;
    public static final int W_GROUP = 1;
    public static final int W_HEALTH = 1;
    public static final int W_COMPLEXITY = 1;
    public static final int W_RULES = 1;

    public static final int IDEAL_SIZE = 166;
    public static final int IDEAL_GROUP = 5;
    public static final float IDEAL_HEALTH = 5.872f;
    public static final int IDEAL_COMPLEXITY = 1;
    public static final int IDEAL_RULES = 1;

    public static float fitness(List<Float> scores) {
        float fitness = 0f;

        fitness += W_CONNECTED * (1 / scores.get(0));
        fitness += W_GEOMETRY * scores.get(1);
        fitness += W_REPEATS * scores.get(2);
        fitness += W_SPAWNING * scores.get(3);
        fitness += W_CONSISTENCY * scores.get(4);

        fitness += W_SIZE * (1f - (Math.abs(IDEAL_SIZE - scores.get(5)) / IDEAL_SIZE));
        fitness += W_GROUP * (1f - (Math.abs(IDEAL_GROUP - scores.get(6)) / IDEAL_GROUP));
        fitness += W_HEALTH * (1f - (Math.abs(IDEAL_HEALTH - scores.get(7)) / IDEAL_HEALTH));

        fitness += W_COMPLEXITY * (1f - (Math.abs(IDEAL_COMPLEXITY - scores.get(8)) / IDEAL_COMPLEXITY));
        fitness += W_RULES * (1f - (Math.abs(IDEAL_RULES - scores.get(9)) / IDEAL_RULES));

        return fitness;
    }
}
