===========================================================
 AI-Based Spam Email Detector
 HTML + CSS front end, pure Java back end (no JavaScript)
===========================================================

WHAT IT IS
----------
A small web application that analyses an email subject and message
and reports a spam probability from 0% to 100%, together with a
classification of Spam (red), Suspicious (orange) or Not Spam (green)
and the list of suspicious words / reasons that were detected.

The interface is plain HTML styled with CSS. The HTML <form> posts
directly to a Java HTTP server, so NO JavaScript is used anywhere.
There is no database, no framework and no external library - only the
Java Standard Library (com.sun.net.httpserver, shipped with the JDK).


FILES
-----
index.html          Homepage: navigation bar, description, subject field,
                    message textarea, "Check Email" and "Clear" buttons.
style.css           All styling: dashboard layout, cards, shadows,
                    responsive rules for desktop / tablet / mobile.
SpamDetector.java   Main class. Starts the web server, serves the static
                    files, reads the submitted form, validates the input
                    and renders the result page.
EmailAnalyzer.java  The detection algorithm (keyword scoring + heuristics).
Result.java         Data holder for score, percentage, classification,
                    reasons and detected spam words.
README.txt          This file.


REQUIREMENTS
------------
Java JDK 8 or newer. Check with:
    java -version
    javac -version


HOW TO COMPILE
--------------
Open a terminal in the folder that contains these files and run:

    javac SpamDetector.java EmailAnalyzer.java Result.java

This creates SpamDetector.class, EmailAnalyzer.class and Result.class.


HOW TO RUN
----------
    java SpamDetector

You will see:

    AI-Based Spam Email Detector running at http://localhost:8080

Open that address in any web browser.
To use a different port:

    java SpamDetector 9090

Stop the server with Ctrl + C.


HOW TO USE
----------
1. Type or paste the email subject (optional) and the email message
   (required) on the homepage.
2. Press "Check Email". The browser posts the form to /check.
3. The result page shows:
      - the original subject (safely escaped)
      - the spam probability percentage and a coloured meter
      - the classification: Spam / Suspicious / Not Spam
      - the detected suspicious words
      - the list of reasons behind the score
      - a "Check Another Email" button
4. "Clear" resets the form without sending anything.


HOW THE ALGORITHM WORKS
-----------------------
A transparent, Naive-Bayes-style additive scoring model:

  * Each spam keyword has a weight (e.g. "lottery" = 10, "offer" = 5).
    The first occurrence scores full weight, repeats score half,
    capped at three occurrences so one repeated word cannot dominate.
  * The subject line is counted twice because bait words usually
    appear there.
  * Extra points are added for:
      - excessive capital letters (shouting)
      - too many exclamation marks or "!!!" / "???"
      - suspicious links (shorteners, raw IP addresses, odd domains)
      - many links in one email
      - requests for passwords, OTP, bank / card details or money
      - explicit currency amounts such as $1,000,000
      - a missing subject line
  * The total is normalised against a maximum of 100 points to produce
    the percentage.

  Classification thresholds:
      70% and above  -> Spam        (red)
      40% to 69%     -> Suspicious  (orange)
      below 40%      -> Not Spam    (green)

To tune the detector, edit the SPAM_WORDS map or the thresholds inside
EmailAnalyzer.java and recompile.


SECURITY AND PRIVACY
--------------------
  * The message field is required; an empty or whitespace-only message
    produces a clear error page instead of a result.
  * Input length is limited (subject 200 chars, message 10,000 chars).
  * Every value echoed back to the browser is HTML-escaped
    (&, <, >, ", ') so no injected markup or script can run.
  * Emails are analysed in memory and discarded immediately - nothing is
    written to a file, log or database.


TROUBLESHOOTING
---------------
"Address already in use"  -> another program uses port 8080; run
                             java SpamDetector 9090 and open that port.
"javac: command not found"-> install a JDK (not just a JRE) and make sure
                             its bin folder is on your PATH.
Page has no styling       -> run the program from the folder that contains
                             index.html and style.css.

===========================================================
 AI-Based Spam Email Detector
===========================================================
