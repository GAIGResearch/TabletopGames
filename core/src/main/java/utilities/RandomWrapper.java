package utilities;

import java.util.Random;

public class RandomWrapper {
    Random rnd;
    long count = 0;

    public RandomWrapper(Random random) {
        rnd = random;
    }

    public int getCalls() {
        return (int) count;
    }

    public int nextInt(int bound) {
        count++;
        return rnd.nextInt(bound);
    }

    public double nextDouble() {
        count++;
        return rnd.nextDouble();
    }

    public Random getRND() {
        return rnd;
    }
}
