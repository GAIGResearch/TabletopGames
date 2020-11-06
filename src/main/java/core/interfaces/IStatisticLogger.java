package core.interfaces;

import utilities.StatSummary;

import java.util.*;

public interface IStatisticLogger {

    void record(Map<String, ?> data);

    StatSummary summary();

}
