package mc.bastion.ajboards;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BastionMCAJBoards extends JavaPlugin implements CommandExecutor, TabCompleter {
    private static final List<String> TYPES = List.of("kills", "deaths", "hours", "clans", "money");
    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private final Map<String, List<TextDisplay>> displays = new HashMap<>();
    private Economy economy;
    private YamlConfiguration clans;
    private File clansFile;
    private BukkitTask task;

    @Override public void onEnable() {
        saveDefaultConfig();
        clansFile = new File(getDataFolder(), "clans.yml");
        if (!clansFile.exists()) saveResource("clans.yml", false);
        clans = YamlConfiguration.loadConfiguration(clansFile);
        Objects.requireNonNull(getCommand("ajboards")).setExecutor(this);
        Objects.requireNonNull(getCommand("ajboards")).setTabCompleter(this);
        loadBoards();
        runUpdater();
    }

    @Override public void onDisable() {
        if (task != null) task.cancel();
        clearDisplays();
    }

    private void hookEconomy() {
        economy = null;
        if (!getServer().getPluginManager().isPluginEnabled("Vault")) return;
        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration != null) economy = registration.getProvider();
    }

    private void runUpdater() {
        if (task != null) task.cancel();
        hookEconomy();
        long interval = Math.max(20, getConfig().getLong("refresh-ticks", 20));
        task = getServer().getScheduler().runTaskTimer(this, this::updateAll, 1L, interval);
    }

    private void clearDisplays() {
        for (List<TextDisplay> lines : displays.values()) for (TextDisplay line : lines) if (line.isValid()) line.remove();
        displays.clear();
    }

    private Location boardLocation(String type) {
        String key = "boards." + type;
        String worldName = getConfig().getString(key + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, getConfig().getDouble(key + ".x"), getConfig().getDouble(key + ".y"), getConfig().getDouble(key + ".z"));
    }

    private void loadBoards() {
        clearDisplays();
        for (String type : TYPES) {
            Location origin = boardLocation(type);
            if (origin == null) continue;
            origin.getChunk().load();
            int total = Math.max(1, Math.min(30, getConfig().getInt("top-size", 10))) + 1;
            double space = getConfig().getDouble("line-spacing", 0.29);
            List<TextDisplay> lines = new ArrayList<>();
            for (int n = 0; n < total; n++) {
                Location at = origin.clone().subtract(0, n * space, 0);
                TextDisplay display = origin.getWorld().spawn(at, TextDisplay.class, entity -> {
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setPersistent(false);
                    entity.setSeeThrough(true);
                    entity.setShadowed(true);
                    entity.setDefaultBackground(false);
                    entity.setLineWidth(450);
                    entity.addScoreboardTag("bastion-ajboards");
                    Transformation t = entity.getTransformation();
                    float scale = (float) Math.max(0.2, getConfig().getDouble("scale", 1.0));
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(), new Vector3f(scale, scale, scale), t.getRightRotation()));
                });
                lines.add(display);
            }
            displays.put(type, lines);
        }
        updateAll();
    }

    private record Entry(String name, double score) { }

    private List<Entry> ranking(String type) {
        List<Entry> entries = new ArrayList<>();
        if (type.equals("clans")) {
            ConfigurationSection section = clans.getConfigurationSection("clans");
            if (section != null) for (String name : section.getKeys(false)) {
                entries.add(new Entry(name, section.getDouble(name, 0)));
            }
        } else {
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                String name = player.getName();
                if (name == null || name.isBlank() || !player.hasPlayedBefore()) continue;
                try {
                    double score = switch (type) {
                        case "kills" -> player.getStatistic(Statistic.PLAYER_KILLS);
                        case "deaths" -> player.getStatistic(Statistic.DEATHS);
                        case "hours" -> player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 72000.0;
                        case "money" -> economy == null ? 0 : economy.getBalance(player);
                        default -> 0;
                    };
                    if (Double.isFinite(score) && score > 0) entries.add(new Entry(name, score));
                } catch (RuntimeException ex) {
                    getLogger().fine("No se pudo leer la estadística de " + name + ": " + ex.getMessage());
                }
            }
        }
        entries.sort(Comparator.comparingDouble(Entry::score).reversed().thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    private void updateAll() {
        for (Map.Entry<String, List<TextDisplay>> board : displays.entrySet()) {
            String type = board.getKey();
            List<TextDisplay> lines = board.getValue();
            List<Entry> ranked = ranking(type);
            String title = getConfig().getString("formats." + type + ".title", type.toUpperCase(Locale.ROOT));
            putText(lines.get(0), title);
            String template = getConfig().getString("formats." + type + ".line", "#{rank} {player}: {value}");
            for (int i = 1; i < lines.size(); i++) {
                String value;
                if (i - 1 < ranked.size()) {
                    Entry e = ranked.get(i - 1);
                    String number = type.equals("hours") ? new DecimalFormat("#,##0.0").format(e.score())
                        : type.equals("money") ? new DecimalFormat("#,##0.##").format(e.score())
                        : new DecimalFormat("#,##0").format(e.score());
                    value = template.replace("{rank}", Integer.toString(i)).replace("{player}", e.name()).replace("{value}", number);
                } else value = i == 1 ? getConfig().getString("empty-line", "&7Sin datos") : "";
                putText(lines.get(i), value);
            }
        }
    }

    private void putText(TextDisplay d, String text) {
        if (!d.isValid()) return;
        String formatted = color(text);
        if (!formatted.equals(d.getText())) d.setText(formatted);
    }

    private String color(String input) {
        Matcher matcher = HEX.matcher(input == null ? "" : input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder codes = new StringBuilder("§x");
            for (char c : hex.toCharArray()) codes.append('§').append(c);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(codes.toString()));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    private void message(CommandSender sender, String s) { sender.sendMessage(color("&#FFCC33&lAJBoards &8» &f" + s)); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bastionajboards.admin")) { message(sender, "&cSin permiso."); return true; }
        if (args.length == 0) { message(sender, "&e/ajboards set <kills|deaths|hours|clans|money> &7| &e/ajboards remove <tipo> &7| &e/ajboards list &7| &e/ajboards reload &7| &e/ajboards clan set <nombre> <puntos>"); return true; }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("reload")) {
            reloadConfig();
            clans = YamlConfiguration.loadConfiguration(clansFile);
            loadBoards(); runUpdater();
            message(sender, "&aConfiguración y hologramas recargados."); return true;
        }
        if (action.equals("refresh")) { updateAll(); message(sender, "&aHologramas actualizados."); return true; }
        if (action.equals("list")) {
            message(sender, "Hologramas: " + String.join(", ", displays.keySet())); return true;
        }
        if (action.equals("clan") && args.length >= 4 && args[1].equalsIgnoreCase("set")) {
            try {
                double points = Double.parseDouble(args[3]);
                if (!Double.isFinite(points) || points < 0) throw new NumberFormatException();
                clans.set("clans." + args[2], points); clans.save(clansFile); updateAll();
                message(sender, "&aClan " + args[2] + " actualizado a " + points + " puntos.");
            } catch (NumberFormatException e) { message(sender, "&cPuntuación inválida."); }
            catch (IOException e) { message(sender, "&cNo se pudieron guardar las puntuaciones: " + e.getMessage()); }
            return true;
        }
        if (action.equals("clan") && args.length >= 3 && args[1].equalsIgnoreCase("remove")) {
            clans.set("clans." + args[2], null);
            try { clans.save(clansFile); } catch (IOException ex) { message(sender, "&cError al guardar."); return true; }
            updateAll(); message(sender, "&aClan eliminado de las puntuaciones."); return true;
        }
        if ((action.equals("set") || action.equals("remove")) && args.length >= 2) {
            String type = args[1].toLowerCase(Locale.ROOT);
            if (!TYPES.contains(type)) { message(sender, "&cTipo no válido: " + type); return true; }
            if (action.equals("set")) {
                if (!(sender instanceof Player p)) { message(sender, "&cDebes estar dentro del juego."); return true; }
                Location loc = p.getLocation().add(0, 2.6, 0);
                getConfig().set("boards." + type + ".world", loc.getWorld().getName());
                getConfig().set("boards." + type + ".x", loc.getX());
                getConfig().set("boards." + type + ".y", loc.getY());
                getConfig().set("boards." + type + ".z", loc.getZ());
            } else getConfig().set("boards." + type, null);
            saveConfig(); loadBoards();
            message(sender, action.equals("set") ? "&aTOP " + type + " colocado." : "&aTOP " + type + " eliminado."); return true;
        }
        message(sender, "&cUso inválido. Escribe /ajboards."); return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("bastionajboards.admin")) return List.of();
        List<String> options = List.of();
        if (args.length == 1) options = List.of("set", "remove", "list", "reload", "refresh", "clan");
        if (args.length == 2) options = args[0].equalsIgnoreCase("clan") ? List.of("set", "remove") : (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) ? TYPES : List.of();
        if (args.length == 3 && args[0].equalsIgnoreCase("clan")) options = clans.getConfigurationSection("clans") == null ? List.of() : new ArrayList<>(clans.getConfigurationSection("clans").getKeys(false));
        String partial = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(partial)).toList();
    }
}
