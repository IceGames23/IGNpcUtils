package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.util.MessageUtil;
import me.icegames.ignpcutils.util.NPCResolver;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SkinCommand implements SubCommand {

    private final IGNpcUtils plugin;
    private final NPCResolver resolver;

    public SkinCommand(IGNpcUtils plugin, NPCResolver resolver) {
        this.plugin = plugin;
        this.resolver = resolver;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Usage: /npcutils skin <npc|alias|group:name> <skinName|URL> [silent:true]
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_skin"));
            return;
        }

        String reference = args[1];
        String skinName = args[2];
        boolean silent = SubCommand.isSilent(args);

        // Resolve NPC reference
        List<Integer> npcIds = resolver.resolve(reference);
        if (npcIds.isEmpty()) {
            SubCommand.sendMessage(sender,
                    MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_npc_reference", "%ref%", reference),
                    silent);
            return;
        }

        // Apply skin to all resolved NPCs
        CompletableFuture.runAsync(() -> {
            int successCount = 0;

            for (int npcId : npcIds) {
                NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
                if (npc == null) {
                    continue;
                }

                if (skinName.startsWith("http")) {
                    // URL skin
                    applySkinFromUrl(npc, skinName, silent, sender);
                    successCount++;
                } else {
                    // Named skin
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
                        skinTrait.setSkinName(skinName);
                    });
                    successCount++;
                }
            }

            final int count = successCount;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (count == 1) {
                    SubCommand.sendMessage(sender,
                            MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_updated",
                                    "%npc%", npcIds.get(0).toString(), "%skin%", skinName),
                            silent);
                } else {
                    SubCommand.sendMessage(sender,
                            MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_updated_group",
                                    "%count%", String.valueOf(count)),
                            silent);
                }
            });
        });
    }

    private void applySkinFromUrl(NPC npc, String urlString, boolean silent, CommandSender sender) {
        SubCommand.sendMessage(sender,
                MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_url_loading"),
                silent);

        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://api.mineskin.org/generate/url");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "IGNpcUtils");
                // v2.0.1: Add timeouts to prevent server freeze
                conn.setConnectTimeout(5000); // 5 seconds
                conn.setReadTimeout(10000); // 10 seconds

                String jsonInputString = "{\"url\": \"" + urlString + "\"}";
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    String json = response.toString();
                    String value = extractJsonValue(json, "value");
                    String signature = extractJsonValue(json, "signature");

                    if (value != null && signature != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
                            skinTrait.setTexture(value, signature);
                        });
                    }
                } else {
                    // v2.0.1: Notify user on HTTP error
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        SubCommand.sendMessage(sender,
                                MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_url_error"),
                                silent);
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load skin from URL: " + urlString);
                // v2.0.1: Notify user on exception
                Bukkit.getScheduler().runTask(plugin, () -> {
                    SubCommand.sendMessage(sender,
                            MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_url_error"),
                            silent);
                });
            }
        });
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1)
            return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1)
            return null;
        return json.substring(start, end);
    }

    @Override
    public String getName() {
        return "skin";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
