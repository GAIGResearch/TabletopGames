package core.components;

import java.util.Random;

public interface IDice {
    int  getNumberOfSides();

    void setNumberOfSides(int number_of_sides);

    IDice copy();

    int roll(Random r);
}
