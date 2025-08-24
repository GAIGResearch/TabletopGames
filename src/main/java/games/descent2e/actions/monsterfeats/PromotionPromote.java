package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.properties.PropertyStringArray;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.Surge;
import games.descent2e.actions.attack.SurgeAttackAction;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Pair;
import utilities.Vector2D;

import java.util.*;

public class PromotionPromote extends DescentAction {

    int minionID;
    int spligID;
    public PromotionPromote(int minionID, int spligID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.minionID = minionID;
        this.spligID = spligID;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Figure minion = (Figure) gs.getComponentById(minionID);
        Figure splig = (Figure) gs.getComponentById(spligID);
        minion.addActionTaken(toString());
        splig.addActionTaken(toString());

        promote(gs, minion);

        return true;
    }

    private void promote (DescentGameState dgs, Figure minion)
    {
        Vector2D position = minion.getPosition();
        List<String> monsterTypes = dgs.getMonsterGroups();
        String monsterName = minion.getName().split(" minion")[0];
        int index = monsterTypes.indexOf(monsterName);

        int masterCounter = 0;

        List<Monster> monsters = dgs.getMonsters().get(index);

        for (Monster monster : monsters)
        {
            if (monster.getName().contains("master")) masterCounter++;
        }

        Monster newMaster = (Monster) minion;
        newMaster.removeAll();
        minion.setComponentName(monsterName + " master " + (masterCounter + 1));

        int act = dgs.getCurrentQuest().getAct();

        List<String[]> questMonsters = dgs.getCurrentQuest().getMonsters();
        HashMap<String, ArrayList<Pair<Figure.Attribute, Integer>>> attributeModifiers = new HashMap<>();

        for (String[] mDef : questMonsters) {
            if (!mDef[0].equals(monsterName)) continue;
            if (mDef.length < 2) continue; // No attributes defined for this monster
            String mod = mDef[2];
            String[] modifiers = mod.split(";");
            for (String modifier : modifiers) {
                String who = modifier.split(":")[0];
                String attribute = modifier.split(":")[1];
                String howMuch = modifier.split(":")[2];
                int amount = Integer.parseInt(howMuch);

                if (!attributeModifiers.containsKey(who)) {
                    attributeModifiers.put(who, new ArrayList<>());
                }
                attributeModifiers.get(who).add(new Pair<>(Figure.Attribute.valueOf(attribute), amount));
            }
        }

        Map<String, Monster> monsterDef = dgs.getData().findMonster(monsterName);
        Monster master = monsterDef.get(act + "-master").copyNewID();

        newMaster.setProperties(monsterDef.get(act + "-master").getProperties());

        PropertyStringArray passives = (PropertyStringArray) master.getProperty("passive");
        if (passives != null)
            newMaster.setPassivesAndSurges(passives.getValues());

        PropertyStringArray actions = ((PropertyStringArray) master.getProperty("action"));
        if (actions != null) {
            newMaster.setActions(actions.getValues());
        }

        // Get rid of all the old Surges
        List<DescentAction> oldSurges = new ArrayList<>();
        for (DescentAction ability : newMaster.getAbilities())
        {
            if (ability instanceof SurgeAttackAction) oldSurges.add(ability);
        }
        for (DescentAction oldSurge : oldSurges) {
            newMaster.removeAbility(oldSurge);
        }
        // And add in all the new ones
        for (Surge surge : newMaster.getSurges()) {
            newMaster.addAbility(new SurgeAttackAction(surge, newMaster.getComponentID()));
        }

        newMaster.getAttribute(Figure.Attribute.MovePoints).setMaximum(master.getAttributeMax(Figure.Attribute.MovePoints));
        newMaster.getAttribute(Figure.Attribute.Health).setMaximum(master.getAttributeMax(Figure.Attribute.Health));

        if (attributeModifiers.containsKey("master")) {
            for (Pair<Figure.Attribute, Integer> modifier : attributeModifiers.get("master")) {
                newMaster.getAttribute(modifier.a).setMaximum(master.getAttribute(modifier.a).getMaximum() + modifier.b);
            }
        }
        if (attributeModifiers.containsKey("all")) {
            for (Pair<Figure.Attribute, Integer> modifier : attributeModifiers.get("all")) {
                newMaster.getAttribute(modifier.a).setMaximum(master.getAttribute(modifier.a).getMaximum() + modifier.b);
            }
        }

        newMaster.getAttribute(Figure.Attribute.MovePoints).setToMin();
        newMaster.getAttribute(Figure.Attribute.Health).setToMax();

        newMaster.setPosition(position);
        newMaster.getNActionsExecuted().setToMin();
        newMaster.removeAllConditions();

        return;
    }

    @Override
    public PromotionPromote copy() {
        return new PromotionPromote(minionID, spligID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromotionPromote that)) return false;
        return minionID == that.minionID && spligID == that.spligID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), minionID, spligID);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure minion = (Figure) dgs.getComponentById(minionID);
        Figure splig = (Figure) dgs.getComponentById(spligID);
        if (minion == null) return false;
        if (!(minion instanceof Monster) || !minion.getName().toLowerCase().contains("minion")) {
            return false;
        }
        if (splig == null) return false;
        return splig instanceof Monster && ((Monster) splig).hasAction(MonsterAbilities.MonsterAbility.PROMOTION);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure splig = (Figure) gameState.getComponentById(spligID);
        Figure minion = (Figure) gameState.getComponentById(minionID);
        String spligName = splig.getName().replace("Hero: ", "");
        String minionName = minion.getName().replace("Hero: ", "");
        return "Promotion: " + spligName + " promotes " + minionName + " to Master";
    }

    @Override
    public String toString() {
        return "Promotion: " + spligID + " promotes " + minionID + " to Master";
    }
}
