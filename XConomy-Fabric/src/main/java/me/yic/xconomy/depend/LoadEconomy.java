package me.yic.xconomy.depend;

import me.yic.xconomy.AdapterManager;
import me.yic.xconomy.lang.MessagesManager;
import me.yic.xconomy.data.DataLink;
import me.yic.xconomy.data.DataFormat;
import me.yic.xconomy.data.sql.SQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadEconomy {
    private static final Logger LOGGER = LoggerFactory.getLogger("xconomy");

    public static boolean load() {
        try {
            // Initialize data connection
            if (!DataLink.create()) {
                LOGGER.error(MessagesManager.systemMessage("database-connection-failed"));
                return false;
            }

            // Initialize data format
            DataFormat.load();

            LOGGER.info(MessagesManager.systemMessage("economy-loaded"));
            return true;
        } catch (Exception e) {
            LOGGER.error(MessagesManager.systemMessage("economy-failed"));
            e.printStackTrace();
            return false;
        }
    }

    public static void unload() {
        try {
            // Save and close data connection
            SQL.close();
        } catch (Exception e) {
            LOGGER.error("Error while unloading economy", e);
        }
    }
}