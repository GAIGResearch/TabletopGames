import java.util.Random;

public class Dice {
    private int number_of_sides;

    public Dice(int number_of_sides) { this.number_of_sides = number_of_sides; }

    public int Row() {
        Random r = new Random();
        return r.nextInt(this.number_of_sides) + 1;
    }

    public static void main(String[] args) {
        Dice dice_6_sides  = new Dice(6);
        Dice dice_12_sides = new Dice(12);
        int  n_times       = 100;

        // Row several times the 6 sides dice
        System.out.println("Row several times a 6 sides dice");
        for (int i=0; i< n_times; i++) {
            System.out.println(dice_6_sides.Row());
        }

        // Row several times the 12 sides dice
        System.out.println("Row several times a 12 sides dice");
        for (int i=0; i< n_times; i++) {
            System.out.println(dice_12_sides.Row());
        }
    }
}
