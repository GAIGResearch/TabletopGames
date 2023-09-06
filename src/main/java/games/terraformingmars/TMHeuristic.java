package games.terraformingmars;
import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.terraformingmars.actions.PayForAction;
import games.terraformingmars.actions.PlayCard;
import games.terraformingmars.components.Award;
import games.terraformingmars.components.GlobalParameter;
import games.terraformingmars.components.Milestone;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.effects.Bonus;
import utilities.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import static games.terraformingmars.TMGameState.TMPhase.Research;

public class TMHeuristic extends TunableParameters implements IStateHeuristic {
    // Weights for action phase, should sum up to 1
    double cardsPlayedWeight = 0.2;
    double productionWeight = 0.2;
    double bonusRewardWeight = 0.03;
    double bonusPenaltyWeight = 0.02;  // Opponent got bonus instead of us
    double milestoneRewardWeight = 0.05;
    double milestonePenaltyWeight = 0.05;
    double milestoneUnclaimedWeight = 0.1;
    double awardFundPenaltyWeight = 0.1;
    double awardFundRewardWeight = 0.02;
    double awardScoreWeight = 0.1;
    double globalParamContributionWeight = 0.1;
    double pointsWeight = 0.03;

    // Weights for research phase, should sum up to 1
    double unplayableCardWeight = 0.5;
    double expensiveCardWeight = 0.2;
    double unnecessaryEventCardWeight = 0.2;
    double unnecessaryAutomatedCardWeight = 0.1;

    // Others
    HashMap<TMTypes.Resource, Double> resourceProductionWeight = new HashMap<TMTypes.Resource, Double>() {{
        put(TMTypes.Resource.MegaCredit, 0.4);
        put(TMTypes.Resource.Steel, 0.15);
        put(TMTypes.Resource.Titanium, 0.05);
        put(TMTypes.Resource.Plant, 0.1);
        put(TMTypes.Resource.Energy, 0.2);
        put(TMTypes.Resource.Heat, 0.1);
    }};

    // Thresholds to help normalization, possibly way off
    int nActiveCardsDiffForTerraform = 5;
    int maxProduction = 100;
    int maxAwardScore = 100;
    int expensiveCardThreshold = 20;

    public TMHeuristic() {
        addTunableParameter("cardsPlayedWeight", 0.2);
        addTunableParameter("productionWeight", 0.5);
        addTunableParameter("bonusRewardWeight", 0.5);
        addTunableParameter("bonusPenaltyWeight", 0.5);
        addTunableParameter("milestoneRewardWeight", 0.5);
        addTunableParameter("milestonePenaltyWeight", 0.5);
        addTunableParameter("milestoneUnclaimedWeight", 0.5);
        addTunableParameter("awardFundPenaltyWeight", 0.5);
        addTunableParameter("awardFundRewardWeight", 0.5);
        addTunableParameter("awardScoreWeight", 0.5);
        addTunableParameter("unplayableCardWeight", 0.5);
        addTunableParameter("expensiveCardWeight", 0.5);
        addTunableParameter("unnecessaryEventCardWeight", 0.5);
        addTunableParameter("unnecessaryAutomatedCardWeight", 0.5);
        addTunableParameter("expensiveCardThreshold", 20);
        addTunableParameter("nActiveCardsDiffForTerraform", 0.5);
        addTunableParameter("maxProduction", 0.5);
        addTunableParameter("maxAwardScore", 0.5);
        addTunableParameter("percGPEarly", 0.5);
        addTunableParameter("percGPMid", 0.5);
        addTunableParameter("percGPLate", 0.5);
        addTunableParameter("MegaCreditProdWeight", 0.5);
        addTunableParameter("SteelProdWeight", 0.5);
        addTunableParameter("TitaniumProdWeight", 0.5);
        addTunableParameter("PlantProdWeight", 0.5);
        addTunableParameter("EnergyProdWeight", 0.5);
        addTunableParameter("HeatProdWeight", 0.5);
        _reset();
    }
    @Override
    public void _reset() {
        cardsPlayedWeight = (double) getParameterValue("cardsPlayedWeight");
        productionWeight = (double) getParameterValue("productionWeight");
        bonusRewardWeight = (double) getParameterValue("bonusRewardWeight");
        bonusPenaltyWeight = (double) getParameterValue("bonusPenaltyWeight");
        milestoneRewardWeight = (double) getParameterValue("milestoneRewardWeight");
        milestonePenaltyWeight = (double) getParameterValue("milestonePenaltyWeight");
        milestoneUnclaimedWeight = (double) getParameterValue("milestoneUnclaimedWeight");
        awardFundRewardWeight = (double) getParameterValue("awardFundRewardWeight");
        awardFundPenaltyWeight = (double) getParameterValue("awardFundPenaltyWeight");
        awardScoreWeight = (double) getParameterValue("awardScoreWeight");
        unplayableCardWeight = (double) getParameterValue("unplayableCardWeight");
        expensiveCardWeight = (double) getParameterValue("expensiveCardWeight");
        unnecessaryEventCardWeight = (double) getParameterValue("unnecessaryEventCardWeight");
        unnecessaryAutomatedCardWeight = (double) getParameterValue("unnecessaryAutomatedCardWeight");
        expensiveCardThreshold = (int) getParameterValue("expensiveCardThreshold");
        nActiveCardsDiffForTerraform = (int) getParameterValue("nActiveCardsDiffForTerraform");
        maxProduction = (int) getParameterValue("maxProduction");
        maxAwardScore = (int) getParameterValue("maxAwardScore");
        TMGameStage.Early.setPercGP((double) getParameterValue("percGPEarly"));
        TMGameStage.Mid.setPercGP((double) getParameterValue("percGPMid"));
        TMGameStage.Late.setPercGP((double) getParameterValue("percGPLate"));
        resourceProductionWeight.put(TMTypes.Resource.MegaCredit, (double) getParameterValue("MegaCreditProdWeight"));
        resourceProductionWeight.put(TMTypes.Resource.Steel, (double) getParameterValue("SteelProdWeight"));
        resourceProductionWeight.put(TMTypes.Resource.Titanium, (double) getParameterValue("TitaniumProdWeight"));
        resourceProductionWeight.put(TMTypes.Resource.Plant, (double) getParameterValue("PlantProdWeight"));
        resourceProductionWeight.put(TMTypes.Resource.Energy, (double) getParameterValue("EnergyProdWeight"));
        resourceProductionWeight.put(TMTypes.Resource.Heat, (double) getParameterValue("HeatProdWeight"));
    }

    @Override
    public double evaluateState(AbstractGameState gameState, int playerId) {
        TMGameState gs = (TMGameState) gameState;
        TMGameParameters params = (TMGameParameters) gs.getGameParameters();
        CoreConstants.GameResult playerResult = gs.getPlayerResults()[playerId];

        if(playerResult == CoreConstants.GameResult.LOSE_GAME) {
            return -1;
        }
        if(playerResult == CoreConstants.GameResult.WIN_GAME) {
            return 1;
        }

        double score = (gs.countPoints(playerId)*1.0 / params.maxPoints) * pointsWeight;

        int nAutomatedCardsPlayed = gs.playerCardsPlayedTypes[playerId].get(TMTypes.CardType.Automated).getValue();
        int nActiveCardsPlayed = gs.playerCardsPlayedTypes[playerId].get(TMTypes.CardType.Active).getValue();
        int nEventsPlayed = gs.playerCardsPlayedTypes[playerId].get(TMTypes.CardType.Event).getValue();

        boolean shouldTerraform = false;
        for (int i = 0; i < gs.getNPlayers(); i++) {
            if (i != playerId && gs.playerCardsPlayedTypes[i].get(TMTypes.CardType.Active).getValue() >= nActiveCardsPlayed + nActiveCardsDiffForTerraform) {
                shouldTerraform = true;
                break;
            }
        }

        TMGameStage stage = TMGameStage.getStage(gs);

        if (stage == TMGameStage.Late) return TMGameStage.getGPPerc(gs);

        if (gs.getGamePhase() == Research) {
            // If research phase, evaluate cards in hand
            int nCardsInHand = gs.playerHands[playerId].getSize();
            for (TMCard c: gs.playerHands[playerId].getComponents()) {
                score += evaluate(playerId, c, gs, stage, shouldTerraform) / nCardsInHand;
            }

        } else {

            int nPoints = gs.playerCardPoints[playerId].getValue();

            int nPointCards = 0;
            for (TMCard c: gs.getPlayerComplicatedPointCards()[playerId].getComponents()) {
                if (c.pointsResource != null || c.pointsTag != null || c.pointsTile != null) nPointCards++;
            }

            if (!shouldTerraform) {
                // Evaluate cards played (Prioritize active cards first half of the game (global params < 0.5 done), then green cards with points)
                if (stage != TMGameStage.Late) {
                    score += nActiveCardsPlayed*1.0/params.maxCards * cardsPlayedWeight;
                } else {
                    score += nPointCards*1.0/params.maxCards * cardsPlayedWeight/2 + nPoints*1.0/params.maxPoints * cardsPlayedWeight/2;
                }
            } else {
                // If all opponents have at least 5 more active cards than you, then focus on terraforming instead:  events and standard projects
                score += nEventsPlayed * cardsPlayedWeight;
            }

            // Percentage contribution to global parameters
            double[] perc = new double[gs.globalParameters.size()];
            int i = 0;
            for (GlobalParameter gp: gs.globalParameters.values()) {
                if (gp.getIncreases().size() > 0) {
                    for (int j = 0; j < gp.getIncreases().size(); j++) {
                        if (gp.getIncreases().get(j).b == playerId)
                            perc[i]++;
                    }
                    perc[i] /= gp.getIncreases().size();
                } else {
                    perc[i] = 0;
                }
                i++;
            }
            double p = Arrays.stream(perc).average().orElse(Double.NaN);
            score += p * globalParamContributionWeight;

            // Evaluate current production (prioritize money)
            double production = 0;
            for (TMTypes.Resource r: TMTypes.Resource.values()) {
                if (r.playerBoardRes) production += resourceProductionWeight.get(r) * gs.playerProduction[playerId].get(r).getValue() / maxProduction;
            }
            score += production * productionWeight;

            // Evaluate global parameter bonuses coming up, penalize if opponent got them, reward if we got them
            for (Bonus b: gs.bonuses) {
                if (b.claimed != -1 && b.claimed == playerId) {
                    score += bonusRewardWeight * gs.bonuses.size();
                } else if (b.claimed != -1 && b.claimed != playerId) {
                    score -= bonusPenaltyWeight * gs.bonuses.size();
                }
            }

            // Evaluate milestones, penalize if opponents claimed some, reward if we did, maximize score for top N-claimed milestones remaining
            for (Milestone m : gs.milestones) {
                if (m.isClaimed() && m.claimed == playerId) {
                    score += milestoneRewardWeight / params.nCostMilestone.length;
                } else if (m.isClaimed() && m.claimed != playerId) {
                    score -= milestonePenaltyWeight / params.nCostMilestone.length;
                }
            }
            if (!gs.nMilestonesClaimed.isMaximum()) {
                double ms = 0;
                int nMilestonesUnclaimed = gs.nMilestonesClaimed.getMaximum() - gs.nMilestonesClaimed.getValue();  // Count top N milestone score TODO
                for (Milestone m: gs.milestones) {
                    if (!m.isClaimed()) {
                        ms += m.checkProgress(gs, playerId)*1.0/m.min;  // TODO count opponents, don't try to go for one where opponents have strong advantage already
                    }
                }
                score += milestoneUnclaimedWeight * ms;
            }

            // If early game, penalize if we fund award. If late game (global params >= 0.8), reward if we fund awards we're leading in (and penalize otherwise)
            for (Award a: gs.awards) {
                if (a.isClaimed()) {
                    Pair<HashSet<Integer>, HashSet<Integer>> wins = gs.awardWinner(a);
                    if (a.claimed == playerId) {
                        if (stage == TMGameStage.Early) {
                            score -= awardFundPenaltyWeight / params.nCostAwards.length;
                        } else if (stage == TMGameStage.Late) {
                            if (wins.a.contains(playerId)) score += awardFundRewardWeight / params.nCostAwards.length;
                            else score -= awardFundPenaltyWeight / params.nCostAwards.length;
                        }
                    }

                    // Maximise score for funded awards
                    score += (a.checkProgress(gs, playerId)*1.0 / maxAwardScore) * (awardScoreWeight / params.nCostAwards.length);
                }
            }

        }

        return score;
    }

    private double evaluate(int playerId, TMCard c, TMGameState gs, TMGameStage stage, boolean shouldTerraform) {
        // Don't buy cards you can't play in < 2 generations (requirements or cost related)
        // Don't buy expensive cards early (cost < 20, gen <= 4) unless active cards
        // Don't buy events early (gen <= 4), and then only if you need to terraform (opponents have at least 5 more active cards than you)
        double score = 0;

        TMGameParameters params = (TMGameParameters) gs.getGameParameters();

        int actualCost = c.cost + params.projectPurchaseCost;
        PayForAction pa = new PayForAction(playerId, new PlayCard(playerId, c, false));
        if (!pa.canBePlayed(gs) && c.cardType != TMTypes.CardType.Active) {
            score -= unplayableCardWeight;
        } else {
            score += unplayableCardWeight;
        }
        if (c.cost < expensiveCardThreshold && stage == TMGameStage.Early && c.cardType != TMTypes.CardType.Active) {
            score -= expensiveCardWeight;
        } else {
            score += expensiveCardWeight;
        }
        if (c.cardType == TMTypes.CardType.Event && (stage == TMGameStage.Early || !shouldTerraform)) {
            score -= unnecessaryEventCardWeight;
        } else {
            score += unnecessaryEventCardWeight;
        }
        if (c.cardType == TMTypes.CardType.Automated && stage == TMGameStage.Early && (c.immediateEffects == null || c.immediateEffects.length == 0)) {
            score -= unnecessaryAutomatedCardWeight;
        } else {
            score += unnecessaryAutomatedCardWeight;
        }
        return score;
    }

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     *
     * @return - new game parameters object.
     */
    @Override
    protected TMHeuristic _copy() {
        TMHeuristic retValue = new TMHeuristic();

        retValue.cardsPlayedWeight = cardsPlayedWeight;
        retValue.productionWeight = productionWeight;
        retValue.bonusRewardWeight = bonusRewardWeight;
        retValue.bonusPenaltyWeight = bonusPenaltyWeight;
        retValue.milestoneRewardWeight = milestoneRewardWeight;
        retValue.milestonePenaltyWeight = milestonePenaltyWeight;
        retValue.milestoneUnclaimedWeight = milestoneUnclaimedWeight;
        retValue.awardFundPenaltyWeight = awardFundPenaltyWeight;
        retValue.awardFundRewardWeight = awardFundRewardWeight;
        retValue.awardScoreWeight = awardScoreWeight;
        retValue.unplayableCardWeight = unplayableCardWeight;
        retValue.expensiveCardWeight = expensiveCardWeight;
        retValue.unnecessaryEventCardWeight = unnecessaryEventCardWeight;
        retValue.unnecessaryAutomatedCardWeight = unnecessaryAutomatedCardWeight;
        retValue.resourceProductionWeight = new HashMap<>(resourceProductionWeight);
        retValue.nActiveCardsDiffForTerraform = nActiveCardsDiffForTerraform;
        retValue.maxProduction = maxProduction;
        retValue.maxAwardScore = maxAwardScore;
        retValue.expensiveCardThreshold = expensiveCardThreshold;

        return retValue;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TMHeuristic)) return false;
        if (!super.equals(o)) return false;
        TMHeuristic that = (TMHeuristic) o;
        return Double.compare(that.cardsPlayedWeight, cardsPlayedWeight) == 0 && Double.compare(that.productionWeight, productionWeight) == 0 && Double.compare(that.bonusRewardWeight, bonusRewardWeight) == 0 && Double.compare(that.bonusPenaltyWeight, bonusPenaltyWeight) == 0 && Double.compare(that.milestoneRewardWeight, milestoneRewardWeight) == 0 && Double.compare(that.milestonePenaltyWeight, milestonePenaltyWeight) == 0 && Double.compare(that.milestoneUnclaimedWeight, milestoneUnclaimedWeight) == 0 && Double.compare(that.awardFundPenaltyWeight, awardFundPenaltyWeight) == 0 && Double.compare(that.awardFundRewardWeight, awardFundRewardWeight) == 0 && Double.compare(that.awardScoreWeight, awardScoreWeight) == 0 && Double.compare(that.unplayableCardWeight, unplayableCardWeight) == 0 && Double.compare(that.expensiveCardWeight, expensiveCardWeight) == 0 && Double.compare(that.unnecessaryEventCardWeight, unnecessaryEventCardWeight) == 0 && Double.compare(that.unnecessaryAutomatedCardWeight, unnecessaryAutomatedCardWeight) == 0 && nActiveCardsDiffForTerraform == that.nActiveCardsDiffForTerraform && maxProduction == that.maxProduction && maxAwardScore == that.maxAwardScore && expensiveCardThreshold == that.expensiveCardThreshold && Objects.equals(resourceProductionWeight, that.resourceProductionWeight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardsPlayedWeight, productionWeight, bonusRewardWeight, bonusPenaltyWeight, milestoneRewardWeight, milestonePenaltyWeight, milestoneUnclaimedWeight, awardFundPenaltyWeight, awardFundRewardWeight, awardScoreWeight, unplayableCardWeight, expensiveCardWeight, unnecessaryEventCardWeight, unnecessaryAutomatedCardWeight, resourceProductionWeight, nActiveCardsDiffForTerraform, maxProduction, maxAwardScore, expensiveCardThreshold);
    }

    /**
     * @return Returns Tuned Parameters corresponding to the current settings
     * (will use all defaults if setParameterValue has not been called at all)
     */
    @Override
    public TMHeuristic instantiate() {
        return this._copy();
    }

    enum TMGameStage {
        Early(0.4),
        Mid(0.6),
        Late(0.8);

        double percGP;

        TMGameStage(double percentage) {
            this.percGP = percentage;
        }

        public void setPercGP(double percGP) {
            this.percGP = percGP;
        }

        static TMGameStage getStage(TMGameState gs) {
            double p = getGPPerc(gs);
            if (p <= Early.percGP) return Early;
            else if (p <= Late.percGP) return Mid;
            else return Late;
        }

        static double getGPPerc(TMGameState gs) {
            double[] perc = new double[gs.globalParameters.size()];
            int i = 0;
            for (GlobalParameter gp: gs.globalParameters.values()) {
                perc[i] = gp.getValueIdx()*1.0 / gp.getMaximum();
                i++;
            }
            return Arrays.stream(perc).average().orElse(Double.NaN);
        }
    }

}