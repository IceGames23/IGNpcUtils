package me.icegames.ignpcutils.managers;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.database.Storage;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.PlayerFilter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NPCManager {

    private final Set<Integer> defaultHiddenNPCs = new HashSet<>();
    // O mapa em memória agora é preenchido pelo banco de dados no login.
    private final Map<UUID, Set<Integer>> shownNPCsPerPlayer = new ConcurrentHashMap<>();
    private final IGNpcUtils plugin;
    private final Storage storage;

    public NPCManager(IGNpcUtils plugin, Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
        loadFromConfig();
    }

    /**
     * Carrega apenas a configuração de NPCs ocultos por padrão do config.yml.
     * Os dados dos jogadores são carregados do banco de dados no momento do login.
     */
    public void loadFromConfig() {
        FileConfiguration config = plugin.getConfig();
        config.set("shown", null); // Remove a seção "shown" antiga para evitar confusão.
        defaultHiddenNPCs.clear();
        defaultHiddenNPCs.addAll(config.getIntegerList("defaultHidden"));
    }

    /**
     * Salva a lista de NPCs ocultos por padrão no config.yml.
     */
    public void saveToConfig() {
        FileConfiguration config = plugin.getConfig();
        config.set("defaultHidden", new ArrayList<>(defaultHiddenNPCs));
        plugin.saveConfig();
    }

    /**
     * Lida com a entrada de um jogador, aplicando as regras de visibilidade de NPC.
     * @param player O jogador que entrou no servidor.
     */
    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();

        // Se o jogador tem a permissão de bypass, mostra todos os NPCs e encerra.
        if (player.hasPermission("npcutils.bypass")) {
            for (int id : defaultHiddenNPCs) {
                NPC npc = CitizensAPI.getNPCRegistry().getById(id);
                if (npc != null) {
                    PlayerFilter filter = npc.getOrAddTrait(PlayerFilter.class);
                    filter.removePlayer(uuid);
                }
            }
            return;
        }

        // Carrega os dados de NPCs visíveis para este jogador do banco de dados.
        Set<Integer> shown = storage.getShownNPCs(uuid);
        shownNPCsPerPlayer.put(uuid, shown); // Armazena em memória

        // Itera sobre todos os NPCs que devem ser ocultos por padrão.
        for (int id : defaultHiddenNPCs) {
            NPC npc = CitizensAPI.getNPCRegistry().getById(id);
            if (npc != null && npc.isSpawned()) {
                PlayerFilter filter = npc.getOrAddTrait(PlayerFilter.class);
                filter.setDenylist();

                if (!shown.contains(id)) {
                    filter.addPlayer(uuid); // Esconde o NPC.
                } else {
                    filter.removePlayer(uuid); // Mostra o NPC.
                }
            }
        }
    }

    /**
     * Lida com a saída de um jogador, limpando seus dados do cache em memória.
     * @param player O jogador que saiu do servidor.
     */
    public void handleQuit(Player player) {
        shownNPCsPerPlayer.remove(player.getUniqueId());
    }

    public boolean addDefaultHidden(int id) {
        return defaultHiddenNPCs.add(id);
    }

    public boolean removeDefaultHidden(int id) {
        return defaultHiddenNPCs.remove(id);
    }

    /**
     * Mostra um NPC para um jogador e salva a alteração no banco de dados.
     * @param id O ID do NPC.
     * @param player O jogador.
     */
    public void showNPCToPlayer(int id, Player player) {
        UUID uuid = player.getUniqueId();

        // Adiciona o ID à lista de NPCs visíveis para o jogador.
        Set<Integer> shown = shownNPCsPerPlayer.computeIfAbsent(uuid, k -> new HashSet<>());
        shown.add(id);

        // Atualiza o banco de dados de forma assíncrona.
        storage.saveShown(uuid, shown);

        // Aplica a mudança de visibilidade no jogo.
        NPC npc = CitizensAPI.getNPCRegistry().getById(id);
        if (npc != null) {
            PlayerFilter filter = npc.getOrAddTrait(PlayerFilter.class);
            filter.removePlayer(uuid);
        }
    }

    /**
     * Oculta um NPC de um jogador e salva a alteração no banco de dados.
     * @param id O ID do NPC.
     * @param player O jogador.
     */
    public void hideNPCFromPlayer(int id, Player player) {
        UUID uuid = player.getUniqueId();

        // Remove o ID da lista de NPCs visíveis para o jogador.
        Set<Integer> shown = shownNPCsPerPlayer.computeIfAbsent(uuid, k -> new HashSet<>());
        shown.remove(id);

        // Atualiza o banco de dados de forma assíncrona.
        storage.saveShown(uuid, shown);

        // Aplica a mudança de visibilidade no jogo.
        NPC npc = CitizensAPI.getNPCRegistry().getById(id);
        if (npc != null) {
            PlayerFilter filter = npc.getOrAddTrait(PlayerFilter.class);
            filter.addPlayer(uuid);
        }
    }

    public Storage getStorage() {
        return storage;
    }
}