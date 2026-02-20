package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.managers.SkinManager;
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
    private final SkinManager skinManager;

    public SkinCommand(IGNpcUtils plugin, NPCResolver resolver, SkinManager skinManager) {
        this.plugin = plugin;
        this.resolver = resolver;
        this.skinManager = skinManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Usage: /npcutils skin <set|save|npc|alias|group> ...
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_skin"));
            return;
        }

        String sub = args[1].toLowerCase();

        if (sub.equals("save")) {
            handleSave(sender, args);
        } else if (sub.equals("set")) {
            handleSet(sender, args);
        } else if (sub.equals("list")) {
            handleList(sender);
        } else if (sub.equals("remove")) {
            handleRemove(sender, args);
        } else if (sub.equals("update")) {
            handleUpdate(sender, args);
        } else {
            // Legacy/Fallback command
            handleLegacy(sender, args);
        }
    }

    private void handleSave(CommandSender sender, String[] args) {
        // /npcutils skin save <name> <url>
        if (args.length < 4) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_skin_save"));
            return;
        }
        String name = args[2];
        String url = args[3];

        SubCommand.sendMessage(sender,
                MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_url_loading"),
                false);

        fetchSkinData(url).thenAccept(skinData -> {
            if (skinData != null) {
                skinManager.saveSkin(name, skinData.getValue(), skinData.getSignature(), url);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    SubCommand.sendMessage(sender,
                            MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_saved", "%name%", name),
                            false);
                });
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    SubCommand.sendMessage(sender,
                            MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_url_error"),
                            false);
                });
            }
        });
    }

    private void handleSet(CommandSender sender, String[] args) {
        // /npcutils skin set <template/URL> [target]
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_skin_set"));
            return;
        }

        String skinReference = args[2];
        String targetReference = args.length > 3 ? args[3] : null;
        boolean silent = SubCommand.isSilent(args);

        List<Integer> npcIds;
        if (targetReference != null) {
            npcIds = resolver.resolve(targetReference);
        } else {
            NPC selected = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
            if (selected != null) {
                npcIds = List.of(selected.getId());
            } else {
                sender.sendMessage(
                        MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_not_found", "%id%", "selection"));
                return;
            }
        }

        if (npcIds.isEmpty()) {
            SubCommand.sendMessage(sender,
                    MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_npc_reference", "%ref%",
                            targetReference != null ? targetReference : "selection"),
                    silent);
            return;
        }

        if (skinManager.hasSkin(skinReference)) {
            SkinManager.SkinData skin = skinManager.getSkin(skinReference);
            applySkinData(npcIds, skin, skinReference, sender, silent);
        } else if (skinReference.startsWith("http")) {
            // It's a URL
            SubCommand.sendMessage(sender,
                    MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_url_loading"),
                    silent);
            fetchSkinData(skinReference).thenAccept(skinData -> {
                if (skinData != null) {
                    applySkinData(npcIds, skinData, skinReference, sender, silent);
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        SubCommand.sendMessage(sender,
                                MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_url_error"),
                                silent);
                    });
                }
            });
        } else {
            SubCommand.sendMessage(sender,
                    MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_template_not_found", "%name%",
                            skinReference),
                    silent);
        }
    }

    private void handleList(CommandSender sender) {
        java.util.Set<String> skins = skinManager.getSkins();
        if (skins.isEmpty()) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_list_empty"));
        } else {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_list_header"));
            for (String skin : skins) {
                sender.sendMessage(
                        MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_list_item", "%name%", skin));
            }
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_skin_remove"));
            return;
        }
        String name = args[2];
        if (!skinManager.hasSkin(name)) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_not_found", "%name%", name));
            return;
        }
        skinManager.removeSkin(name);
        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_removed", "%name%", name));
    }

    private void handleUpdate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_skin_update"));
            return;
        }
        String name = args[2];
        String url = args[3];

        if (!skinManager.hasSkin(name)) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_not_found", "%name%", name));
            return;
        }

        SubCommand.sendMessage(sender,
                MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_url_loading"),
                false);

        fetchSkinData(url).thenAccept(skinData -> {
            if (skinData != null) {
                skinManager.saveSkin(name, skinData.getValue(), skinData.getSignature(), url);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    SubCommand.sendMessage(sender,
                            MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_saved", "%name%", name),
                            false);
                });
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    SubCommand.sendMessage(sender,
                            MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_url_error"),
                            false);
                });
            }
        });
    }

    private void applySkinData(List<Integer> npcIds, SkinManager.SkinData result, String skinName, CommandSender sender,
            boolean silent) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int npcId : npcIds) {
                NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
                if (npc != null) {
                    SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
                    skinTrait.setTexture(result.getValue(), result.getSignature());
                }
            }
            if (npcIds.size() == 1) {
                SubCommand.sendMessage(sender,
                        MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_updated",
                                "%npc%", npcIds.get(0).toString(), "%skin%", skinName),
                        silent);
            } else {
                SubCommand.sendMessage(sender,
                        MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_updated_group",
                                "%count%", String.valueOf(npcIds.size())),
                        silent);
            }
        });
    }

    private void handleLegacy(CommandSender sender, String[] args) {
        // Original arguments: /npcutils skin <npc|alias|group:name> <skinName|URL>
        // [silent:true]
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
            for (int npcId : npcIds) {
                NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
                if (npc == null) {
                    continue;
                }

                if (skinName.startsWith("http")) {
                    // URL skin
                    fetchSkinData(skinName).thenAccept(skinData -> {
                        if (skinData != null) {
                            applySkinData(List.of(npcId), skinData, skinName, sender, silent);
                        } else {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                SubCommand.sendMessage(sender,
                                        MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_url_error"),
                                        silent);
                            });
                        }
                    });
                    // Note: logic slightly changed from strictly separating successCount. Async
                    // makes this harder to count in bulk.
                    // But for legacy compatibility, we should probably stick closer to original or
                    // accept async nature.
                    // The original code was waiting. Let's assume singular updates for URL in
                    // legacy.
                } else {
                    // Named skin
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
                        skinTrait.setSkinName(skinName);
                    });
                }
            }

            // Re-implementing original feedback logic for legacy is tricky with async
            // fetch.
            // Simplified feedback:
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!skinName.startsWith("http")) { // URL feedback is handled in callback
                    if (npcIds.size() == 1) {
                        SubCommand.sendMessage(sender,
                                MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_updated",
                                        "%npc%", npcIds.get(0).toString(), "%skin%", skinName),
                                silent);
                    } else {
                        SubCommand.sendMessage(sender,
                                MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_updated_group",
                                        "%count%", String.valueOf(npcIds.size())),
                                silent);
                    }
                } else {
                    SubCommand.sendMessage(sender,
                            MessageUtil.getMessage(plugin.getMessagesConfig(), "skin_url_loading"),
                            silent);
                }
            });
        });

    }

    private CompletableFuture<SkinManager.SkinData> fetchSkinData(String urlString) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://api.mineskin.org/generate/url");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "IGNpcUtils");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);

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
                        return new SkinManager.SkinData(value, signature, urlString);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to fetch skin from URL: " + urlString);
            }
            return null;
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
