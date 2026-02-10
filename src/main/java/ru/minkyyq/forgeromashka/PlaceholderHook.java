package ru.minkyyq.forgeromashka;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class PlaceholderExpansion {
    private final ForgeRomashka plugin;

    public PlaceholderExpansion(ForgeRomashka plugin) {
        this.plugin = plugin;
    }

    public String getIdentifier() {
        return "romashka";
    }

    public String getAuthor() {
        return "YourName";
    }

    public String getVersion() {
        return "1.0";
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        } else if (params.equalsIgnoreCase("balance")) {
            return String.valueOf(this.plugin.getBalance(player.getName()));
        } else {
            if (params.startsWith("nick_top") || params.startsWith("balance_top")) {
                String[] parts = params.split("_top");
                if (parts.length == 2) {
                    try {
                        int rank = Integer.parseInt(parts[1]) - 1;
                        List<Map.Entry<String, Integer>> topPlayers = this.plugin.getTopPlayers();
                        if (rank >= 0 && rank < topPlayers.size()) {
                            Map.Entry<String, Integer> entry = (Map.Entry)topPlayers.get(rank);
                            if (params.startsWith("nick_top")) {
                                return (String)entry.getKey();
                            }

                            if (params.startsWith("balance_top")) {
                                return String.valueOf(entry.getValue());
                            }
                        }
                    } catch (NumberFormatException var7) {
                    }
                }
            }

            return null;
        }
    }
}
