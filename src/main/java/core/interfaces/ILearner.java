package core.interfaces;

public interface ILearner {

    Object learnFrom(String... files);

    String name();
}
