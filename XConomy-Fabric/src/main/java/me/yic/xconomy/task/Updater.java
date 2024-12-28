package me.yic.xconomy.task;

import me.yic.xconomy.XConomyFabric;
import me.yic.xconomy.lang.MessagesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Updater implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("xconomy");
    private static final String VERSION_URL = "https://api.github.com/repos/YiC200333/XConomy/releases/latest";

    @Override
    public void run() {
        try {
            URL url = new URL(VERSION_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String latestVersion = extractVersion(response.toString());
                if (latestVersion != null && !latestVersion.equals(XConomyFabric.PVersion)) {
                    LOGGER.info(MessagesManager.systemMessage("new-version").replace("%version%", latestVersion));
                }
            }
        } catch (Exception e) {
            LOGGER.error(MessagesManager.systemMessage("update-check-failed"));
        }
    }

    private String extractVersion(String json) {
        // Simple version extraction from GitHub API response
        int tagStart = json.indexOf("\"tag_name\":\"");
        if (tagStart != -1) {
            tagStart += 12;
            int tagEnd = json.indexOf("\"", tagStart);
            if (tagEnd != -1) {
                return json.substring(tagStart, tagEnd);
            }
        }
        return null;
    }
}