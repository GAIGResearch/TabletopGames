package games.virus;

import games.virus.cards.VirusCard;

public class VirusBody {
    public VirusOrgan hearth;
    public VirusOrgan brain;
    public VirusOrgan stomach;
    public VirusOrgan bone;

    public VirusBody()
    {
        hearth  = new VirusOrgan();
        brain   = new VirusOrgan();
        stomach = new VirusOrgan();
        bone    = new VirusOrgan();
    }

    @Override
    public String toString() {
        return "Hearth: " + hearth.toString() + " Brain: " + brain.toString() +" Stomach: " + stomach.toString() + " Bone: " + bone.toString();
    }

    public boolean hasOrgan(VirusCard.VirusCardOrgan organ) {
        if (organ == VirusCard.VirusCardOrgan.Hearth)
            return hearth.state != VirusOrgan.VirusOrganState.None;
        else if (organ == VirusCard.VirusCardOrgan.Stomach)
            return stomach.state != VirusOrgan.VirusOrganState.None;
        else if (organ == VirusCard.VirusCardOrgan.Brain)
            return brain.state != VirusOrgan.VirusOrganState.None;
        else if (organ == VirusCard.VirusCardOrgan.Bone)
            return bone.state != VirusOrgan.VirusOrganState.None;
        return false;
    }

    public boolean hasOrganImmunised(VirusCard.VirusCardOrgan organ) {
        if (organ == VirusCard.VirusCardOrgan.Hearth)
            return hearth.state == VirusOrgan.VirusOrganState.Immunised;
        else if (organ == VirusCard.VirusCardOrgan.Stomach)
            return stomach.state == VirusOrgan.VirusOrganState.Immunised;
        else if (organ == VirusCard.VirusCardOrgan.Brain)
            return brain.state == VirusOrgan.VirusOrganState.Immunised;
        else if (organ == VirusCard.VirusCardOrgan.Bone)
            return bone.state == VirusOrgan.VirusOrganState.Immunised;
        return false;
    }

    public void addOrgan(VirusCard card) {
        if (card.organ == VirusCard.VirusCardOrgan.Hearth) {
            hearth.initialiseOrgan();
            hearth.cards.add(card);
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Stomach) {
            stomach.initialiseOrgan();
            stomach.cards.add(card);
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Brain) {
            brain.initialiseOrgan();
            brain.cards.add(card);
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Bone) {
            bone.initialiseOrgan();
            bone.cards.add(card);
        }
    }

    public VirusOrgan.VirusOrganState applyMedicine(VirusCard card) {
        VirusOrgan.VirusOrganState newState = VirusOrgan.VirusOrganState.None;
        if (card.organ == VirusCard.VirusCardOrgan.Hearth) {
            newState = hearth.applyMedicine();
            hearth.cards.add(card);
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Stomach) {
            newState = stomach.applyMedicine();
            stomach.cards.add(card);
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Brain) {
            newState = brain.applyMedicine();
            brain.cards.add(card);
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Bone) {
            newState = bone.applyMedicine();
            bone.cards.add(card);
        }
        return newState;
    }

    public VirusOrgan.VirusOrganState applyVirus(VirusCard card) {
        VirusOrgan.VirusOrganState newState = VirusOrgan.VirusOrganState.None;

        if (card.organ == VirusCard.VirusCardOrgan.Hearth) {
            newState = hearth.applyVirus();
            hearth.cards.add(card);
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Stomach) {
            newState = stomach.applyVirus();
            stomach.cards.add(card);
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Brain) {
            newState = brain.applyVirus();
            brain.cards.add(card);
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Bone) {
            newState = bone.applyVirus();
            bone.cards.add(card);
        }
        return newState;
    }

    public VirusCard removeAVirusCard(VirusCard card)
    {
        if (card.organ == VirusCard.VirusCardOrgan.Hearth) {
            return hearth.removeAVirusCard();
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Stomach) {
            return stomach.removeAVirusCard();
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Brain) {
            return brain.removeAVirusCard();
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Bone) {
            return bone.removeAVirusCard();
        }
        return null;
    }

    public VirusCard removeAMedicineCard(VirusCard card)
    {
        if (card.organ == VirusCard.VirusCardOrgan.Hearth) {
            return hearth.removeAMedicineCard();
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Stomach) {
            return stomach.removeAMedicineCard();
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Brain) {
            return brain.removeAMedicineCard();
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Bone) {
            return bone.removeAMedicineCard();
        }
        return null;
    }

    public VirusCard removeAnOrganCard(VirusCard card)
    {
        if (card.organ == VirusCard.VirusCardOrgan.Hearth) {
            return hearth.removeAnOrganCard();
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Stomach) {
            return stomach.removeAnOrganCard();
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Brain) {
            return brain.removeAnOrganCard();
        }
        else if (card.organ == VirusCard.VirusCardOrgan.Bone) {
            return bone.removeAnOrganCard();
        }
        return null;
    }

    // Return true if the body has all organs and they are healthy
    public boolean areAllOrganHealthy()
    {
        return hearth.isHealthy() && stomach.isHealthy() && brain.isHealthy() && bone.isHealthy();
    }

}
