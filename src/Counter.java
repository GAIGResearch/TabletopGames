public class Counter {
    private int count;
    private int minimum;
    private int maximum;

    public Counter(int minimum, int maximum, int initial_value) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.count   = initial_value;
    }

    public Boolean isMinimum()         { return this.count == this.minimum; }
    public Boolean isMaximum()         { return this.count == this.maximum; }
    public int     getCounter()         { return this.count;                 }

    public void    Increment(int value) {
        this.count += value;
        if (this.count > this.maximum) {
            this.count = this.maximum;
        }

    }

    public void    Decrement(int value) {
        this.count -= value;
        if (this.count < this.minimum) {
            this.count = this.minimum;
        }
    }

    public static void main(String[] args) {
        Counter life_counter1   = new Counter(0,10, 10);
        Counter life_counter2   = new Counter(0,5,  5);

        Counter points_counter1 = new Counter(0,100,0);
        Counter points_counter2 = new Counter(0,5, 0);

        System.out.println("Test a life counter (0, 10) starting at 10");
        life_counter1.Decrement(1);
        life_counter1.Decrement(2);
        life_counter1.Decrement(1);
        life_counter1.Increment(2);
        life_counter1.Decrement(1);
        life_counter1.Decrement(1);
        System.out.println("It should be 6: " + life_counter1.getCounter());
        System.out.println("");

        System.out.println("Test when a life counter (0, 5) starting at 5 gets the minimum");
        for (int i=0; i< 10; i++) {
            life_counter2.Decrement(1);
            if (life_counter2.isMinimum()) {
                System.out.println("The counter [" + life_counter2.getCounter() + "] get the minimum");
            } else {
                System.out.println("The counter [" + life_counter2.getCounter() + "] do not get the minimum");
            }
        }
        System.out.println("");

        System.out.println("Test a points counter (0, 100) starting at 0");
        points_counter1.Increment(10);
        points_counter1.Increment(20);
        points_counter1.Decrement(5);
        points_counter1.Increment(25);
        points_counter1.Decrement(10);
        points_counter1.Increment(10);
        System.out.println("It should be 50: " + points_counter1.getCounter());
        System.out.println("");

        System.out.println("Test when a points counter (0, 5) starting at 0 gets the maximum");
        for (int i=0; i< 10; i++) {
            points_counter2.Increment(1);
            if (points_counter2.isMaximum()) {
                System.out.println("The counter [" + points_counter2.getCounter() + "] get the maximum");
            } else {
                System.out.println("The counter [" + points_counter2.getCounter() + "] do not get the maximum");
            }
        }System.out.println("");
    }
}
