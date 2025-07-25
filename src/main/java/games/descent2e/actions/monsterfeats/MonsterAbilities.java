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

import static games.descent2e.DescentHelper.*;

public class MonsterAbilities {

    public enum MonsterAbility {
        HOWL,
        GRAB,
        HEAL,
        THROW,
        AIR,
        EARTH,
        FIRE,
        WATER,
        FIREBREATH;
    }

    public enum MonsterPassive {
        SCAMPER,
        COWARDLY,
        NIGHTSTALKER,
        SHAMBLING,
        WEB,
        REACH,
        FLAIL,
        SHADOW,
        AIR;
    }

    public static ArrayList<AbstractAction> getMonsterActions(DescentGameState dgs)
    {
        Figure f = dgs.getActingFigure();
        if (!(f instanceof Monster)) return new ArrayList<>();
        else {
            Monster actingFigure = (Monster) dgs.getActingFigure();
            ArrayList<AbstractAction> actions = new ArrayList<>();
            if (actingFigure.getActions().isEmpty()) return actions;

            List<Hero> heroes = dgs.getHeroes();

            for (MonsterAbility action : actingFigure.getActions()) {

                List<Integer> targets = new ArrayList<>();

                switch (action) {
                    case HOWL:

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

                    case GRAB:

                        targets = getMeleeTargets(dgs, f, false);

                        if (!targets.isEmpty()) {
                            for (Integer target : targets) {
                                DescentAction grab = new Grab(actingFigure.getComponentID(), target);
                                if (grab.canExecute(dgs))
                                    actions.add(grab);
                            }
                        }

                        break;

                    case HEAL:

                        int range = 3;
                        for (List<Monster> monsters : dgs.getMonsters()) {
                            for (Monster monster : monsters) {
                                if (monster.getComponentID() == actingFigure.getComponentID() || checkAllSpaces(dgs, actingFigure, monster, 3, false)) {
                                    DescentAction heal = new Heal(monster.getComponentID(), range, true);
                                    if (heal.canExecute(dgs))
                                        actions.add(heal);
                                }
                            }
                        }

                        break;

                    case THROW:

                        targets = getMeleeTargets(dgs, f, false);

                        if (!targets.isEmpty()) {
                            for (Integer target : targets) {
                                DescentAction throwAttack = new Throw(actingFigure.getComponentID(), target);
                                if (throwAttack.canExecute(dgs))
                                    actions.add(throwAttack);
                            }
                        }

                        break;

                    case AIR:

                        DescentAction air = new Air(actingFigure.getComponentID());
                        if (air.canExecute(dgs))
                            actions.add(air);

                        break;

                    case EARTH:

                        targets = getAdjacentTargets(dgs, f, false);
                        if (!targets.isEmpty()) {
                            DescentAction earth = new Earth(f.getComponentID(), targets);
                            if (earth.canExecute(dgs))
                                actions.add(earth);
                        }

                        break;

                    case FIRE:

                        // Fire explicitly targets all figures - not just enemy Heroes
                        // So have fun with Friendly Fire hitting your own Monsters!
                        targets = getAdjacentTargets(dgs, f, true);
                        if (!targets.isEmpty()) {
                            DescentAction fire = new Fire(f.getComponentID(), targets);
                            if (fire.canExecute(dgs))
                                actions.add(fire);
                        }

                        break;

                    case WATER:

                        targets = getAdjacentTargets(dgs, f, false);
                        if (!targets.isEmpty()) {
                            DescentAction water = new Water(f.getComponentID(), targets);
                            if (water.canExecute(dgs))
                                actions.add(water);
                        }

                        break;

                    default:
                        break;
                }
            }
            return actions;
        }
    }
}
