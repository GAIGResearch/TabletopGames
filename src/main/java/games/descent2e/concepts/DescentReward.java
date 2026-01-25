package games.descent2e.concepts;

import core.CoreConstants;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import org.json.simple.JSONObject;
import utilities.Utils;

import java.util.Objects;

public class DescentReward {
    enum RewardType {
        Attribute,
        Token
    }
    enum ApplyType {
        Add,
        Set
    }

    RewardType rewardType;
    Figure.Attribute attribute;
    DescentTypes.DescentToken token;
    double value;
    boolean mustWinToReceive, mustLoseToReceive;
    ApplyType apply = ApplyType.Add;
    double multiplier = 1.0;
    CountGameFeature countGameFeatureMultiplier;

    public void applyReward(DescentGameState gs, int playerIdx) {
        if (mustWinToReceive && gs.getPlayerResults()[playerIdx] != CoreConstants.GameResult.WIN_GAME ||
            mustLoseToReceive && gs.getPlayerResults()[playerIdx] != CoreConstants.GameResult.LOSE_GAME) return;

        // Calculate modifier if needed
        if (countGameFeatureMultiplier != null) multiplier = countGameFeatureMultiplier.count(gs);

        if (rewardType == RewardType.Token) {
            // more complicated TODO
        } else if (rewardType == RewardType.Attribute) {
            Figure f;
            if (playerIdx == gs.getOverlordPlayer()) f = gs.getOverlord();
            else f = gs.getHeroes().get(playerIdx); // TODO is this idx correct?
            if (apply == ApplyType.Add) {
                f.incrementAttribute(attribute, (int)(value * multiplier));
            } else if (apply == ApplyType.Set) {
                f.setAttribute(attribute, (int)(value * multiplier));
            }
        }
    }

    public static DescentReward parse(JSONObject jsonObject) {
        DescentReward dr = new DescentReward();
        dr.rewardType = RewardType.valueOf ((String) jsonObject.get("rewardType"));
        if (dr.rewardType == RewardType.Attribute) dr.attribute = Figure.Attribute.valueOf((String)jsonObject.get("attribute"));
        else if (dr.rewardType == RewardType.Token) dr.token = DescentTypes.DescentToken.valueOf((String)jsonObject.get("token"));
        dr.value = (double) jsonObject.get("value");
        if (jsonObject.containsKey("apply")) dr.apply = ApplyType.valueOf((String) jsonObject.get("apply"));
        if (jsonObject.containsKey("multiplier")) {
            Object o = jsonObject.get("multiplier");
            if (o instanceof JSONObject) {
                // parse first as a CountGameFeature class
                JSONObject def = (JSONObject) o;
                dr.countGameFeatureMultiplier = CountGameFeature.parse(def);
            } else {
                dr.multiplier = (double) jsonObject.get("multiplier");
            }
        }
        if (jsonObject.containsKey("mustWinToReceive")) dr.mustWinToReceive = (boolean) jsonObject.get("mustWinToReceive");
        if (jsonObject.containsKey("mustLoseToReceive")) dr.mustLoseToReceive = (boolean) jsonObject.get("mustLoseToReceive");
        return dr;
    }

    public DescentReward copy() {
        DescentReward dr = new DescentReward();
        dr.rewardType = rewardType;
        dr.attribute = attribute;
        dr.token = token;
        dr.value = value;
        dr.mustWinToReceive = mustWinToReceive;
        dr.mustLoseToReceive = mustLoseToReceive;
        dr.apply = apply;
        dr.multiplier = multiplier;
        if (countGameFeatureMultiplier != null) dr.countGameFeatureMultiplier = countGameFeatureMultiplier.copy();
        return dr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DescentReward that = (DescentReward) o;
        return Double.compare(that.value, value) == 0 && mustWinToReceive == that.mustWinToReceive && mustLoseToReceive == that.mustLoseToReceive && Double.compare(that.multiplier, multiplier) == 0 && rewardType == that.rewardType && attribute == that.attribute && token == that.token && apply == that.apply && Objects.equals(countGameFeatureMultiplier, that.countGameFeatureMultiplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rewardType, attribute, token, value, mustWinToReceive, mustLoseToReceive, apply, multiplier, countGameFeatureMultiplier);
    }
}
