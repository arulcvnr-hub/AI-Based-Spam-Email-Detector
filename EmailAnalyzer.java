import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EmailAnalyzer.java
 *
 * Contains the whole spam-detection algorithm. No machine-learning library is
 * used: this is a transparent, Naive-Bayes-style additive scoring model.
 *
 * How it works
 * ------------
 * 1. Every spam indicator found in the email adds a number of points.
 * 2. The subject line counts double, because spammers load it with bait words.
 * 3. The total points are converted into a 0-100% probability.
 * 4. The percentage decides the final label:
 *        >= 70%  -> Spam
 *        40-69%  -> Suspicious
 *        <  40%  -> Not Spam
 */
public class EmailAnalyzer {

    /** Weighted spam vocabulary: word -> points per occurrence. */
    private static final Map<String, Integer> SPAM_WORDS = new LinkedHashMap<String, Integer>();
    static {
        // Very strong indicators
        SPAM_WORDS.put("lottery", 10);
        SPAM_WORDS.put("winner", 10);
        SPAM_WORDS.put("you have won", 12);
        SPAM_WORDS.put("congratulations", 8);
        SPAM_WORDS.put("prize", 9);
        SPAM_WORDS.put("claim", 7);
        SPAM_WORDS.put("click here", 9);
        SPAM_WORDS.put("wire transfer", 10);
        SPAM_WORDS.put("bank account", 10);
        SPAM_WORDS.put("credit card", 9);
        SPAM_WORDS.put("password", 10);
        SPAM_WORDS.put("verify your account", 11);
        SPAM_WORDS.put("send money", 11);

        // Medium indicators
        SPAM_WORDS.put("free", 6);
        SPAM_WORDS.put("offer", 5);
        SPAM_WORDS.put("urgent", 7);
        SPAM_WORDS.put("money", 6);
        SPAM_WORDS.put("cash", 6);
        SPAM_WORDS.put("cheap", 5);
        SPAM_WORDS.put("discount", 4);
        SPAM_WORDS.put("risk free", 6);
        SPAM_WORDS.put("guaranteed", 6);
        SPAM_WORDS.put("act now", 8);
        SPAM_WORDS.put("limited time", 6);
        SPAM_WORDS.put("buy now", 7);
        SPAM_WORDS.put("earn extra", 7);
        SPAM_WORDS.put("work from home", 6);
        SPAM_WORDS.put("investment", 4);
        SPAM_WORDS.put("bitcoin", 6);
        SPAM_WORDS.put("crypto", 5);
        SPAM_WORDS.put("loan", 4);
        SPAM_WORDS.put("viagra", 12);
        SPAM_WORDS.put("no cost", 5);
        SPAM_WORDS.put("100%", 4);
        SPAM_WORDS.put("winner selected", 10);
        SPAM_WORDS.put("unsubscribe", 2);
    }

    /** Words that strongly suggest a request for credentials / money. */
    private static final String[] CREDENTIAL_REQUESTS = {
        "password", "otp", "one time password", "pin number", "cvv",
        "bank account", "account number", "credit card", "debit card",
        "send money", "wire transfer", "western union", "gift card",
        "social security", "aadhaar", "verify your account", "update your details"
    };

    /** Link-shorteners / suspicious domains frequently used in spam. */
    private static final String[] SUSPICIOUS_DOMAINS = {
        "bit.ly", "tinyurl.com", "t.co/", "goo.gl", "ow.ly", "is.gd",
        "rb.gy", "cutt.ly", "shorturl", ".ru/", ".tk/", ".xyz/", ".top/", ".click/"
    };

    private static final Pattern URL_PATTERN =
        Pattern.compile("(https?://|www\\.)[^\\s<>\"]+", Pattern.CASE_INSENSITIVE);

    /** Maximum score used to normalise the result to a percentage. */
    private static final int MAX_SCORE = 100;

    /**
     * Analyzes one email and returns a Result object.
     *
     * @param subject email subject (may be empty)
     * @param message email body (must not be empty - validated by the caller)
     */
    public Result analyze(String subject, String message) {
        String safeSubject = subject == null ? "" : subject.trim();
        String safeMessage = message == null ? "" : message.trim();

        // Subject is weighted twice, so bait words in the subject matter more.
        String combined = (safeSubject + " " + safeSubject + " " + safeMessage);
        String lower = combined.toLowerCase();

        int score = 0;
        List<String> reasons = new ArrayList<String>();
        Set<String> foundWords = new LinkedHashSet<String>();

        // ---------- 1. Keyword scoring (with repetition awareness) ----------
        int keywordPoints = 0;
        for (Map.Entry<String, Integer> entry : SPAM_WORDS.entrySet()) {
            String word = entry.getKey();
            int weight = entry.getValue();
            int count = countOccurrences(lower, word);

            if (count > 0) {
                foundWords.add(word);
                // First hit counts fully; repeats count half (diminishing returns),
                // capped at 3 occurrences so one repeated word cannot dominate.
                int effective = Math.min(count, 3);
                keywordPoints += weight + (effective - 1) * (weight / 2);

                if (count >= 3) {
                    reasons.add("The word \"" + word + "\" is repeated " + count
                            + " times, which is typical of bulk spam.");
                }
            }
        }
        if (keywordPoints > 0) {
            score += keywordPoints;
            reasons.add("Found " + foundWords.size()
                    + " known spam keyword(s) worth " + keywordPoints + " points.");
        }

        // ---------- 2. Excessive capital letters ----------
        double capsRatio = capitalRatio(safeSubject + " " + safeMessage);
        if (capsRatio > 0.55) {
            score += 15;
            reasons.add("Over " + percent(capsRatio)
                    + "% of the letters are capitals - the email is \"shouting\".");
        } else if (capsRatio > 0.35) {
            score += 8;
            reasons.add("A high share of capital letters (" + percent(capsRatio) + "%).");
        }
        if (isAllCaps(safeSubject)) {
            score += 8;
            reasons.add("The subject line is written entirely in capital letters.");
        }

        // ---------- 3. Too many exclamation marks ----------
        int bangs = countChar(combined, '!');
        if (bangs >= 6) {
            score += 12;
            reasons.add("Contains " + bangs + " exclamation marks.");
        } else if (bangs >= 3) {
            score += 6;
            reasons.add("Contains " + bangs + " exclamation marks.");
        }
        if (combined.contains("!!!") || combined.contains("???")) {
            score += 5;
            reasons.add("Uses repeated punctuation such as \"!!!\" or \"???\".");
        }

        // ---------- 4. Suspicious links ----------
        Matcher urlMatcher = URL_PATTERN.matcher(safeMessage);
        int urlCount = 0;
        boolean shady = false;
        while (urlMatcher.find()) {
            urlCount++;
            String url = urlMatcher.group().toLowerCase();
            for (String domain : SUSPICIOUS_DOMAINS) {
                if (url.contains(domain)) {
                    shady = true;
                    foundWords.add(domain);
                }
            }
            // Raw IP address links, e.g. http://192.168.1.1/login
            if (url.matches(".*//\\d{1,3}(\\.\\d{1,3}){3}.*")) {
                shady = true;
                reasons.add("A link points to a raw IP address instead of a domain name.");
            }
        }
        if (shady) {
            score += 15;
            reasons.add("Contains shortened or untrusted links often used in phishing.");
        }
        if (urlCount >= 4) {
            score += 8;
            reasons.add("Contains " + urlCount + " links, far more than a normal email.");
        } else if (urlCount > 0) {
            reasons.add("Contains " + urlCount + " link(s).");
        }

        // ---------- 5. Requests for passwords, bank details or money ----------
        List<String> credentialHits = new ArrayList<String>();
        for (String phrase : CREDENTIAL_REQUESTS) {
            if (lower.contains(phrase)) {
                credentialHits.add(phrase);
                foundWords.add(phrase);
            }
        }
        if (!credentialHits.isEmpty()) {
            score += 12 * Math.min(credentialHits.size(), 3);
            reasons.add("Asks for sensitive information: " + join(credentialHits) + ".");
        }

        // ---------- 6. Currency amounts, e.g. $1,000,000 ----------
        if (Pattern.compile("[$€£₹]\\s?\\d").matcher(combined).find()) {
            score += 8;
            reasons.add("Mentions a money amount, a classic reward/scam hook.");
        }

        // ---------- 7. Missing subject ----------
        if (safeSubject.isEmpty()) {
            score += 4;
            reasons.add("The email has no subject line.");
        }

        // ---------- Normalise to a 0-100% probability ----------
        int probability = (int) Math.round((score * 100.0) / MAX_SCORE);
        if (probability > 100) probability = 100;
        if (probability < 0) probability = 0;

        // ---------- Final classification ----------
        Result.Classification classification;
        if (probability >= 70) {
            classification = Result.Classification.SPAM;
        } else if (probability >= 40) {
            classification = Result.Classification.SUSPICIOUS;
        } else {
            classification = Result.Classification.NOT_SPAM;
        }

        if (reasons.isEmpty()) {
            reasons.add("No spam indicators were detected in this email.");
        }

        return new Result(safeSubject, score, probability, classification,
                          reasons, new ArrayList<String>(foundWords));
    }

    // ------------------------------------------------------------------
    // Small helper methods
    // ------------------------------------------------------------------

    /** Counts how many times a phrase appears inside the text. */
    private int countOccurrences(String text, String phrase) {
        int count = 0;
        int index = text.indexOf(phrase);
        while (index >= 0) {
            count++;
            index = text.indexOf(phrase, index + phrase.length());
        }
        return count;
    }

    /** Counts a single character inside the text. */
    private int countChar(String text, char c) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == c) count++;
        }
        return count;
    }

    /** Ratio of uppercase letters to all letters (0.0 - 1.0). */
    private double capitalRatio(String text) {
        int letters = 0, upper = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) upper++;
            }
        }
        if (letters < 12) return 0.0; // too short to judge
        return (double) upper / letters;
    }

    /** True when a non-trivial string is entirely uppercase. */
    private boolean isAllCaps(String text) {
        String letters = text.replaceAll("[^A-Za-z]", "");
        return letters.length() >= 6 && letters.equals(letters.toUpperCase());
    }

    private int percent(double ratio) {
        return (int) Math.round(ratio * 100);
    }

    private String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}
