package llm;

import games.GameType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class DocumentSummariser {

    private String documentProcessedText;

    public DocumentSummariser(String filePath) {

        String documentFullText;
        if (filePath.endsWith("pdf")) {
            try {
                // Load the PDF document
                PDDocument document = PDDocument.load(new File(filePath));

                // Instantiate PDFTextStripper to extract text
                PDFTextStripper pdfStripper = new PDFTextStripper();

                // Retrieve text from the PDF
                documentFullText = pdfStripper.getText(document);
                // Close the document
                document.close();
            } catch (IOException e) {
                throw new IllegalArgumentException("Error reading PDF file: " + filePath, e);
            }
        } else {
            // Read in with Scanner
            Scanner scanner = new Scanner(filePath);
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append("\n");
            }
            documentFullText = sb.toString();
        }

        // Process for line breaks

        StringBuilder result = new StringBuilder();
        String[] lines = documentFullText.split("\r?\n");
        Pattern numberPattern = Pattern.compile("\\d+");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (numberPattern.matcher(line).matches()) {
                // If the line contains only a number, keep the line break only
                result.append("\n");
            } else {
                // If the line does not contain only a number, remove the line break
                result.append(line);
                // Check if the next line exists and is not a number, if so, add a space
                if (i + 1 < lines.length && !numberPattern.matcher(lines[i + 1].trim()).matches()) {
                    result.append(" ");
                }
            }
        }
        documentProcessedText = result.toString();
        if (documentProcessedText.startsWith("\n")) {
            documentProcessedText = documentProcessedText.substring(1);
        }
    }

    public String processText() {
        return processText("Game Rules", 500);
    }

    // Removes page numbers and replaces with new paragraphs instead. Removes line breaks otherwise
    // to avoid artificial PDF line breaks. Caveat: also removes line breaks we might want...
    public String processText(String areaOfInterest, int wordLimit) {

        String queryPrompt = String.format("""
                Does this section of the document contain information about %s?
                Rate this section from 1 to 5, where 1 is not at all and 5 is very much.
                Just return a single number between 1 and 5.
                Do not include any explanation.
                """, areaOfInterest);

        String summaryPrompt = String.format("""
                Summarise the information about %s in this text in 1-2 sentences.
                Be concise and clear.
                """, areaOfInterest);

        String finalSummary = String.format("""
                Summarise the information about %s below in no more than %d words.
                This summary will be used by a developer to implement the game.
                Be concise and clear.
                """, areaOfInterest, wordLimit);

        LLMAccess llm = new LLMAccess(LLMAccess.LLM_MODEL.MISTRAL, LLMAccess.LLM_SIZE.SMALL, "RulesSummary_LLM_Log.txt");

        int charactersPerRequest = 2500;
        int characterOverlap = 500;
        int totalLength = documentProcessedText.length();
        int start = 0;
        StringBuilder summary = new StringBuilder();
        while (start < totalLength) {
            int end = start + charactersPerRequest;
            String text = documentProcessedText.substring(start, Math.min(end, totalLength));
            String prompt = queryPrompt + "\n" + text;
            String response = llm.getResponse(prompt);
            int responseInt = 0;
            try {
                responseInt = Integer.parseInt(response.trim().substring(0, 1));
            } catch (NumberFormatException e) {
                System.out.println("Invalid response: " + response);
                responseInt = 1;
            }
            if (responseInt >= 3) {
                response = llm.getResponse(summaryPrompt + "\n" + text);
                summary.append(response);
            }
            start = end - characterOverlap;
        }

        // Now ask for the final summary
        return llm.getResponse(finalSummary + "\n" + summary);
    }

    public static void main(String[] args) {
        DocumentSummariser summariser = new DocumentSummariser("data/loveletter/rulebook.pdf");
        System.out.println(summariser.processText());

        System.out.println(GameType.LoveLetter.loadRulebook());
    }
}


