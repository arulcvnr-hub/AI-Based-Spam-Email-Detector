AI-BASED SPAM EMAIL DETECTOR
=============================

1. PROJECT DESCRIPTION
----------------------

AI-Based Spam Email Detector is a web application that analyzes email
content and identifies whether an email is Spam, Not Spam, or Suspicious.

The project uses:

- HTML for the webpage structure
- CSS for styling and responsive design
- Java for backend processing and spam detection
- No JavaScript, Python, PHP, external APIs, or external libraries

The application checks the email subject and message content for common spam
indicators and calculates a spam probability score.

2. FEATURES
-----------

- Simple and user-friendly interface
- Email subject and message input
- Spam detection using Java
- Spam probability score from 0% to 100%
- Classification into:
  - Spam
  - Not Spam
  - Suspicious
- Detection of common spam keywords
- Detection of suspicious links
- Detection of excessive capital letters
- Detection of repeated exclamation marks
- Detection of requests for money or personal information
- Responsive HTML and CSS design
- Input validation
- Displays the reasons why an email was classified as spam
- Does not permanently store email content

3. TECHNOLOGIES USED
--------------------

Frontend:
- HTML5
- CSS3

Backend:
- Java

Detection Method:
- Keyword-based spam scoring algorithm
- Rule-based email analysis

4. PROJECT STRUCTURE
--------------------

AI-Spam-Email-Detector/
|
|-- index.html
|-- style.css
|-- SpamDetector.java
|-- EmailAnalyzer.java
|-- Result.java
|-- README.txt

5. HOW THE SYSTEM WORKS
-----------------------

The user enters an email subject and message into the form.

The Java backend analyzes the email using different rules:

- Spam keywords increase the spam score
- Suspicious links increase the spam score
- Excessive capital letters increase the spam score
- Too many exclamation marks increase the spam score
- Requests for passwords, bank details, or money increase the spam score
- Repeated spam words increase the spam score

After analysis, the system calculates a final score.

Classification rules:

- 0% to 29%: Not Spam
- 30% to 59%: Suspicious
- 60% to 100%: Spam

6. REQUIREMENTS
---------------

To run this project, you need:

- Java Development Kit (JDK) 8 or higher
- A web browser such as Google Chrome, Microsoft Edge, or Firefox
- A text editor or Java IDE such as:
  - Visual Studio Code
  - IntelliJ IDEA
  - Eclipse
  - NetBeans

7. INSTALLATION
---------------

Step 1: Install Java

Download and install the Java Development Kit from:

https://www.oracle.com/java/technologies/downloads/

Verify the installation by opening Command Prompt or Terminal and typing:

java -version

javac -version

Step 2: Download or copy the project files

Place all project files inside one folder named:

AI-Spam-Email-Detector

Step 3: Open the project folder in a Java IDE or terminal.

8. COMPILATION
--------------

Open a terminal inside the project folder and compile the Java files:

javac SpamDetector.java EmailAnalyzer.java Result.java

If the project uses packages, compile the files according to the package
structure.

9. RUNNING THE APPLICATION
--------------------------

Run the Java backend using:

java SpamDetector

The application will start the local server.

Open a web browser and visit the local address shown by the Java program.
For example:

http://localhost:8080

The exact port number may be different depending on the Java configuration.

10. HOW TO USE
--------------

1. Open the application in a web browser.
2. Enter the email subject.
3. Paste or type the email message.
4. Click the "Check Email" button.
5. View the spam probability score.
6. View the classification result.
7. Read the detected spam indicators.
8. Click "Check Another Email" to analyze another message.

11. SAMPLE TEST EMAIL
---------------------

Subject:
Congratulations! You Won a Free Prize

Message:
Congratulations! You are the lucky winner of a free prize.
Click here immediately to claim your money. Send your bank details
and password to receive the reward.

Expected Result:

Classification: Spam

Possible Reasons:

- Contains the word "Congratulations"
- Contains the word "Free"
- Contains the word "Prize"
- Contains an urgent request
- Requests bank details
- Contains suspicious instructions

12. LIMITATIONS
---------------

- This project uses a rule-based spam detection method.
- It does not use a real trained machine learning model.
- Some legitimate emails may be incorrectly classified as spam.
- Some spam emails may not be detected if they do not contain common
  spam indicators.
- The system does not connect to an external email service.
- Email data is not permanently saved.

13. FUTURE ENHANCEMENTS
-----------------------

The project can be improved by adding:

- A trained machine learning model
- A database for storing analysis history
- User login and registration
- Email file upload support
- Advanced natural language processing
- Improved link and attachment detection
- Admin dashboard
- Detection accuracy reports
- Support for multiple languages

14. SECURITY
-----------

- Do not enter real passwords, bank details, or private information.
- Email messages should only be used for testing.
- User input should be validated before processing.
- Email content should not be stored without user permission.
- The application should sanitize content before displaying it.

15. PROJECT PURPOSE
-------------------

The purpose of this project is to demonstrate how an email spam detector
can be created using HTML, CSS, and Java. It helps users understand basic
email classification, rule-based analysis, and backend form processing.

16. AUTHOR
----------

Project Name: AI-Based Spam Email Detector

Developed using:
HTML, CSS, and Java
