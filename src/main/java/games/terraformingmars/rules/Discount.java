package games.terraformingmars.rules;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import games.terraformingmars.rules.requirements.Requirement;
import utilities.Pair;

/** Written to JSON as {@code {"requirement": ..., "amount": n}}. */
@JsonIgnoreProperties({"a", "b"})
public class Discount extends Pair<Requirement, Integer> {
    @JsonCreator
    public Discount(@JsonProperty("requirement") Requirement a, @JsonProperty("amount") Integer b) {
        super(a, b);
    }

    @JsonProperty("requirement")
    Requirement requirement() {
        return a;
    }

    @JsonProperty("amount")
    Integer amount() {
        return b;
    }
}
