package games.toads;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ToadParameters extends TunableParameters {

    String dataPath = "data/toads/";
    String cardFile = "cards.json";
    int nRounds = 2;
    int nCards = 2;
    int handSize = 4;

    public ToadParameters() {
        addTunableParameter("dataPath", "data/toads/");
        addTunableParameter("cardFile", "cards.json");
        addTunableParameter("nRounds", 2);
        addTunableParameter("nCards", 2);
        addTunableParameter("handSize", 4);
    }

    @Override
    public void _reset() {
        dataPath = (String) getParameterValue("dataPath");
        nRounds = (int) getParameterValue("nRounds");
        nCards = (int) getParameterValue("nCards");
        cardFile = (String) getParameterValue("cardFile");
        handSize = (int) getParameterValue("handSize");
    }

    public List<ToadCard> getCardDeck() {
        // load in the cards from the data path
        List<ToadCard> retValue = new ArrayList<>();
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(dataPath + cardFile)) {

            JSONObject tmp = (JSONObject) jsonParser.parse(reader);
            JSONArray data = (JSONArray) tmp.get("cards");
            for (Object o : data) {
                JSONObject card = (JSONObject) o;
                String name = (String) card.get("name");
                int value = ((Long) card.get("value")).intValue();
                int count = ((Long) card.get("count")).intValue();
                String cardClass = (String) card.get("special");
                ToadAbility actionClass = (cardClass == null || cardClass.isEmpty()) ? null :
                        Class.forName("games.toads.abilities." + cardClass)
                                .asSubclass(ToadAbility.class).getDeclaredConstructor().newInstance();

                for (int i = 0; i < count; i++)
                    retValue.add(new ToadCard(name, value, actionClass));
            }

        } catch (IOException | ParseException e) {
            System.out.println("Error reading cards from file");
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.out.println("Error instantiating card class");
            throw new RuntimeException(e);
        }

        return retValue;
    }

    @Override
    protected AbstractParameters _copy() {
        return new ToadParameters();
    }


    @Override
    protected boolean _equals(Object o) {
        return (o instanceof ToadParameters) && super.equals(o);
    }

    @Override
    public ToadParameters instantiate() {
        return this;
    }

}
