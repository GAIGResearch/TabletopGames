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
        FIREBREATH,
        // --- LIEUTENANT ABILITIES ---
        DOMINION,
        SHADOWBOLT,
        CRYHAVOC,
        SACRIFICE,
        SEDUCE,
        WAIL,
        IGNITE,
        OVERPOWER;
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
        AIR,
        // --- LIEUTENANT PASSIVES ---
        FLY,
        AFTERSHOCK,
        REGENERATION,
        UNMOVABLE;
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

            int range = 0;

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

                        range = 3;
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

                    // --- LIEUTENANT ABILITIES ---

                    // Baron Zachareth
                    case DOMINION:

                        for (Hero h : heroes)
                        {
                            if (hasLineOfSight(dgs, actingFigure.getPosition(), h.getPosition()) && !h.isOffMap()) {
                                DescentAction dominion = new Dominion(actingFigure.getComponentID(), h.getComponentID());
                                if (dominion.canExecute(dgs))
                                    actions.add(dominion);
                            }
                        }

                        break;

                    case SHADOWBOLT:

                        targets = getRangedTargets(dgs, f);
                        if (!targets.isEmpty()) {
                            for (int target : targets) {
                                DescentAction shadowBolt = new ShadowBolt(actingFigure.getComponentID(), target);
                                if (shadowBolt.canExecute(dgs))
                                    actions.add(shadowBolt);
                            }
                        }

                        break;

                    // Belthir
                    case CRYHAVOC:

                        // This counts as an attack, so if he's already attacked this turn, he can't use this
                        if (actingFigure.hasAttacked()) break;

                        // Cry Havoc allows us to move up to our movement speed and attack everyone we pass through
                        // As the final space must be empty, our maximum number of targets is one less than our movement speed
                        range = actingFigure.getAttributeValue(Figure.Attribute.MovePoints) - 1;
                        CryHavoc cryHavoc = new CryHavoc(actingFigure.getComponentID(), range);
                        if (cryHavoc.canExecute(dgs)) {
                            actions.add(cryHavoc);
                        }

                        break;

                    // Lady Eliza Farrow
                    case SACRIFICE:

                        getAdjacentTargets(dgs, f, true).forEach(target -> {
                            DescentAction sacrifice = new Sacrifice(actingFigure.getComponentID(), target);
                            if (sacrifice.canExecute(dgs))
                                actions.add(sacrifice);
                        });

                    case SEDUCE:

                        for (Hero hero : heroes) {
                            if (inRange(actingFigure.getPosition(), hero.getPosition(), 3) && !hero.isOffMap()) {
                                DescentAction seduce = new Seduce(actingFigure.getComponentID(), hero.getComponentID());
                                if (seduce.canExecute(dgs))
                                    actions.add(seduce);
                            }
                        }

                        break;

                    case WAIL:

                        for (Hero h : heroes) {
                            if (inRange(actingFigure.getPosition(), h.getPosition(), 3)) {
                                targets.add(h.getComponentID());
                            }
                        }

                        if (!targets.isEmpty()) {
                            DescentAction wail = new Wail(actingFigure.getComponentID(), targets);
                            if (wail.canExecute(dgs))
                                actions.add(wail);
                        }
                        break;

                    // Lord Merick Fallow

                    case IGNITE:

                        // Don't even bother if he doesn't meet the HP requirement for this, save yourself the time.
                        if (actingFigure.getAttribute(Figure.Attribute.Health).getValue() <= 1) break;

                        // Merick's Ignite explicitly targets all figures - not just enemy Heroes
                        // Another case of Friendly Fire!
                        targets = getAdjacentTargets(dgs, f, true);
                        if (!targets.isEmpty()) {
                            DescentAction ignite = new Ignite(f.getComponentID(), targets);
                            if (ignite.canExecute(dgs))
                                actions.add(ignite);
                        }

                        break;

                    // Sir Alric Farrow
                    case OVERPOWER:
                        // Because Overpower can be interrupted by ordinary Move actions if he already have MovePoints,
                        // Alric's Overpower range is his maximum MovePoints plus his current MovePoints
                        range = f.getAttributeMax(Figure.Attribute.MovePoints) + f.getAttributeValue(Figure.Attribute.MovePoints);
                        for (Hero h : heroes)
                        {
                            if (inRange(actingFigure.getPosition(), h.getPosition(), range) && !h.isOffMap()) {
                                targets.add(h.getComponentID());
                            }
                        }
                        DescentAction overpower = new Overpower(actingFigure.getComponentID(), targets, range);
                        if (overpower.canExecute(dgs))
                            actions.add(overpower);
                        break;

                    default:
                        break;
                }
            }
            return actions;
        }
    }
}
