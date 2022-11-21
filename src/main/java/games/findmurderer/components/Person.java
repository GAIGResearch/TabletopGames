package games.findmurderer.components;

import core.components.Token;
import games.findmurderer.MurderGameState;

import java.util.*;

public class Person extends Token {

    public enum PersonType {
        Killer,
        Civilian
    }

    public enum Status {
        Dead,
        Alive;

        @Override
        public String toString() {
            return name().substring(0,1);
        }
    }

    public PersonType personType;  // The type of person: civilian or killer
    public Status status; // The status of the person, alive or dead
    public MurderGameState.PlayerMapping killer;  // The player who killed this person (null if person is alive)

    public TreeMap<Integer, HashSet<Integer>> interactionHistory;  // Mapping from round number to list of other people they were adjacent to in that round

    public Person() {
        super("");

        // By default, people are alive and civilians
        this.personType = PersonType.Civilian;
        this.status = Status.Alive;

        // Initialise hashmap
        interactionHistory = new TreeMap<>();
    }

    // Setters:
    public void setPersonType(PersonType pt) {
        personType = pt;
    }
    public void setStatus(Status status) {
        this.status = status;
    }

    public void addInteractions(int round, HashSet<Integer> adjacent) {
        this.interactionHistory.put(round, adjacent);
    }

    // Copy constructor
    Person(String name, int ID) {
        super(name, ID);
    }

    @Override
    public Person copy() {
        // Deep copy of the person object, keeps the same ID for the person
        Person p = new Person("", componentID);
        p.status = status;
        p.personType = personType;
        p.killer = killer;
        p.interactionHistory = new TreeMap<>();
        for (int r: interactionHistory.keySet()) {
            p.interactionHistory.put(r, new HashSet<>(interactionHistory.get(r)));
        }
        copyComponentTo(this);
        return p;
    }

    @Override
    public String toString() {
        return componentID + " (" + status.toString() + ")" + (personType==PersonType.Killer? "-K" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;
        if (!super.equals(o)) return false;
        Person person = (Person) o;
        return personType == person.personType && status == person.status && killer == person.killer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), personType, status, killer);
    }
}
