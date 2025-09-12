package games.powergrid.components;


public class PowerGridCardSmokeTest {

    public static void main(String[] args) {


        PowerGridGraphBoard board = PowerGridGraphBoard.northAmerica();

        // --- Print cities ---
        System.out.println("Cities on board:");
        for (PowerGridCity c : board.cities()) {
            System.out.printf("  %d: %s (region %d, double=%s)%n",
                    c.getComponentID(), c.name(), c.region(), c.isDouble());
        }

        // --- Print adjacency ---
        System.out.println("\nConnections:");
        for (PowerGridCity c : board.cities()) {
            System.out.print(c.name() + " -> ");
            board.edgesFrom(c.getComponentID())
                 .forEach(e -> System.out.print(board.city(e.to).name() + "(" + e.cost + ") "));
            System.out.println();
        }
    }
}
