package games.hanabi;

public enum CardType {
    R1(1,"Red"),
    R2(2,"Red"),
    R3(3,"Red"),
    R4(4,"Red"),
    R5(5,"Red"),
    Y1(1,"Yellow"),
    Y2(2,"Yellow"),
    Y3(3,"Yellow"),
    Y4(4,"Yellow"),
    Y5(5,"Yellow"),
    G1(1,"Green"),
    G2(2,"Green"),
    G3(3,"Green"),
    G4(4,"Green"),
    G5(5,"Green"),
    W1(1,"White"),
    W2(2,"White"),
    W3(3,"White"),
    W4(4,"White"),
    W5(5,"White"),
    B1(1,"Blue"),
    B2(2,"Blue"),
    B3(3,"Blue"),
    B4(4,"Blue"),
    B5(5,"Blue");


    public final int number;
    public final String color;

    CardType(int number, String color) {
        this.number = number;
        this.color = color;

    }
}
