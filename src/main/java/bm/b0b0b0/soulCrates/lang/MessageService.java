package bm.b0b0b0.soulCrates.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageService {

    public static final String DEFAULT_LOCALE = "en";

    private static final List<String> JAR_LOCALES = List.of("en", "ru");

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;
    private final Map<String, FileConfiguration> bundles = new LinkedHashMap<>();

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        reload();
    }

    public void reload() {
        bundles.clear();
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        for (String localeId : JAR_LOCALES) {
            ensureJarLocaleFile(langDir, localeId);
        }
        loadDiscoveredLocaleFiles(langDir);
    }

    private void ensureJarLocaleFile(File langDir, String localeId) {
        File target = new File(langDir, "messages_" + localeId + ".yml");
        if (!target.exists()) {
            plugin.saveResource("lang/messages_" + localeId + ".yml", false);
        }
    }

    private void loadDiscoveredLocaleFiles(File langDir) {
        File[] files = langDir.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        FileConfiguration englishBundled = loadBundledDefaults("en");
        for (File file : files) {
            String localeId = localeIdFromFileName(file.getName());
            if (localeId == null) {
                continue;
            }
            FileConfiguration disk = YamlConfiguration.loadConfiguration(file);
            FileConfiguration bundled = loadBundledDefaults(localeId);
            if (bundled != null) {
                mergeMissingKeys(disk, bundled);
            } else if (englishBundled != null) {
                mergeMissingKeys(disk, englishBundled);
            }
            bundles.put(localeId, disk);
        }
    }

    private static String localeIdFromFileName(String fileName) {
        if (!fileName.startsWith("messages_") || !fileName.endsWith(".yml")) {
            return null;
        }
        String localeId = fileName.substring("messages_".length(), fileName.length() - ".yml".length()).trim();
        if (localeId.isEmpty()) {
            return null;
        }
        return localeId.toLowerCase(Locale.ROOT);
    }

    private FileConfiguration loadBundledDefaults(String localeId) {
        String resourcePath = "lang/messages_" + localeId + ".yml";
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return null;
        }
    }

    private static void mergeMissingKeys(FileConfiguration target, FileConfiguration defaults) {
        for (String key : defaults.getKeys(true)) {
            if (!target.contains(key)) {
                target.set(key, defaults.get(key));
            }
        }
    }

    public String resolveLocaleId(UUID playerId) {
        if (playerId == null) {
            return DEFAULT_LOCALE;
        }
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) {
            return DEFAULT_LOCALE;
        }
        return resolveLocaleId(player);
    }

    public String resolveLocaleId(Player player) {
        if (player == null) {
            return DEFAULT_LOCALE;
        }
        String locale = player.locale().toLanguageTag().toLowerCase(Locale.ROOT);
        if (locale.startsWith("ru")) {
            return "ru";
        }
        return DEFAULT_LOCALE;
    }

    public String raw(UUID playerId, String key) {
        String localeId = resolveLocaleId(playerId);
        FileConfiguration bundle = bundles.getOrDefault(localeId, bundles.get(DEFAULT_LOCALE));
        if (bundle == null) {
            return key;
        }
        String value = bundle.getString(key);
        if (value == null) {
            FileConfiguration fallback = bundles.get(DEFAULT_LOCALE);
            if (fallback != null) {
                value = fallback.getString(key);
            }
        }
        return value == null ? key : value;
    }

    public Component component(UUID playerId, String key, TagResolver... resolvers) {
        String template = raw(playerId, key);
        return miniMessage.deserialize(template, resolvers);
    }

    public Component parse(String template) {
        return miniMessage.deserialize(template == null ? "" : template);
    }

    public void send(UUID playerId, String key, TagResolver... resolvers) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) {
            return;
        }
        player.sendMessage(component(playerId, key, resolvers));
    }

    public void send(CommandSender sender, String key, TagResolver... resolvers) {
        if (sender instanceof Player player) {
            send(player.getUniqueId(), key, resolvers);
            return;
        }
        String template = raw(null, key);
        sender.sendMessage(miniMessage.deserialize(template, resolvers));
    }

    public Component prefix(UUID playerId) {
        return component(playerId, "prefix");
    }

    public Component prefixed(UUID playerId, String key, TagResolver... resolvers) {
        return prefix(playerId).append(component(playerId, key, resolvers));
    }

    public TagResolver placeholder(String name, String value) {
        return Placeholder.parsed(name, value == null ? "" : value);
    }
}
