package us.ajg0702.antixray;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebhookSender {
    public static void send(Logger logger, String endpoint, String message) {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; utf-8");

            String jsonInputString = "{" +
                    "\"content\":\"" + message.replaceAll("\\\"", "\\\\\"") + "\"" +
                    "}";

            // Write the JSON data to the output stream
            try(OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if(responseCode != 200 && responseCode != 204) {
                logger.warning("Discord webhook failed: " + responseCode + " " + connection.getResponseMessage());
                if(responseCode == 400) {
                    logger.info("Got bad response! Here is the request we sent: " + jsonInputString);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error when sending discord webhook:", e);
        }
    }
}
