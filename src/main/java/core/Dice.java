package core;

import java.util.Random;

public class Dice extends Component {
    private int number_of_sides;

    public Dice() {
        super.type = ComponentType.DICE;
        this.number_of_sides = 0;
    }

    public Dice(int number_of_sides) {
        super.type = ComponentType.DICE;
        this.number_of_sides = number_of_sides;
    }

    public int  getNumberOfSides()                    { return this.number_of_sides;            }
    public void setNumberOfSides(int number_of_sides) { this.number_of_sides = number_of_sides; }

    public Dice copy() {
        return new Dice(this.number_of_sides);
    }

    public int roll() {
        Random r = new Random();
        return r.nextInt(this.number_of_sides) + 1;
    }

    public static void main(String[] args) {
        int  n_times       = 100;
        Dice dice_6_sides  = new Dice(6);
        Dice dice_12_sides = new Dice(12);

        // Test copy method
        Dice dice_8_sides = dice_6_sides.copy();
        dice_8_sides.setNumberOfSides(8);
        System.out.println("A dice with  6 sides has " + dice_6_sides.getNumberOfSides()  + " sides.");
        System.out.println("A dice with  8 sides has " + dice_8_sides.getNumberOfSides()  + " sides.");
        System.out.println("A dice with 12 sides has " + dice_12_sides.getNumberOfSides() + " sides.");

        // Row several times the 6 sides dice
        System.out.println("Row several times a 6 sides dice");
        for (int i=0; i< n_times; i++) {
            System.out.println(dice_6_sides.roll());
        }

        // Row several times the 12 sides dice
        System.out.println("Row several times a 8 sides dice");
        for (int i=0; i< n_times; i++) {
            System.out.println(dice_8_sides.roll());
        }


        // Row several times the 12 sides dice
        System.out.println("Row several times a 12 sides dice");
        for (int i=0; i< n_times; i++) {
            System.out.println(dice_12_sides.roll());
        }
    }
}
