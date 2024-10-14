package games.conquest.components;

import games.conquest.CQParameters;
import utilities.ImageIO;

import javax.swing.*;

public enum CommandType {
    BattleCry("Battle Cry", 75, 5, false, 200, 200, 1),
    Stoicism("Stoicism", 75, 5, false, 400, 0, 1),
    Regenerate("Regenerate", 150, 4),
    Bombard("Bombard", 200, 2, true, -100, 0, 1),
    WindsOfFate("Winds of Fate", 150, 2),
    Charge("Charge", 75, 5, false, 0, 0, 2),
    Chastise("Chastise", 50, 3, true, 0, 0, 0),
    Vigilance("Vigilance", 50, 3),
    ShieldWall("Shield Wall", 50, 5);

    public final int cost;
    public final int cooldown;
    public final String name;
    public final int health;
    public final int damage;
    public final int speed;
    public final boolean enemy;
    public final ImageIcon icon;

    CommandType(String name, int cost, int cooldown) {
        this(name, cost, cooldown, false, 0, 0, 1);
    }

    CommandType(String name, int cost, int cooldown, boolean enemy, int health, int damage, int speed) {
        this.name = name;
        this.cost = cost;
        this.cooldown = cooldown;
        this.enemy = enemy;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.icon = new ImageIcon(ImageIO.GetInstance().getImage(CQParameters.dataPath + name + ".png"));
    }

    @Override
    public String toString() {
        return name;
    }
}
