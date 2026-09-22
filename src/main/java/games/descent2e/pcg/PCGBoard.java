package games.descent2e.pcg;

import com.google.crypto.tink.subtle.Random;
import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.properties.PropertyInt;
import core.properties.PropertyStringArray;
import games.descent2e.concepts.DescentReward;
import games.descent2e.concepts.GameOverCondition;
import games.descent2e.concepts.Quest;
import utilities.Pair;

import java.util.*;

import static core.CoreConstants.nodeHash;
import static core.CoreConstants.spaceHash;
import static games.descent2e.pcg.GenerateBoards.getTileByName;
import static games.descent2e.pcg.GenerateBoards.tiles;

public class PCGBoard {
    String name;
    HashSet<String> monsterTraits = new HashSet<>();
    List<Pair<String, String>> monsters = new ArrayList<>();
    String heroStartingPosition;
    int act = 1;
    int startingXP = 0;
    int gold = 0;

    List<GameOverCondition> gameOver = new ArrayList<>();
    List<DescentReward> commonRewards = new ArrayList<>();
    List<DescentReward> heroRewards = new ArrayList<>();
    List<DescentReward> overlordRewards = new ArrayList<>();

    List<PCGNode> board = new ArrayList<>();

    public PCGBoard() {
    }

    public PCGBoard (Quest quest, GraphBoard board) {
        name = quest.getName();
        monsterTraits.addAll(quest.getMonsterTraits());
        for (String[] monster : quest.getMonsters()) {
            monsters.add(new Pair<>(monster[0], monster[1]));
        }
        heroStartingPosition = quest.getStartingTile();
        act = quest.getAct();
        startingXP = quest.getStartingXP();
        gold = quest.getGold();
        gameOver.addAll(quest.getGameOverConditions());
        commonRewards.addAll(quest.getCommonRewards());
        heroRewards.addAll(quest.getHeroRewards());
        overlordRewards.addAll(quest.getOverlordRewards());

        int id = 0;

        for (BoardNode node : board.getBoardNodes()) {
            id++;
            String n = node.getComponentName();
            int orientation = ((PropertyInt) node.getProperty("orientation")).value;
            String[] neighbours = ((PropertyStringArray) node.getProperty("neighbours")).getValues();
            String[] connections = ((PropertyStringArray) node.getProperty("connections")).getValues();
            List<Connection> connects = new ArrayList<>();
            HashMap<Connection, String> neighbourhood = new HashMap<>();
            for (int i = 0; i < connections.length; i++) {
                Connection c = getConnection(connections[i]);
                connects.add(c);
                neighbourhood.put(c, neighbours[i]);
            }

            GridBoard original = getTileByName(n);
            int maxConnections = ((PropertyInt) original.getProperty(nodeHash)).value;
            int size = ((PropertyInt) original.getProperty(spaceHash)).value;

            PCGNode tile = new PCGNode(n, connects, neighbourhood, orientation, maxConnections, size, id);
            this.board.add(tile);
        }
    }

    public PCGBoard copy() {
        PCGBoard copy = new PCGBoard();
        copy.name = name;
        copy.monsterTraits.addAll(monsterTraits);
        for (Pair<String, String> monster : monsters) {
            copy.monsters.add(new Pair<>(monster.a, monster.b));
        }
        copy.heroStartingPosition = heroStartingPosition;
        copy.act = act;
        copy.startingXP = startingXP;
        copy.gold = gold;

        copy.gameOver.addAll(gameOver);
        copy.commonRewards.addAll(commonRewards);
        copy.heroRewards.addAll(heroRewards);
        copy.overlordRewards.addAll(overlordRewards);

        for (PCGNode node : board) {
            copy.board.add(node.copy());
        }
        return copy;
    }

    public Connection getConnection(String connection) {
        switch (connection.split("-")[0]) {
            case "N" -> {
                return Connection.NORTH;
            }
            case "E" -> {
                return Connection.EAST;
            }
            case "S" -> {
                return Connection.SOUTH;
            }
            case "W" -> {
                return Connection.WEST;
            }
            default -> {
                return null;
            }
        }
    }

    public static PCGBoard createOffspring(PCGBoard questTemplate, PCGBoard boardTemplate, int id) {

        PCGBoard offspring = new PCGBoard();

        offspring.name = "PCG-" + id;

        offspring.monsterTraits.addAll(questTemplate.monsterTraits);
        for (Pair<String, String> monster : questTemplate.monsters) {
            offspring.monsters.add(new Pair<>(monster.a, monster.b));
        }
        offspring.heroStartingPosition = offspring.heroStartingPosition;
        offspring.act = questTemplate.act;
        offspring.startingXP = questTemplate.startingXP;
        offspring.gold = questTemplate.gold;

        offspring.gameOver.addAll(questTemplate.gameOver);
        offspring.commonRewards.addAll(questTemplate.commonRewards);
        offspring.heroRewards.addAll(questTemplate.heroRewards);
        offspring.overlordRewards.addAll(questTemplate.overlordRewards);

        for (PCGNode node : boardTemplate.board) {
            offspring.board.add(node.copy());
        }

        return offspring;
    }
}
