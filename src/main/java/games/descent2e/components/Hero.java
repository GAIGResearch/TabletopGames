package games.descent2e.components;

import core.CoreConstants;
import core.components.Card;
import core.components.Deck;
import core.properties.Property;
import core.properties.PropertyString;
import core.properties.PropertyStringArray;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static games.descent2e.DescentConstants.*;

// TODO: figure out how to do ability/heroic-feat
public class Hero extends Figure {

    Deck<Card> skills;
    Deck<Card> handEquipment;
    Card armor;
    Deck<Card> otherEquipment;
    Map<String, Integer> equipSlotsAvailable;

    // TODO: reset fatigue every quest to max fatigue
    String[] defence;

    String heroicFeat;
    boolean featAvailable, rested;

    String ability;

    public Hero(String name) {
        super(name);

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
        if (rested) attributes.get(Attribute.Fatigue).setValue(0);
        rested = false;
    }

    protected Hero(String name, int ID) {
        super(name, ID);
    }

    public Deck<Card> getSkills() {
        return skills;
    }

    public void setSkills(Deck<Card> skills) {
        this.skills = skills;
    }

    public Deck<Card> getHandEquipment() {
        return handEquipment;
    }

    public void setHandEquipment(Deck<Card> handEquipment) {
        this.handEquipment = handEquipment;
    }

    public Card getArmor() {
        return armor;
    }

    public void setArmor(Card armor) {
        this.armor = armor;
    }

    public Deck<Card> getOtherEquipment() {
        return otherEquipment;
    }

    public void setOtherEquipment(Deck<Card> otherEquipment) {
        this.otherEquipment = otherEquipment;
    }

    public Map<String, Integer> getEquipSlotsAvailable() {
        return equipSlotsAvailable;
    }

    public void setEquipSlotsAvailable(HashMap<String, Integer> equipSlotsAvailable) {
        this.equipSlotsAvailable = equipSlotsAvailable;
    }

    public String[] getDefence() {
        return defence;
    }

    public void setDefence(String[] defence) {
        this.defence = defence;
    }

    public String getHeroicFeat() {
        return heroicFeat;
    }

    public void setHeroicFeat(String heroicFeat) {
        this.heroicFeat = heroicFeat;
    }

    public boolean isFeatAvailable() {
        return featAvailable;
    }

    public void setFeatAvailable(boolean featAvailable) {
        this.featAvailable = featAvailable;
    }

    public String getAbility() {
        return ability;
    }

    public void setAbility(String ability) {
        this.ability = ability;
    }

    public boolean hasRested() {
        return rested;
    }

    public void setRested(boolean rested) {
        this.rested = rested;
    }

    public boolean equip(Card c) {
        // Check if equipment
        Property cost = c.getProperty(costHash);
        if (cost != null) {
            // Equipment! Check if it's legal to equip
            String[] equip = ((PropertyStringArray)c.getProperty(equipSlotHash)).getValues();
            boolean canEquip = true;
            Map<String, Integer> equipSlots = new HashMap<>(equipSlotsAvailable);
            for (String e: equip) {
                if (equipSlots.get(e) < 1) {
                    canEquip = false;
                    break;
                } else {
                    equipSlots.put(e, equipSlots.get(e)-1);
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

    public List<Item> getWeapons() {
        List<Item> retValue =  new ArrayList<>();
        for (int i = 0; i < handEquipment.getSize(); i++) {
            Item c = new Item(handEquipment.get(i));
            if (c.isAttack()) {
                retValue.add(c);
            }
        }
        return retValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hero)) return false;
        if (!super.equals(o)) return false;
        Hero hero = (Hero) o;
        return featAvailable == hero.featAvailable && rested == hero.rested && Objects.equals(skills, hero.skills) && Objects.equals(handEquipment, hero.handEquipment) && Objects.equals(armor, hero.armor) && Objects.equals(otherEquipment, hero.otherEquipment) && Objects.equals(equipSlotsAvailable, hero.equipSlotsAvailable) && Arrays.equals(defence, hero.defence) && Objects.equals(heroicFeat, hero.heroicFeat) && Objects.equals(ability, hero.ability);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), skills, handEquipment, armor, otherEquipment, equipSlotsAvailable, heroicFeat, featAvailable, rested, ability);
        result = 31 * result + Arrays.hashCode(defence);
        return result;
    }

    @Override
    public Hero copy() {
        Hero copy = new Hero(componentName, componentID);
        return copyTo(copy);
    }

    @Override
    public Hero copyNewID() {
        Hero copy = new Hero(componentName);
        return copyTo(copy);
    }

    @NotNull
    private Hero copyTo(Hero copy) {
        copy.equipSlotsAvailable = new HashMap<>();
        copy.equipSlotsAvailable.putAll(equipSlotsAvailable);
        copy.skills = skills.copy();
        copy.handEquipment = handEquipment.copy();
        copy.otherEquipment = otherEquipment.copy();
        if (armor != null) {
            copy.armor = armor.copy();
        }
        copy.defence = new String[this.defence.length];
        System.arraycopy(this.defence, 0, copy.defence, 0, this.defence.length);
        copy.heroicFeat = this.heroicFeat;
        copy.featAvailable = this.featAvailable;
        copy.ability = this.ability;
        copy.rested = rested;
        super.copyComponentTo(copy);
        return copy;
    }

    /**
     * Creates a Token objects from a JSON object.
     * @param figure - JSON to parse into Figure object.
     */
    protected void loadHero(JSONObject figure) {
        super.loadFigure(figure);
        this.defence = ((PropertyStringArray)getProperty(defenceHash)).getValues();
        this.featAvailable = true;
        this.heroicFeat = ((PropertyString)getProperty(heroicFeatHash)).value;
        this.ability = ((PropertyString)getProperty(abilityHash)).value;
    }

    /**
     * Loads all figures from a JSON file.
     * @param filename - path to file.
     * @return - List of Figure objects.
     */
    public static List<Hero> loadHeroes(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Hero> figures = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                Hero newFigure = new Hero("");
                newFigure.loadHero((JSONObject) o);
                figures.add(newFigure);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return figures;
    }
}
