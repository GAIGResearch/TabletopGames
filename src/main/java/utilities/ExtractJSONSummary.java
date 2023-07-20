package utilities;
import java.util.*;
import org.json.simple.JSONObject;

public class ExtractJSONSummary {
    public static void main(String[] args) {
        // the first arg is the output format (jsonToTxt, or txtToJson)
        // the second arg is the directory for JSON files
        // the third arg is the name of the summary text file
        if (args[0].equalsIgnoreCase("jsonToTxt")) {
            Map<String, JSONObject> stuff = JSONUtils.loadJSONObjectsFromDirectory(args[1], true, "");
            JSONUtils.writeSummaryOfJSONFiles(stuff, args[2]);
        } else if (args[0].equalsIgnoreCase("txtToJson")) {
            JSONUtils.writeJSONFilesFromSummary(args[2], args[1]);
        } else {
            System.out.println("Invalid first argument: " + args[0] + ". Must be jsonToTxt or txtToJson");
        }
    }
}
