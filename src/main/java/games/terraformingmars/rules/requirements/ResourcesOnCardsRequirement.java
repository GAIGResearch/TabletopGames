package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;
import utilities.ImageIO;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

public class ResourcesOnCardsRequirement implements Requirement<TMGameState> {

    public final TMTypes.Resource[] resources;
    public final int[] nMin;
    int nResources;

    public ResourcesOnCardsRequirement(TMTypes.Resource[] resources, int[] nMin) {
        this.resources = resources;
        this.nMin = nMin;

        nResources = 0;
        if (resources != null) {
            for (int k = 0; k < resources.length; k++) {
                nResources += nMin[k];
            }
        }
    }

    @Override
    public boolean testCondition(TMGameState gs) {
        for (int i = 0; i < nMin.length; i++) {
            TMTypes.Resource res = resources[i];
            int nRes = 0;
            for (TMCard c: gs.getPlayerComplicatedPointCards()[gs.getCurrentPlayer()].getComponents()) {
                if (c.resourceOnCard == res) nRes += c.nResourcesOnCard;
            }
            if (nRes < nMin[i]) return false;
        }
        return true;
    }

    @Override
    public boolean isMax() {
        return false;
    }

    @Override
    public boolean appliesWhenAnyPlayer() {
        return false;
    }

    @Override
    public String getDisplayText(TMGameState gs) {
        if (nResources > 4) return ""+nMin[0];
        return null;
    }

    @Override
    public String getReasonForFailure(TMGameState gs) {
        String reasons = "";
        for (int i = 0; i < nMin.length; i++) {
            TMTypes.Resource res = resources[i];
            int nRes = 0;
            for (TMCard c: gs.getPlayerComplicatedPointCards()[gs.getCurrentPlayer()].getComponents()) {
                if (c.resourceOnCard == res) nRes += c.nResourcesOnCard;
            }
            if (nRes < nMin[i]) {
                reasons += "Need " + nMin[i] + " " + res + "s on cards. ";
            } else {
                reasons += "Enough " + res + "s on cards. ";
            }
        }
        return reasons;
    }

    @Override
    public Image[] getDisplayImages() {
        int n = nResources;
        if (n > 4) n = resources.length;
        Image[] imgs = new Image[n];
        int i = 0;
        for (int k = 0; k < resources.length; k++) {
            String path = resources[k].getImagePath();
            if (n == nResources) {
                for (int j = 0; j < nMin[k]; j++) {
                    imgs[i] = ImageIO.GetInstance().getImage(path);
                    i++;
                }
            } else {
                imgs[i] = ImageIO.GetInstance().getImage(path);
                i++;
            }
        }
        return imgs;
    }

    @Override
    public ResourcesOnCardsRequirement copy() {
        return new ResourcesOnCardsRequirement(resources.clone(), nMin.clone());
    }

    @Override
    public Requirement<TMGameState> copySerializable() {
        return new ResourcesOnCardsRequirement(resources != null && resources.length > 0 ? resources.clone() : null, nMin != null && nMin.length > 0 ? nMin.clone() : null);
    }

    @Override
    public String toString() {
        return "Tags Played";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourcesOnCardsRequirement)) return false;
        ResourcesOnCardsRequirement that = (ResourcesOnCardsRequirement) o;
        return nResources == that.nResources && Arrays.equals(resources, that.resources) && Arrays.equals(nMin, that.nMin);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(nResources);
        result = 31 * result + Arrays.hashCode(resources);
        result = 31 * result + Arrays.hashCode(nMin);
        return result;
    }
}
