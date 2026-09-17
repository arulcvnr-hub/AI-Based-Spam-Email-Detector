import java.util.ArrayList;
import java.util.List;

/**
 * Result.java
 *
 * Simple data holder (a "model" class) that carries the outcome of the
 * spam analysis from EmailAnalyzer back to SpamDetector, which renders it
 * as HTML. It stores no data permanently - it only lives for one request.
 */
public class Result {

    /** The three possible verdicts for an email. */
    public enum Classification {
        SPAM("Spam", "bg-spam", "text-spam", "#c62828"),
        SUSPICIOUS("Suspicious", "bg-suspicious", "text-suspicious", "#ef6c00"),
        NOT_SPAM("Not Spam", "bg-safe", "text-safe", "#2e7d32");

        private final String label;
        private final String bgClass;
        private final String textClass;
        private final String color;

        Classification(String label, String bgClass, String textClass, String color) {
            this.label = label;
            this.bgClass = bgClass;
            this.textClass = textClass;
            this.color = color;
        }

        public String getLabel()     { return label; }
        public String getBgClass()   { return bgClass; }   // CSS class for the banner
        public String getTextClass() { return textClass; } // CSS class for text
        public String getColor()     { return color; }     // raw colour for the meter
    }

    private final String subject;              // original (raw) subject
    private final int spamScore;               // raw points collected
    private final int probability;             // 0 - 100 %
    private final Classification classification;
    private final List<String> reasons;        // human readable explanations
    private final List<String> spamWords;      // spam keywords that were found

    public Result(String subject,
                  int spamScore,
                  int probability,
                  Classification classification,
                  List<String> reasons,
                  List<String> spamWords) {
        this.subject = subject;
        this.spamScore = spamScore;
        this.probability = probability;
        this.classification = classification;
        this.reasons = reasons == null ? new ArrayList<String>() : reasons;
        this.spamWords = spamWords == null ? new ArrayList<String>() : spamWords;
    }

    public String getSubject()                 { return subject; }
    public int getSpamScore()                  { return spamScore; }
    public int getProbability()                { return probability; }
    public Classification getClassification()  { return classification; }
    public List<String> getReasons()           { return reasons; }
    public List<String> getSpamWords()         { return spamWords; }
}
