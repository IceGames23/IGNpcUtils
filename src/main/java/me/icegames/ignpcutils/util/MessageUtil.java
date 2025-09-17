package me.icegames.ignpcutils.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class MessageUtil {
    public static String getMessage(FileConfiguration messageConfig, String path, String... placeholders) {
        Object messageObj = messageConfig.get(path);
        String message;
        String prefix = messageConfig.getString("prefix", "");

        if (messageObj instanceof String) {
            message = (String) messageObj;
        } else if (messageObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> messageList = (List<String>) messageObj;
            message = String.join("\n", messageList);
        } else {
            message = "§cMessage '" + path + "' not found in messages.yml.";
            return message;
        }

        for (int i = 0; i < placeholders.length; i += 2) {
            String key = placeholders[i];
            String value = (i + 1 < placeholders.length && placeholders[i + 1] != null) ? placeholders[i + 1] : "";
            message = message.replace(key, value);
        }

        String finalMessage = prefix + message;
        return finalMessage.replace("&", "§");
    }
}
