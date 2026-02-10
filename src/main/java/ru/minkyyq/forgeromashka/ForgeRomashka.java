package ru.minkyyq.forgeromashka;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class ForgeRomashka extends JavaPlugin implements TabCompleter {
    private File playersFile;
    private FileConfiguration playersConfig;
    private int maxBalance;
    private int startingBalance;
    private String currencyName;

    public void onEnable() {
        this.saveDefaultConfig();
        this.setupPlayerData();
        this.loadConfigValues();
        this.getCommand("rom").setExecutor(this);
        this.getCommand("rom").setTabCompleter(this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            (new PlaceholderHook(this)).register();
        }

        this.getLogger().info("PmRomashka enabled!");
    }

    public void onDisable() {
        this.savePlayerData();
        this.getLogger().info("PmRomashka disabled!");
    }

    private void loadConfigValues() {
        this.maxBalance = this.getConfig().getInt("settings.max_balance", Integer.MAX_VALUE);
        this.startingBalance = this.getConfig().getInt("settings.starting_balance", 0);
        this.currencyName = this.getConfig().getString("settings.currency_name", "Ромашки");
    }

    private void setupPlayerData() {
        this.playersFile = new File(this.getDataFolder(), "players.yml");
        if (!this.playersFile.exists()) {
            try {
                this.playersFile.createNewFile();
            } catch (IOException var2) {
                var2.printStackTrace();
            }
        }

        this.playersConfig = YamlConfiguration.loadConfiguration(this.playersFile);
    }

    private void savePlayerData() {
        try {
            this.playersConfig.save(this.playersFile);
        } catch (IOException var2) {
            var2.printStackTrace();
        }

    }

    public int getBalance(String playerName) {
        return this.playersConfig.getInt(playerName + ".balance", this.startingBalance);
    }

    public void setBalance(String playerName, int amount) {
        if (amount > this.maxBalance) {
            amount = this.maxBalance;
        }

        this.playersConfig.set(playerName + ".balance", amount);
        this.savePlayerData();
    }

    public void addBalance(String playerName, int amount) {
        this.setBalance(playerName, this.getBalance(playerName) + amount);
    }

    public void takeBalance(String playerName, int amount) {
        this.setBalance(playerName, Math.max(0, this.getBalance(playerName) - amount));
    }

    List<Entry<String, Integer>> getTopPlayers() {
        Map<String, Integer> balances = new HashMap();
        Iterator var2 = this.playersConfig.getKeys(false).iterator();

        while(var2.hasNext()) {
            String key = (String)var2.next();
            balances.put(key, this.playersConfig.getInt(key + ".balance", this.startingBalance));
        }

        return (List)balances.entrySet().stream().sorted((a, b) -> {
            return Integer.compare((Integer)b.getValue(), (Integer)a.getValue());
        }).limit((long)this.getConfig().getInt("top.limit", 10)).collect(Collectors.toList());
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 0 && !args[0].equalsIgnoreCase("help")) {
            String var5 = args[0].toLowerCase();
            byte var6 = -1;
            switch(var5.hashCode()) {
                case -934641255:
                    if (var5.equals("reload")) {
                        var6 = 6;
                    }
                    break;
                case -339185956:
                    if (var5.equals("balance")) {
                        var6 = 3;
                    }
                    break;
                case 113762:
                    if (var5.equals("set")) {
                        var6 = 2;
                    }
                    break;
                case 3173137:
                    if (var5.equals("give")) {
                        var6 = 0;
                    }
                    break;
                case 3552391:
                    if (var5.equals("take")) {
                        var6 = 1;
                    }
                    break;
                case 3566014:
                    if (var5.equals("tops")) {
                        var6 = 4;
                    }
                    break;
                case 41740528:
                    if (var5.equals("giveall")) {
                        var6 = 5;
                    }
            }

            switch(var6) {
                case 0:
                    this.handleGiveCommand(sender, args);
                    break;
                case 1:
                    this.handleTakeCommand(sender, args);
                    break;
                case 2:
                    this.handleSetCommand(sender, args);
                    break;
                case 3:
                    this.handleBalanceCommand(sender, args);
                    break;
                case 4:
                    this.handleTopsCommand(sender);
                    break;
                case 5:
                    this.handleGiveAllCommand(sender, args);
                    break;
                case 6:
                    this.reloadConfig();
                    sender.sendMessage(this.colorize(this.getConfig().getString("messages.reload_success")));
                    break;
                default:
                    sender.sendMessage(this.colorize("&cНеизвестная команда! Используйте /rom help для получения списка команд."));
            }

            return true;
        } else {
            this.sendHelpMessage(sender);
            return true;
        }
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        int amount = this.parseAmount(args[2]);
        if (targetPlayer == null) {
            sender.sendMessage(this.colorize("&cИгрок не найден: " + args[1]));
        } else {
            this.addBalance(targetPlayer.getName(), amount);
            sender.sendMessage(this.colorize("&aВы выдали &e" + amount + " " + this.currencyName + " &aигроку &e" + targetPlayer.getName()));
        }
    }

    private void handleTakeCommand(CommandSender sender, String[] args) {
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        int amount = this.parseAmount(args[2]);
        if (targetPlayer == null) {
            sender.sendMessage(this.colorize("&cИгрок не найден: " + args[1]));
        } else {
            this.takeBalance(targetPlayer.getName(), amount);
            sender.sendMessage(this.colorize("&aВы забрали &e" + amount + " " + this.currencyName + " &aу игрока &e" + targetPlayer.getName()));
        }
    }

    private void handleSetCommand(CommandSender sender, String[] args) {
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        int amount = this.parseAmount(args[2]);
        if (targetPlayer == null) {
            sender.sendMessage(this.colorize("&cИгрок не найден: " + args[1]));
        } else {
            this.setBalance(targetPlayer.getName(), amount);
            sender.sendMessage(this.colorize("&aВы установили баланс &e" + amount + " " + this.currencyName + " &aу игрока &e" + targetPlayer.getName()));
        }
    }

    private void handleBalanceCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(this.colorize(this.getConfig().getString("messages.balance_list_header")));
            Iterator var3 = this.playersConfig.getKeys(false).iterator();

            while(var3.hasNext()) {
                String playerName = (String)var3.next();
                double balance = (double)this.getBalance(playerName);
                sender.sendMessage(this.colorize(this.getConfig().getString("messages.balance_list_format").replace("{player}", playerName).replace("{balance}", String.valueOf(balance)).replace("{currency}", this.currencyName)));
            }
        } else {
            String playerName = args[1];
            if (this.playersConfig.contains(playerName + ".balance")) {
                double balance = (double)this.getBalance(playerName);
                sender.sendMessage(this.colorize(this.getConfig().getString("messages.balance_single_format").replace("{player}", playerName).replace("{balance}", String.valueOf(balance)).replace("{currency}", this.currencyName)));
            } else {
                sender.sendMessage(this.colorize(this.getConfig().getString("messages.player_not_found").replace("{player}", playerName)));
            }
        }

    }

    private void handleTopsCommand(CommandSender sender) {
        sender.sendMessage(this.colorize("&6Топ игроков:"));
        List<Entry<String, Integer>> topPlayers = this.getTopPlayers();

        for(int i = 0; i < topPlayers.size(); ++i) {
            sender.sendMessage(this.colorize("&e#" + (i + 1) + " &a" + (String)((Entry)topPlayers.get(i)).getKey() + ": &e" + ((Entry)topPlayers.get(i)).getValue() + " " + this.currencyName));
        }

    }

    private void handleGiveAllCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(this.colorize("&cИспользование: /rom giveall <online/offline/all> <сумма>"));
        } else {
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException var12) {
                sender.sendMessage(this.colorize("&cСумма должна быть целым числом."));
                return;
            }

            if (amount <= 0) {
                sender.sendMessage(this.colorize("&cВведите положительную сумму."));
            } else {
                String type = args[1].toLowerCase();
                int count = 0;
                byte var7 = -1;
                switch(type.hashCode()) {
                    case -1548612125:
                        if (type.equals("offline")) {
                            var7 = 1;
                        }
                        break;
                    case -1012222381:
                        if (type.equals("online")) {
                            var7 = 0;
                        }
                        break;
                    case 96673:
                        if (type.equals("all")) {
                            var7 = 2;
                        }
                }

                OfflinePlayer[] var8;
                int var9;
                int var10;
                OfflinePlayer offlinePlayer;
                label61:
                switch(var7) {
                    case 0:
                        Iterator var13 = Bukkit.getOnlinePlayers().iterator();

                        while(true) {
                            if (!var13.hasNext()) {
                                break label61;
                            }

                            Player player = (Player)var13.next();
                            this.addBalance(player.getName(), amount);
                            ++count;
                        }
                    case 1:
                        var8 = Bukkit.getOfflinePlayers();
                        var9 = var8.length;
                        var10 = 0;

                        while(true) {
                            if (var10 >= var9) {
                                break label61;
                            }

                            offlinePlayer = var8[var10];
                            if (!offlinePlayer.isOnline()) {
                                this.addBalance(offlinePlayer.getName(), amount);
                                ++count;
                            }

                            ++var10;
                        }
                    case 2:
                        var8 = Bukkit.getOfflinePlayers();
                        var9 = var8.length;
                        var10 = 0;

                        while(true) {
                            if (var10 >= var9) {
                                break label61;
                            }

                            offlinePlayer = var8[var10];
                            this.addBalance(offlinePlayer.getName(), amount);
                            ++count;
                            ++var10;
                        }
                    default:
                        sender.sendMessage(this.colorize("&cОшибка! Используйте 'online', 'offline' или 'all' в качестве аргумента."));
                        return;
                }

                sender.sendMessage(this.colorize(this.getConfig().getString("messages.give_all_success").replace("{amount}", String.valueOf(amount)).replace("{currency}", this.currencyName).replace("{type}", type).replace("{count}", String.valueOf(count))));
            }
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(this.colorize(this.getConfig().getString("messages.help_header")));
        List<String> helpCommands = this.getConfig().getStringList("messages.help_commands");
        Iterator var3 = helpCommands.iterator();

        while(var3.hasNext()) {
            String command = (String)var3.next();
            sender.sendMessage(this.colorize(command));
        }

    }

    private int parseAmount(String amountString) {
        try {
            return Integer.parseInt(amountString);
        } catch (NumberFormatException var3) {
            return 0;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("give", "take", "set", "balance", "tops", "giveall", "reload", "help");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("giveall")) {
            return Arrays.asList("online", "offline", "all");
        } else {
            return args.length != 2 || !args[0].equalsIgnoreCase("give") && !args[0].equalsIgnoreCase("take") && !args[0].equalsIgnoreCase("set") ? Collections.emptyList() : (List)Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).collect(Collectors.toList());
        }
    }
}