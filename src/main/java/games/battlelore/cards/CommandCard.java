package games.battlelore.cards;

import core.components.Card;
import games.battlelore.components.Unit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommandCard extends Card
{
    //Command cards allow different type of "orders" to troops
    public enum CommandType
    {
        LineAdvance, //1-1-1
        PatrolLeft, //2-0-0
        AttackRight, //0-0-3
        Wedge, // 1-2-1
        Onslaught,
        DarkenTheSkies,
        BattleMarch
    }

    public enum Effects
    {
        LoreGain,// BattleLore
        AdditionalMove, //Onslaught
        AdditionalAttack
        //DarkenTheSkies
    }

    public enum Condition
    {
        NotWeak, //BattleMarch
        OnlyInfantry //Onslaught

    }

    private int orderCount;
    private int bonusAttackCount;
    private int bonusLorePoints;
    private int bonusMoveCount;
    private Boolean hasCondition;
    protected String title;
    protected String explanation;


    public CommandCard(String title, String explanation, int orderCount, int bonusAttackCount, int bonusLorePoints, int bonusMoveCount)
    {
        this.title = title;
        this.explanation = explanation;
        this.orderCount = orderCount;
        this.bonusAttackCount = bonusAttackCount;
        this.bonusLorePoints = bonusLorePoints;
        this.bonusMoveCount = bonusMoveCount;

    }

    public CommandCard()
    {
        this.title = "N/A";
        this.orderCount = 0;
        this.bonusAttackCount = 0;
        this.bonusLorePoints = 0;
        this.bonusMoveCount = 0;


    }


    public static List<CommandCard> loadCommandCards(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<CommandCard> commandCards = new ArrayList<>();

        try (FileReader reader = new FileReader(filename))
        {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                CommandCard newCard = new CommandCard();
                newCard.loadCommandCard((JSONObject) o);
                commandCards.add(newCard);
            }

        }
        catch (IOException | ParseException e)
        {
            e.printStackTrace();
        }

        return commandCards;
    }

    public void loadCommandCard(JSONObject unit)
    {
        this.title = (String) unit.get("title");
        this.explanation = (String) unit.get("explanation");
        this.orderCount = ((Long) ( (JSONArray) unit.get("orderCount")).get(1)).intValue();
        this.hasCondition = ((Boolean) ( (JSONArray) unit.get("hasCondition")).get(1)).booleanValue();
        this.bonusLorePoints = ((Long) ( (JSONArray) unit.get("bonusLorePoints")).get(1)).intValue();
        this.bonusMoveCount = ((Long) ( (JSONArray) unit.get("bonusMove")).get(1)).intValue();

        parseComponent(this, unit);
    }
}
