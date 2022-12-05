package evaluation.loggers;
import core.interfaces.IStatisticLogger;
import evaluation.summarisers.TAGStatSummary;
import utilities.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
public class CompositeSummaryLogger implements IStatisticLogger {

    HashMap<String, SummaryLogger> loggerSet;
    File logFile;
    public boolean printToConsole = true;

    public CompositeSummaryLogger()
    {
        loggerSet = new HashMap<>();
    }

    public CompositeSummaryLogger(String logFile) {
        this.logFile = new File(logFile);
    }

    @Override
    public void record(Map<String, ?> data) {
        for(String str : data.keySet())
        {
            if(!loggerSet.containsKey(str))
                loggerSet.put(str, new SummaryLogger());
            loggerSet.get(str).record((Map<String, ?>) data.get(str));
        }
    }


    @Override
    public void record(String key, Object datum) {
        throw new AssertionError("CompositeSummaryLogger requires one hierarchy level for data records");
    }

    @Override
    public void processDataAndFinish() {

        if(printToConsole) for(String cat : loggerSet.keySet())
        {
            System.out.println(cat);
            System.out.println(loggerSet.get(cat));
        }

        if(logFile != null && logFile.exists())
        {
            try {
                for(String cat : loggerSet.keySet())
                {
                    SummaryLogger sl = loggerSet.get(cat);

                    Pair<String, String> data = sl.getFileOutput();
                    FileWriter writer = new FileWriter(logFile, true);
                    writer.write(cat + ":\n");
                    writer.write(data.a); //header
                    writer.write(data.b); //body
                    writer.flush();
                    writer.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void processDataAndNotFinish() {
    }
    @Override
    public Map<String, TAGStatSummary> summary() {
        return null;
    }

    @Override
    public CompositeSummaryLogger emptyCopy(String id) {
        if (logFile == null) return new CompositeSummaryLogger();
        return new CompositeSummaryLogger(logFile.getPath());  // todo include id in filename
    }
}
