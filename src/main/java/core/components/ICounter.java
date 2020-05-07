package core.components;

public interface ICounter {
    ICounter copy();

    Boolean isMinimum();

    Boolean isMaximum();

    int getValue();

    void increment(int value);

    void decrement(int value);

    void setValue(int i);

    String getID();
}
