package games.descent2e;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import static games.descent2e.DescentHelper.*;
import static games.descent2e.components.Figure.Attribute.*;

import utilities.Utils;
import utilities.Vector2D;
import java.util.ArrayList;
import java.util.List;

public class DescentHeuristic extends TunableParameters implements IStateHeuristic {

    // The total HP of the Heroes   - Beneficial to the Heroes
    double FACTOR_HERO_HP = 0.8;

    // The number of Heroes defeated - Beneficial to the Overlord
    double FACTOR_HERO_DEFEATED = 0.7;

    // The total HP of the monsters - Beneficial to the Overlord
    double FACTOR_MONSTERS_HP = 0.8;

    // The number of monsters defeated - Beneficial to the Heroes
    double FACTOR_MONSTERS_DEFEATED = 0.7;

    // The Overlord's fatigue value - Beneficial to the Overlord
    double FACTOR_OVERLORD_FATIGUE = 0.7;

    // How close the Overlord is to increasing their fatigue - Beneficial to the Overlord
    double FACTOR_OVERLORD_THREAT = 0.2;

    // How close the Heroes are to winning - Beneficial to the Heroes
    double FACTOR_HEROES_THREAT = 0.2;

    // Penalise the current player if they End Turn poorly - Penalises both Heroes and Overlord
    double FACTOR_DONE_NOTHING = 0.0;

    public DescentHeuristic() {
        addTunableParameter("FACTOR_HERO_HP", FACTOR_HERO_HP);
        addTunableParameter("FACTOR_HERO_DEFEATED", FACTOR_HERO_DEFEATED);
        addTunableParameter("FACTOR_MONSTERS_HP", FACTOR_MONSTERS_HP);
        addTunableParameter("FACTOR_MONSTERS_DEFEATED", FACTOR_MONSTERS_DEFEATED);
        addTunableParameter("FACTOR_OVERLORD_FATIGUE", FACTOR_OVERLORD_FATIGUE);
        addTunableParameter("FACTOR_OVERLORD_THREAT", FACTOR_OVERLORD_THREAT);
        addTunableParameter("FACTOR_HEROES_THREAT", FACTOR_HEROES_THREAT);
        addTunableParameter("FACTOR_DONE_NOTHING", FACTOR_DONE_NOTHING);
    }


    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {

        DescentGameState dgs = (DescentGameState) gs;
        DescentParameters dp = (DescentParameters) gs.getGameParameters();
        CoreConstants.GameResult playerResult = gs.getPlayerResults()[playerId];

        if (!gs.isNotTerminal()) {
            return playerResult.value;
        }

        double retValue = 0.0;

        List<Double> heuristics = getHeuristics(dgs, playerId);

        for(double h : heuristics)
        {
            retValue += h;
        }

        // As playerResult.value is 1.0 for a win or -1.0 for a loss
        // we should clamp the return value to just below to ensure that a win or loss is more noticeable
        return Utils.clamp(retValue, -0.99, 0.99);
    }

    public List<Double> getHeuristics(DescentGameState dgs, int playerId)
    {
        List<Double> heuristics = new ArrayList<>();

        String questName = dgs.getCurrentQuest().getName();

        // Some heuristics will be beneficial to the overlord and detrimental to the heroes
        // Likewise, some will be beneficial to the heroes and detrimental to the overlord
        // We need to flip the sign of the heuristic if the player is the overlord where relevant
        Figure overlord = dgs.getOverlord();
        double isOverlord = playerId == dgs.overlordPlayer ? -1.0 : 1.0;

        heuristics.add(FACTOR_HERO_HP * isOverlord * (getHeroesHP(dgs) / getHeroesMaxHP(dgs)));
        heuristics.add(-1 * FACTOR_HERO_DEFEATED * isOverlord * (getHeroesDefeated(dgs) / dgs.heroes.size()));

        heuristics.add(-1 * FACTOR_OVERLORD_FATIGUE * isOverlord * ((double) overlord.getAttributeValue(Fatigue) / (double) overlord.getAttributeMax(Fatigue)));
        heuristics.add(-1 * FACTOR_OVERLORD_THREAT * isOverlord * (getOverlordThreat(dgs, questName)));


        switch (questName)
        {
            case "Acolyte of Saradyn":
                // We only care about the Barghests, as their defeat is the only way for the Heroes to win, as the Goblin Archers infinitely respawn
                // The Barghests are the second monsters in the list, i.e. index 1
                heuristics.add(-1 * FACTOR_MONSTERS_HP * isOverlord * (getMonstersHP(dgs, 1) / getMonstersMaxHP(dgs, 1)));
                heuristics.add(FACTOR_MONSTERS_DEFEATED * isOverlord * getMonstersDefeated(dgs, 1) / dgs.monstersOriginal.get(1).size());
                break;
            default:
                heuristics.add(FACTOR_MONSTERS_HP * isOverlord * (getMonstersHP(dgs, -1) / getMonstersMaxHP(dgs, -1)) / dgs.monsters.size());
                heuristics.add(FACTOR_MONSTERS_DEFEATED * isOverlord * (getMonstersDefeated(dgs, 0) / dgs.monstersOriginal.get(0).size()));
                break;
        }

        heuristics.add(FACTOR_HEROES_THREAT * isOverlord * (getHeroesThreat(dgs, questName) / dgs.heroes.size()));

        // Penalise the current player if they immediately End Turn
        // This is to encourage the AI to do something, even if it's just moving a figure
        // We penalise the player if they have not executed any actions, or if they have MovePoints remaining and have not moved
        if (FACTOR_DONE_NOTHING > 0.0) {
            Figure actingFigure = dgs.getActingFigure();
            if (actingFigure.getNActionsExecuted().isMinimum() || (actingFigure.getAttribute(MovePoints).getValue() != 0 && !actingFigure.hasMoved())) {
                heuristics.add(-1.0 * FACTOR_DONE_NOTHING);
            }
        }
        // Rounds the Heuristics to 6 decimal places
        heuristics.replaceAll(aDouble -> (double) Math.round(aDouble * 1000000d) / 1000000d);

        return heuristics;
    }

    public List<Double> getHeuristicParameters()
    {
        List<Double> params = new ArrayList<>();
        params.add(FACTOR_HERO_HP);
        params.add(FACTOR_HERO_DEFEATED);
        params.add(FACTOR_MONSTERS_HP);
        params.add(FACTOR_MONSTERS_DEFEATED);
        params.add(FACTOR_OVERLORD_FATIGUE);
        params.add(FACTOR_OVERLORD_THREAT);
        params.add(FACTOR_HEROES_THREAT);
        params.add(FACTOR_DONE_NOTHING);
        return params;
    }

    private double getHeroesHP(DescentGameState dgs) {
        return dgs.heroes.stream().mapToDouble(h -> h.getAttributeValue(Health)).sum();
    }
    private double getHeroesMaxHP(DescentGameState dgs) {
        return dgs.heroes.stream().mapToDouble(h -> h.getAttributeMax(Health)).sum();
    }
    private double getHeroesDefeated(DescentGameState dgs) {
        return dgs.heroes.stream().filter(Hero::isDefeated).count();
    }

    private double getMonstersHP(DescentGameState dgs, int index) {
        if (index < 0 || index >= dgs.monsters.size()) return getAllMonstersHP(dgs);
        return dgs.monsters.get(index).stream().mapToDouble(m -> m.getAttributeValue(Health)).sum();
    }
    private double getMonstersMaxHP(DescentGameState dgs, int index) {
        if (index < 0 || index >= dgs.monstersOriginal.size()) return getAllMonstersMaxHP(dgs);
        return dgs.monstersOriginal.get(index).stream().mapToDouble(m -> m.getAttributeMax(Health)).sum();
    }
    private double getAllMonstersHP(DescentGameState dgs) {
        double retVal = 0.0;
        for (int i = 0; i < dgs.monsters.size(); i++) {
            retVal += getMonstersHP(dgs, i);
        }
        return retVal;
    }

    private double getAllMonstersMaxHP(DescentGameState dgs) {
        double retVal = 0.0;
        for (int i = 0; i < dgs.monstersOriginal.size(); i++) {
            retVal += getMonstersMaxHP(dgs, i);
        }
        return retVal;
    }

    private double getMonstersDefeated(DescentGameState dgs, int index) {
        // Subtract the number of monsters in the original list from the number in the current list
        return dgs.monstersOriginal.get(index).size() - dgs.monsters.get(index).size();
    }
    private double getOverlordThreat(DescentGameState dgs, String questName) {
        double retVal = 0.0;
        switch (questName)
        {
            case "Acolyte of Saradyn":
                // We need to check how many Goblin Archers (index 0) are within the scorezone of 9A
                // or in the neighbouring zones of 21A, entrance1A and 8A
                String scoreZone = "9A";
                List<Vector2D> tileCoords = new ArrayList<>(dgs.gridReferences.get(scoreZone).keySet());

                // If any Goblins are slain (and thus respawning), the Overlord should be penalised
                // This rewards the Heroes for killing Goblins
                retVal = -1.0 * getMonstersDefeated(dgs, 0) / dgs.monstersOriginal.get(0).size();

                List<Monster> monsters = dgs.getMonsters().get(0);
                for (Monster m : monsters) {
                    int closest = 0;
                    double distance = 10000.0;
                    Vector2D position = m.getPosition();
                    if (tileCoords.contains(position)) distance = 0.0;
                    for (int i = 0; i < tileCoords.size(); i++) {
                        if (distance <= 1.0) break;
                        double dist = getDistance(position, tileCoords.get(i));
                        if (dist < distance) {
                            distance = dist;
                            closest = i;
                        }
                    }
                    if (distance > 0.0) {
                        int range = bfsLee(dgs, position, tileCoords.get(closest));
                        int movement = Math.min(m.getAttribute(MovePoints).getValue(), m.getAttributeMax(MovePoints));
                        double potential = Math.max(0.0, range - (movement / 10.0));
                        double d = 1.0 - (potential / 5.0);
                        retVal += ((double) Math.round(d * 1000000d) / 1000000d);
                        if (!hasLineOfSight(dgs, position, tileCoords.get(closest))) {
                            retVal -= 0.01;
                        }
                    }
                    else {
                        retVal += 1.0;
                    }
                }

                retVal = retVal / dgs.getOriginalMonsters().get(0).size();

                break;
            default:
                break;
        }
        return retVal;
    }

    private double getHeroesThreat(DescentGameState dgs, String questName) {
        double retVal = 0.0;
        switch (questName)
        {
            case "Acolyte of Saradyn":
                // We need to check how far away the Heroes are from the Barghests
                // The closer the Heroes are, the better
                List<Monster> barghests = dgs.monsters.get(1);
                // If all the Barghests are slain, the Heroes win, so there is no further need to check
                if (barghests.isEmpty()) return 1.0;
                int closest = 0;
                for (Hero h : dgs.heroes) {
                    // Heroes that are defeated should not be counted
                    // This rewards the Overlord for defeating Heroes
                    /*if (h.isDefeated())
                    {
                        continue;
                    }*/
                    DescentTypes.AttackType attackType = getAttackType(h);
                    int minRange = attackType == DescentTypes.AttackType.MELEE ? 1 : 2;
                    double distance = 10000.0;
                    Vector2D position = h.getPosition();
                    for (int i = 0; i < barghests.size(); i++) {
                        if (distance <= 1.0) break;
                        Monster m = barghests.get(i);
                        double dist = getDistance(position, m.getPosition());
                        if (dist < distance) {
                            distance = dist;
                            closest = i;
                        }
                    }
                    Vector2D target = barghests.get(closest).getPosition();
                    int range = bfsLee(dgs, position, target);
                    if (range <= minRange && hasLineOfSight(dgs, position, target))
                    {
                        retVal += 1.0;
                    }
                    else {
                        int movement = Math.min(h.getAttribute(MovePoints).getValue(), h.getAttributeMax(MovePoints));
                        double potential = Math.max(0.0, range - (movement / 4.0));
                        double d = 1.0 - (potential / 5.0);
                        retVal += ((double) Math.round(d * 1000000d) / 1000000d);
                    }
                }
                retVal = retVal / dgs.heroes.size();
                break;
            default:
                break;
        }
        return retVal;
    }


    @Override
    protected DescentHeuristic _copy() {
        DescentHeuristic retVal = new DescentHeuristic();
        retVal.FACTOR_HERO_HP = FACTOR_HERO_HP;
        retVal.FACTOR_HERO_DEFEATED = FACTOR_HERO_DEFEATED;
        retVal.FACTOR_MONSTERS_HP = FACTOR_MONSTERS_HP;
        retVal.FACTOR_MONSTERS_DEFEATED = FACTOR_MONSTERS_DEFEATED;
        retVal.FACTOR_OVERLORD_FATIGUE = FACTOR_OVERLORD_FATIGUE;
        retVal.FACTOR_OVERLORD_THREAT = FACTOR_OVERLORD_THREAT;
        retVal.FACTOR_DONE_NOTHING = FACTOR_DONE_NOTHING;
        return retVal;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DescentHeuristic)
        {
            DescentHeuristic other = (DescentHeuristic) o;
            return FACTOR_HERO_HP == other.FACTOR_HERO_HP &&
                    FACTOR_HERO_DEFEATED == other.FACTOR_HERO_DEFEATED &&
                    FACTOR_MONSTERS_HP == other.FACTOR_MONSTERS_HP &&
                    FACTOR_MONSTERS_DEFEATED == other.FACTOR_MONSTERS_DEFEATED &&
                    FACTOR_OVERLORD_FATIGUE == other.FACTOR_OVERLORD_FATIGUE &&
                    FACTOR_OVERLORD_THREAT == other.FACTOR_OVERLORD_THREAT &&
                    FACTOR_DONE_NOTHING == other.FACTOR_DONE_NOTHING;
        }
        return false;
    }

    @Override
    public Object instantiate() {
        return this._copy();
    }

    @Override
    public void _reset() {
        FACTOR_HERO_HP = (double) getParameterValue("FACTOR_HERO_HP");
        FACTOR_HERO_DEFEATED = (double) getParameterValue("FACTOR_HERO_DEFEATED");
        FACTOR_MONSTERS_HP = (double) getParameterValue("FACTOR_MONSTERS_HP");
        FACTOR_MONSTERS_DEFEATED = (double) getParameterValue("FACTOR_MONSTERS_DEFEATED");
        FACTOR_OVERLORD_FATIGUE = (double) getParameterValue("FACTOR_OVERLORD_FATIGUE");
        FACTOR_OVERLORD_THREAT = (double) getParameterValue("FACTOR_OVERLORD_THREAT");
        FACTOR_DONE_NOTHING = (double) getParameterValue("FACTOR_DONE_NOTHING");
    }
}
