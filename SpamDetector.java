import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SpamDetector.java  -  MAIN CLASS
 *
 * Starts a small web server using only the Java Standard Library
 * (com.sun.net.httpserver, included in every JDK).
 *
 *   GET  /            -> serves index.html
 *   GET  /style.css   -> serves the stylesheet
 *   POST /check       -> reads the submitted form, validates it,
 *                        runs EmailAnalyzer and renders the result page
 *
 * The HTML form posts directly to this server, so the application works
 * completely without JavaScript.
 *
 * Compile:  javac SpamDetector.java EmailAnalyzer.java Result.java
 * Run:      java SpamDetector            (or: java SpamDetector 9090)
 */
public class SpamDetector {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int MAX_SUBJECT = 200;
    private static final int MAX_MESSAGE = 10000;

    private static final EmailAnalyzer analyzer = new EmailAnalyzer();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port, falling back to 8080.");
            }
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new StaticHandler());
        server.createContext("/check", new CheckHandler());
        server.setExecutor(null); // single-threaded default executor is enough
        server.start();

        System.out.println("AI-Based Spam Email Detector running at http://localhost:" + port);
        System.out.println("Press Ctrl + C to stop.");
    }

    // ==================================================================
    // Serves index.html and style.css from the current folder
    // ==================================================================
    static class StaticHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                sendFile(exchange, "index.html", "text/html; charset=utf-8");
            } else if (path.equals("/style.css")) {
                sendFile(exchange, "style.css", "text/css; charset=utf-8");
            } else {
                byte[] body = ("<h1>404 - Not found</h1><p><a href=\"/\">Go home</a></p>").getBytes(UTF8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(404, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            }
        }
    }

    // ==================================================================
    // Handles the submitted form and renders the result page
    // ==================================================================
    static class CheckHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Someone opened /check directly - send them back to the form.
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }

            String body = readBody(exchange.getRequestBody());
            Map<String, String> form = parseForm(body);

            String subject = form.containsKey("subject") ? form.get("subject") : "";
            String message = form.containsKey("message") ? form.get("message") : "";

            // ---------- VALIDATION ----------
            if (message == null || message.trim().isEmpty()) {
                sendHtml(exchange, 400, errorPage(
                    "The email message cannot be empty. Please paste the email text and try again."));
                return;
            }
            if (subject.length() > MAX_SUBJECT) {
                sendHtml(exchange, 400, errorPage(
                    "The subject is too long. Please keep it under " + MAX_SUBJECT + " characters."));
                return;
            }
            if (message.length() > MAX_MESSAGE) {
                sendHtml(exchange, 400, errorPage(
                    "The message is too long. Please keep it under " + MAX_MESSAGE + " characters."));
                return;
            }

            // ---------- ANALYSIS ----------
            Result result = analyzer.analyze(subject, message);

            // The email itself is never stored; only this response is produced.
            sendHtml(exchange, 200, resultPage(result));
        }
    }

    // ==================================================================
    // HTML rendering
    // ==================================================================

    /** Builds the coloured result page. */
    private static String resultPage(Result r) {
        Result.Classification c = r.getClassification();

        StringBuilder words = new StringBuilder();
        if (r.getSpamWords().isEmpty()) {
            words.append("<p style=\"color:#5b6b7f;margin:0\">No suspicious words were detected.</p>");
        } else {
            words.append("<div class=\"chip-wrap\">");
            for (String w : r.getSpamWords()) {
                words.append("<span class=\"chip\">").append(escape(w)).append("</span>");
            }
            words.append("</div>");
        }

        StringBuilder reasons = new StringBuilder("<ul class=\"reason-list\">");
        List<String> list = r.getReasons();
        for (String reason : list) {
            reasons.append("<li>").append(escape(reason)).append("</li>");
        }
        reasons.append("</ul>");

        String subjectText = r.getSubject().isEmpty()
                ? "<em style=\"color:#5b6b7f\">(no subject provided)</em>"
                : escape(r.getSubject());

        return page("Analysis Result",
            "<section class=\"result-banner " + c.getBgClass() + "\">"
          +   "<h2>" + escape(c.getLabel().toUpperCase()) + "</h2>"
          +   "<p>Spam probability: " + r.getProbability() + "%</p>"
          + "</section>"

          + "<section class=\"card\">"
          +   "<h2 class=\"card-title\">Summary</h2>"
          +   "<p style=\"margin:0 0 6px;color:#5b6b7f;font-weight:600\">Email Subject</p>"
          +   "<div class=\"subject-box\">" + subjectText + "</div>"

          +   "<p style=\"margin:22px 0 0;color:#5b6b7f;font-weight:600\">Spam Probability</p>"
          +   "<div class=\"score-number " + c.getTextClass() + "\">" + r.getProbability() + "%</div>"
          +   "<div class=\"meter\"><div class=\"meter-fill\" style=\"width:" + r.getProbability()
          +       "%;background:" + c.getColor() + "\"></div></div>"

          +   "<div class=\"detail-row\"><span>Classification</span>"
          +     "<span class=\"" + c.getTextClass() + "\" style=\"font-weight:700\">"
          +       escape(c.getLabel()) + "</span></div>"
          +   "<div class=\"detail-row\"><span>Raw score</span><span>" + r.getSpamScore()
          +     " points</span></div>"
          +   "<div class=\"detail-row\"><span>Indicators found</span><span>"
          +     r.getSpamWords().size() + "</span></div>"
          + "</section>"

          + "<section class=\"card\">"
          +   "<h2 class=\"card-title\">Detected Suspicious Words</h2>"
          +   words
          + "</section>"

          + "<section class=\"card\">"
          +   "<h2 class=\"card-title\">Why This Score?</h2>"
          +   reasons
          +   "<div class=\"button-row\">"
          +     "<a class=\"btn btn-primary\" href=\"/\">Check Another Email</a>"
          +   "</div>"
          + "</section>");
    }

    /** Builds a friendly error page for invalid input. */
    private static String errorPage(String messageText) {
        return page("Invalid Input",
            "<section class=\"card\">"
          +   "<h2 class=\"card-title\">Invalid Input</h2>"
          +   "<div class=\"error-box\">" + escape(messageText) + "</div>"
          +   "<div class=\"button-row\">"
          +     "<a class=\"btn btn-primary\" href=\"/\">Back to the Detector</a>"
          +   "</div>"
          + "</section>");
    }

    /** Shared page shell: navigation bar, content, footer. */
    private static String page(String title, String content) {
        return "<!DOCTYPE html>"
          + "<html lang=\"en\"><head><meta charset=\"UTF-8\">"
          + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
          + "<title>" + escape(title) + " | AI-Based Spam Email Detector</title>"
          + "<link rel=\"stylesheet\" href=\"/style.css\"></head><body>"
          + "<header class=\"navbar\">"
          +   "<div class=\"brand\"><span class=\"logo\">AI</span>"
          +   "<span class=\"brand-text\">Spam Email Detector</span></div>"
          +   "<nav><a href=\"/\">Home</a><a href=\"/#detector\">Spam Detector</a>"
          +   "<a href=\"/#about\">About</a></nav>"
          + "</header>"
          + "<main class=\"container\">" + content + "</main>"
          + "<footer class=\"footer\">AI-Based Spam Email Detector</footer>"
          + "</body></html>";
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /** Escapes HTML so user input can never inject markup (sanitisation). */
    private static String escape(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&':  sb.append("&amp;");  break;
                case '<':  sb.append("&lt;");   break;
                case '>':  sb.append("&gt;");   break;
                case '"':  sb.append("&quot;"); break;
                case '\'': sb.append("&#39;");  break;
                default:   sb.append(ch);
            }
        }
        return sb.toString();
    }

    /** Parses an application/x-www-form-urlencoded request body. */
    private static Map<String, String> parseForm(String body) {
        Map<String, String> map = new HashMap<String, String>();
        if (body == null || body.isEmpty()) return map;
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            try {
                if (eq > 0) {
                    String key = URLDecoder.decode(pair.substring(0, eq), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                    map.put(key, value);
                } else if (!pair.isEmpty()) {
                    map.put(URLDecoder.decode(pair, "UTF-8"), "");
                }
            } catch (Exception ignored) {
                // Malformed field - skip it rather than crashing the server.
            }
        }
        return map;
    }

    private static String readBody(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        return new String(out.toByteArray(), UTF8);
    }

    private static void sendHtml(HttpExchange exchange, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(UTF8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void sendFile(HttpExchange exchange, String name, String type) throws IOException {
        File file = new File(name);
        if (!file.exists()) {
            byte[] msg = ("<h1>Missing file: " + escape(name)
                    + "</h1><p>Run the program from the folder that contains index.html and style.css.</p>")
                    .getBytes(UTF8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(500, msg.length);
            exchange.getResponseBody().write(msg);
            exchange.close();
            return;
        }
        FileInputStream in = new FileInputStream(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        in.close();

        byte[] bytes = out.toByteArray();
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
