package games.backgammon.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPlayerDecorator;
import core.interfaces.IToJSON;
import games.backgammon.BGGamePhase;
import games.backgammon.BGGameState;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LoadedDiceDecorator implements IPlayerDecorator, IToJSON {

    double MAX_DETECTION_CHANCE = 0.10;

    final List<double[]> pdfs;
    final boolean permanentChange;
    double detectionChance;
    final boolean randomiseDetectionChance;
    // currently we only load the first die (the second/third will not be changed)

    public LoadedDiceDecorator(int sides, double[] probabilities, boolean permanentChange, double chance) {
        pdfs = new ArrayList<>(probabilities.length / sides);
        this.randomiseDetectionChance = chance < 0.0;
        this.permanentChange = permanentChange;
        this.detectionChance = chance;
        int nDice = probabilities.length / sides;
        for (int i = 0; i < nDice; i++) {
            double[] pdf = new double[sides];
            for (int j = 0; j < sides; j++) {
                pdf[j] = probabilities[i * sides + j];
            }
            pdfs.add(pdf);
        }
    }

    public LoadedDiceDecorator(JSONObject json) {
        // we expect a JSON object with a "probabilities" field
        pdfs = new ArrayList<>();
        int sides = json.get("sides") != null ? ((Long) json.get("sides")).intValue() : 6; // default to 6 sides if not specified
        List<Double> probabilities = (List<Double>) json.get("probabilities");
        detectionChance = json.get("detectionChance") != null ? (Double) json.get("detectionChance") : 0.0;
        randomiseDetectionChance = detectionChance < 0.0;
        MAX_DETECTION_CHANCE = json.get("maxDetectionChance") != null ? (Double) json.get("maxDetectionChance") : MAX_DETECTION_CHANCE;
        permanentChange = json.get("isPermanent") != null ? (Boolean) json.get("isPermanent") : false; // default to false if not specified
        double[] pdf = new double[sides];
        for (int i = 0; i < probabilities.size(); i++) {
            pdf[i % sides] += probabilities.get(i);
            if ((i + 1) % sides == 0) {
                pdfs.add(pdf);
                pdf = new double[sides];
            }
        }
        // log any unexpected entries in the json file
        String[] expected = new String[]{"class", "sides", "probabilities", "isPermanent", "detectionChance"};
        for (Object key : json.keySet()) {
            if (key instanceof String keyString) {
                if (!Arrays.asList(expected).contains(keyString)) {
                    System.out.println("Unexpected key in LoadedDiceDecorator JSON: " + key);
                }
            } else {
                System.out.println("Unexpected key in LoadedDiceDecorator JSON: " + key);
            }
        }
    }


    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("class", this.getClass().getCanonicalName());
        json.put("sides", pdfs.getFirst().length);
        JSONArray probabilities = new JSONArray();
        for (double[] pdf : pdfs) {
            for (double prob : pdf) {
                probabilities.add(prob);
            }
        }
        json.put("probabilities", probabilities);
        json.put("isPermanent", permanentChange);
        json.put("detectionChance", randomiseDetectionChance ? -1.0 : detectionChance);
        if (MAX_DETECTION_CHANCE != 0.10)
            json.put("maxDetectionChance", MAX_DETECTION_CHANCE);
        return json;
    }

    public double[] getPDF(int n) {
        return pdfs.get(n);
    }

    public int getPDFCount() {
        return pdfs.size();
    }

    private boolean pdfsAreRoughlyEqual(double[] pdf1, double[] pdf2) {
        if (pdf1 == null ^ pdf2 == null)
            return false;
        if (pdf1 == null && pdf2 == null)
            return true;
        if (pdf1.length != pdf2.length) {
            return false;
        }
        for (int i = 0; i < pdf1.length; i++) {
            if (Math.abs(pdf1[i] - pdf2[i]) > 0.001) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
        // we add the LoadDice action to the list of possible actions for the decision player
        if (state.getGamePhase() == BGGamePhase.RollDice) {
            List<AbstractAction> newPossibleActions = new ArrayList<>(possibleActions);
            BGGameState bgs = (BGGameState) state;
            double[] currentPDF = bgs.getDicePdf(0);
            for (double[] pdf : pdfs) {
                if (pdfsAreRoughlyEqual(currentPDF, pdf))
                    continue; // skip the current pdf, as this is already in use
                if (permanentChange) {
                    newPossibleActions.add(LoadDice.getPermanentShift(0, pdf, detectionChance));
                } else {
                    newPossibleActions.add(LoadDice.getOneOffShift(0, pdf, detectionChance));
                }
            }
            return newPossibleActions;
        }
        return possibleActions;
    }

    @Override
    public boolean decisionPlayerOnly() {
        return true; // the opponent is not modelled as being able to cheat
    }

    @Override
    public void initialiseBeforeGame() {
        IPlayerDecorator.super.initialiseBeforeGame();
        if (randomiseDetectionChance) {
            // we randomise up to max detection chance
            detectionChance = Math.random() * MAX_DETECTION_CHANCE;
        }
    }

    public boolean isPermanent() {
        return permanentChange;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LoadedDiceDecorator that = (LoadedDiceDecorator) obj;
        // check pdfs
        if (pdfs.size() != that.pdfs.size()) return false;
        for (int i = 0; i < pdfs.size(); i++) {
            for (int j = 0; j < pdfs.get(i).length; j++) {
                if (Math.abs(pdfs.get(i)[j] - that.pdfs.get(i)[j]) > 0.0001) return false;
            }
        }
        return permanentChange == that.permanentChange &&
                (detectionChance == that.detectionChance || randomiseDetectionChance) &&
                MAX_DETECTION_CHANCE == that.MAX_DETECTION_CHANCE &&
                randomiseDetectionChance == that.randomiseDetectionChance;
    }

}
