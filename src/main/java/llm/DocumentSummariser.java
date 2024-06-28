package llm;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class DocumentSummariser {

    private String documentFullText;

    public DocumentSummariser(String filePath) {

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
                throw new RuntimeException("Error reading PDF file: " + filePath, e);
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
    }

    public String processText() {
        return processText("");
    }

    // Removes page numbers and replaces with new paragraphs instead. Removes line breaks otherwise
    // to avoid artificial PDF line breaks. Caveat: also removes line breaks we might want...
    public String processText(String prompt) {
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
        String ret = result.toString();
        if (ret.startsWith("\n")) {
            ret = ret.substring(1);
        }
        return ret;
    }
}


