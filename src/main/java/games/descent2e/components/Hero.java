package games.descent2e.components;

import core.CoreConstants;
import core.components.Counter;
import core.components.Deck;
import core.properties.Property;
import core.properties.PropertyString;
import core.properties.PropertyStringArray;
import games.descent2e.DescentGameState;
import games.descent2e.abilities.HeroAbilities;
import games.descent2e.actions.Move;
import games.descent2e.concepts.HeroicFeat;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static games.descent2e.DescentConstants.*;

// TODO: figure out how to do ability/heroic-feat
public class Hero extends Figure {

    Deck<DescentCard> skills;
    Deck<DescentCard> handEquipment;
    DescentCard armor;
    Deck<DescentCard> otherEquipment;
    Map<String, Integer> equipSlotsAvailable;

    // TODO: reset fatigue every quest to max fatigue

    String heroicFeatStr;
    HeroicFeat.HeroFeat heroicFeat;
    boolean usedHeroAbility, featAvailable, rested, defeated;

    HeroAbilities.HeroAbility heroAbility;
    String abilityStr;

    public Hero(String name, int nActionsPossible) {
        super(name, nActionsPossible);

        skills = new Deck<>("Skills", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        handEquipment = new Deck<>("Hands", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        otherEquipment = new Deck<>("OtherItems", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        equipSlotsAvailable = new HashMap<>();
        equipSlotsAvailable.put("hand", 2);
        equipSlotsAvailable.put("armor", 1);
        equipSlotsAvailable.put("other", 2);

        tokenType = "Hero";
    }

    @Override
    public void resetRound() {
        super.resetRound();
    }

    protected Hero(String name, Counter actions, int ID) {
        super(name, actions, ID);
    }

    public Deck<DescentCard> getSkills() {
        return skills;
    }

    public void setSkills(Deck<DescentCard> skills) {
        this.skills = skills;
    }

    public Deck<DescentCard> getHandEquipment() {
        return handEquipment;
    }

    public void setHandEquipment(Deck<DescentCard> handEquipment) {
        this.handEquipment = handEquipment;
    }

    public DescentCard getArmor() {
        return armor;
    }

    public void setArmor(DescentCard armor) {
        this.armor = armor;
    }

    public Deck<DescentCard> getOtherEquipment() {
        return otherEquipment;
    }

    public void setOtherEquipment(Deck<DescentCard> otherEquipment) {
        this.otherEquipment = otherEquipment;
    }

    public Map<String, Integer> getEquipSlotsAvailable() {
        return equipSlotsAvailable;
    }

    public void setEquipSlotsAvailable(HashMap<String, Integer> equipSlotsAvailable) {
        this.equipSlotsAvailable = equipSlotsAvailable;
    }

    public String getHeroicFeatStr() {
        return heroicFeatStr;
    }

    public HeroicFeat.HeroFeat getHeroicFeat() {
        return heroicFeat;
    }

    public void setHeroicFeatStr(String heroicFeatStr) {
        this.heroicFeatStr = heroicFeatStr;
    }
    public void setHeroicFeat(HeroicFeat.HeroFeat heroicFeat) {
        this.heroicFeat = heroicFeat;
    }

    public boolean isFeatAvailable() {
        return featAvailable;
    }

    public void setFeatAvailable(boolean featAvailable) {
        this.featAvailable = featAvailable;
    }

    public HeroAbilities.HeroAbility getAbility() {
        return heroAbility;
    }
    public String getAbilityStr() {
        return abilityStr;
    }
    public void setAbility(HeroAbilities.HeroAbility heroAbility) {
        this.heroAbility = heroAbility;;
    }

    public void setAbilityStr(String abilityStr) {
        this.abilityStr = abilityStr;
    }

    public boolean hasRested() {
        return rested;
    }

    public void setRested(boolean rested) {
        this.rested = rested;
    }

    public boolean hasUsedHeroAbility() {
        return usedHeroAbility;
    }
    public void setUsedHeroAbility(boolean usedHeroAbility) {
        this.usedHeroAbility = usedHeroAbility;
    }

    public void setDefeated(DescentGameState dgs, boolean defeated) {
        this.defeated = defeated;

        if (defeated) {
            // Loses all health
            setAttribute(Figure.Attribute.Health, 0);
            // Max fatigue
            setAttributeToMax(Figure.Attribute.Fatigue);
            // Discard conditions
            getConditions().clear();
            // Remove from map
            Move.remove(dgs, this);
            setOffMap(true);
        }
        else {
            Move.replace(dgs, this);
        }
    }

    public boolean isDefeated() {
        return defeated;
    }

    public boolean equip(DescentCard c) {
        // Check if equipment
        Property cost = c.getProperty(costHash);
        if (cost != null) {
            // Equipment! Check if it's legal to equip
            String[] equip = ((PropertyStringArray) c.getProperty(equipSlotHash)).getValues();
            boolean canEquip = true;
            Map<String, Integer> equipSlots = new HashMap<>(equipSlotsAvailable);
            for (String e : equip) {
                if (equipSlots.get(e) < 1) {
                    canEquip = false;
                    break;
                } else {
                    equipSlots.put(e, equipSlots.get(e) - 1);
                }
            }
            if (canEquip) {
                equipSlotsAvailable = equipSlots;
                switch (equip[0]) {
                    case "armor":
                        armor = c;
                        break;
                    case "hand":
                        handEquipment.add(c);
                        break;
                    case "other":
                        otherEquipment.add(c);
                        break;
                }
                return true;
            }
            return false;
        } else {
            // A skill
            skills.add(c);
            return true;
        }
    }

    public List<DescentCard> getWeapons() {
        return handEquipment.stream().filter(DescentCard::isAttack).collect(Collectors.toList());
    }

    @Override
    public DicePool getAttackDice() {
        Optional<DescentCard> wpn = getWeapons().stream().findFirst();
        if (wpn.isPresent())
            return wpn.get().getDicePool();
        return DicePool.empty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Hero hero = (Hero) o;
        return usedHeroAbility == hero.usedHeroAbility && featAvailable == hero.featAvailable &&
                rested == hero.rested && defeated == hero.defeated &&
                Objects.equals(skills, hero.skills) && Objects.equals(handEquipment, hero.handEquipment) &&
                Objects.equals(armor, hero.armor) && Objects.equals(otherEquipment, hero.otherEquipment) &&
                Objects.equals(equipSlotsAvailable, hero.equipSlotsAvailable) && Objects.equals(heroicFeatStr, hero.heroicFeatStr) &&
                heroicFeat == hero.heroicFeat && heroAbility == hero.heroAbility && Objects.equals(abilityStr, hero.abilityStr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), skills, handEquipment, armor, otherEquipment,
                equipSlotsAvailable, heroicFeatStr, heroicFeat.ordinal(), usedHeroAbility, featAvailable,
                rested, defeated, heroAbility.ordinal(), abilityStr);
    }

    @Override
    public Hero copy() {
        Hero copy = new Hero(componentName, nActionsExecuted.copy(), componentID);
        return copyTo(copy);
    }

    @Override
    public Hero copyNewID() {
        Hero copy = new Hero(componentName, -1);
        return copyTo(copy);
    }

    private Hero copyTo(Hero copy) {
        copy.equipSlotsAvailable = new HashMap<>();
        copy.equipSlotsAvailable.putAll(equipSlotsAvailable);
        if (skills != null) copy.skills = skills.copy();
        if (handEquipment != null) copy.handEquipment = handEquipment.copy();
        if (otherEquipment != null) copy.otherEquipment = otherEquipment.copy();
        if (armor != null) copy.armor = armor.copy();
        copy.heroicFeatStr = this.heroicFeatStr;
        copy.heroicFeat = this.heroicFeat;
        copy.usedHeroAbility = this.usedHeroAbility;
        copy.featAvailable = this.featAvailable;
        copy.abilityStr = this.abilityStr;
        copy.heroAbility = this.heroAbility;
        copy.rested = this.rested;
        copy.defeated = this.defeated;
        super.copyComponentTo(copy);
        return copy;
    }

    /**
     * Creates a Token objects from a JSON object.
     *
     * @param figure - JSON to parse into Figure object.
     */
    protected void loadHero(JSONObject figure) {
        super.loadFigure(figure);
        String[] defence = ((PropertyStringArray) getProperty(defenceHash)).getValues();
        defenceDice = DicePool.constructDicePool(defence);
        this.featAvailable = true;
        String heroicFeatStrFull = ((PropertyString) getProperty(heroicFeatHash)).value;
        this.heroicFeatStr = heroicFeatStrFull.split(":", 2)[1];
        String abilityStrFull = ((PropertyString) getProperty(abilityHash)).value;
        this.abilityStr = abilityStrFull.split(":", 2)[1];

        // Ensures that, if there is a problem with the json name, a default NONE value is assigned
        this.heroicFeat = HeroicFeat.HeroFeat.NONE;
        this.heroAbility = HeroAbilities.HeroAbility.NONE;
        String tempFeat = heroicFeatStrFull.split(":")[0];
        String tempAbility = abilityStrFull.split(":")[0];
        for (HeroicFeat.HeroFeat feat : HeroicFeat.HeroFeat.values())
        {
            if (feat.name().equals(tempFeat))
            {
                this.heroicFeat = feat;
                break;
            }
        }
        for (HeroAbilities.HeroAbility ability : HeroAbilities.HeroAbility.values())
        {
            if (ability.name().equals(tempAbility))
            {
                this.heroAbility = ability;
                break;
            }
        }
    }

    /**
     * Loads all figures from a JSON file.
     *
     * @param filename - path to file.
     * @return - List of Figure objects.
     */
    public static List<Hero> loadHeroes(String filename) {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Hero> figures = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for (Object o : data) {

                Hero newFigure = new Hero("", -1);
                newFigure.loadHero((JSONObject) o);
                figures.add(newFigure);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return figures;
    }
}
