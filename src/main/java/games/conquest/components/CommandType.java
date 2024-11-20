package games.conquest.components;

import games.conquest.CQGameState;
import games.conquest.CQParameters;
import utilities.ImageIO;
import games.conquest.CQGameState.CQGamePhase;
import static games.conquest.CQGameState.CQGamePhase.*;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum CommandType {
    BattleCry("Battle Cry", 75, 5, MovementPhase, false, 200, 200, 1),
    Stoicism("Stoicism", 75, 5, MovementPhase, false, 400, 0, 1),
    Regenerate("Regenerate", 150, 4, Arrays.asList(MovementPhase, RallyPhase)),
    Bombard("Bombard", 200, 2, Collections.singletonList(MovementPhase), true, -100, 0, 1),
    WindsOfFate("Winds of Fate", 150, 2, null),
    Charge("Charge", 75, 5, MovementPhase, false, 0, 0, 2),
    Chastise("Chastise", 50, 3, MovementPhase, true, 0, 0, 0),
    Vigilance("Vigilance", 50, 3, Collections.singletonList(MovementPhase)),
    ShieldWall("Shield Wall", 50, 5, Collections.singletonList(MovementPhase));

    public final int cost;
    public final int cooldown;
    public final String name;
    public final int health;
    public final int damage;
    public final int speed;
    public final boolean enemy;
    public final ImageIcon icon;
    public final List<CQGamePhase> phases;

    CommandType(String name, int cost, int cooldown, List<CQGamePhase> phases) {
        this(name, cost, cooldown, phases, false, 0, 0, 1);
    }

    CommandType(String name, int cost, int cooldown, CQGamePhase phase, boolean enemy, int health, int damage, int speed) {
        this(name, cost, cooldown, Collections.singletonList(phase), enemy, health, damage, speed);
    }
    CommandType(String name, int cost, int cooldown, List<CQGamePhase> phases, boolean enemy, int health, int damage, int speed) {
        this.name = name;
        this.cost = cost;
        this.cooldown = cooldown;
        this.enemy = enemy;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.icon = new ImageIcon(ImageIO.GetInstance().getImage(CQParameters.dataPath + name + ".png"));
        this.phases = phases;
    }

    @Override
    public String toString() {
        return name;
    }
}
