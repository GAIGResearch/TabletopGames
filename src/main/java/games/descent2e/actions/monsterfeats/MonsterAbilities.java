package games.descent2e.actions.monsterfeats;

import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.GridBoard;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.archetypeskills.Heal;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import utilities.Pair;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;

import static games.descent2e.DescentHelper.getAttackingTiles;
import static games.descent2e.DescentHelper.inRange;

public class MonsterAbilities {

    public enum MonsterAbility {
        HOWL,
        GRAB,
        HEAL,
        THROW,
        AIR,
        EARTH,
        FIRE,
        WATER;
    }

    public enum MonsterPassive {
        SCAMPER,
        COWARDLY,
        NIGHTSTALKER,
        SHAMBLING,
        WEB,
        REACH,
        SHADOW;
    }

    public static ArrayList<AbstractAction> getMonsterActions(DescentGameState dgs)
    {
        Figure f = dgs.getActingFigure();
        if (!(f instanceof Monster)) return new ArrayList<>();
        else {
            Monster actingFigure = (Monster) dgs.getActingFigure();
            ArrayList<AbstractAction> actions = new ArrayList<>();
            if (actingFigure.getActions().isEmpty()) return actions;
            for (MonsterAbility action : actingFigure.getActions()) {
                switch (action) {
                    case HOWL:

                        List<Hero> heroes = dgs.getHeroes();
                        List<Integer> targets = new ArrayList<>();

                        Pair<Integer, Integer> size = actingFigure.getSize();
                        List<BoardNode> attackingTiles = new ArrayList<>();

                        Vector2D currentLocation = actingFigure.getPosition();
                        GridBoard board = dgs.getMasterBoard();
                        BoardNode anchorTile = board.getElement(currentLocation);

                        if (size.a > 1 || size.b > 1) {
                            attackingTiles.addAll(getAttackingTiles(actingFigure.getComponentID(), anchorTile, attackingTiles));
                        } else {
                            attackingTiles.add(anchorTile);
                        }

                        for (BoardNode currentTile : attackingTiles) {
                            for (Hero h : heroes) {
                                if (targets.contains(h.getComponentID())) continue;
                                Vector2D other = h.getPosition();
                                if (inRange(((PropertyVector2D) currentTile.getProperty("coordinates")).values, other, 3)) {
                                    targets.add(h.getComponentID());
                                }
                            }
                        }

                        if (!targets.isEmpty()) {
                            DescentAction howl = new Howl(actingFigure.getComponentID(), targets);
                            if (howl.canExecute(dgs))
                                actions.add(howl);
                        }
                        break;
//                case GRAB:
                    /*DescentAction grab = new Grab();
                    if (grab.canExecute(dgs))
                        actions.add(new Grab());
                    break;*/
                    case HEAL:
                        int range = 3;
                        for (List<Monster> monsters : dgs.getMonsters()) {
                            for (Monster monster : monsters) {
                                if (monster.getComponentID() == actingFigure.getComponentID() || inRange(actingFigure.getPosition(), monster.getPosition(), range)) {
                                    DescentAction heal = new Heal(monster.getComponentID(), range, true);
                                    if (heal.canExecute(dgs))
                                        actions.add(heal);
                                }
                            }
                        }
                        break;
                /*case THROW:
                    DescentAction throwAction = new Throw();
                    if (throwAction.canExecute(dgs))
                        actions.add(new Throw());
                    break;
                case AIR:
                    DescentAction air = new Air();
                    if (air.canExecute(dgs))
                        actions.add(new Air());
                    break;
                case EARTH:
                    DescentAction earth = new Earth();
                    if (earth.canExecute(dgs))
                        actions.add(new Earth());
                    break;
                case FIRE:
                    DescentAction fire = new Fire();
                    if (fire.canExecute(dgs))
                        actions.add(new Fire());
                    break;
                case WATER:
                    DescentAction water = new Water();
                    if (water.canExecute(dgs))
                        actions.add(new Water());
                    break;*/
                    default:
                        break;
                }
            }
            return actions;
        }
    }
}
