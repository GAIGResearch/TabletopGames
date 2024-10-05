package games.conquest.components;

public enum TroopType {
    Scout(6, 100, 100, 1, 25, "Scout"),
    FootSoldier(4, 100, 300, 1, 50, "Foot Soldier"),
    Halberdier(4, 200, 200, 2, 75, "Halberdier"),
    Archer(3, 100, 100, 6, 100, "Archer"),
    Mage(4, 200, 100, 4, 100, "Mage"),
    Knight(4, 300, 300, 1, 100, "Knight"),
    Champion(4, 400, 500, 1, 200, "Champion");

    public final int movement;
    public final int damage;
    public final int health;
    public final int range;
    public final int cost;
    public final char troopID;
    public final String troopName;

    TroopType(int movement, int damage, int health, int range, int cost, String troopName) {
        this.movement = movement;
        this.damage = damage;
        this.health = health;
        this.range = range;
        this.cost = cost;
        this.troopName = troopName;
        this.troopID = troopName.charAt(0);
    }
}
