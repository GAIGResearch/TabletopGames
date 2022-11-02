package core.interfaces;

public interface ILearner {

    void learnFrom(String... files);

    boolean writeToFile(String file);

    String name();

}
