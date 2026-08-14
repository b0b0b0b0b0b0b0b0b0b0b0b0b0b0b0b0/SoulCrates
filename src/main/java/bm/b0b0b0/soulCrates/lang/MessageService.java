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
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
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
    private Supplier<String> forcedLocaleId = () -> null;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        reload();
    }

    public void setForcedLocaleSupplier(Supplier<String> forcedLocaleId) {
        this.forcedLocaleId = forcedLocaleId != null ? forcedLocaleId : () -> null;
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

    public List<String> loadedLocaleIds() {
        return List.copyOf(bundles.keySet());
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
            if (defaults.isConfigurationSection(key)) {
                continue;
            }
            if (!target.contains(key)) {
                target.set(key, defaults.get(key));
            }
        }
    }

    public String resolveLocaleId(UUID playerId) {
        String configured = configuredLocaleId();
        if (configured != null) {
            return configured;
        }
        if (playerId == null) {
            return DEFAULT_LOCALE;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return DEFAULT_LOCALE;
        }
        return resolveLocaleId(player);
    }

    public String resolveLocaleId(Player player) {
        String configured = configuredLocaleId();
        if (configured != null) {
            return configured;
        }
        if (player == null) {
            return DEFAULT_LOCALE;
        }
        return normalizeLocaleId(player.locale());
    }

    public String resolveLocaleId(CommandSender sender) {
        if (sender instanceof Player player) {
            return resolveLocaleId(player);
        }
        String configured = configuredLocaleId();
        return configured != null ? configured : DEFAULT_LOCALE;
    }

    public Locale javaLocale(UUID playerId) {
        return Locale.forLanguageTag(resolveLocaleId(playerId));
    }

    public String raw(UUID playerId, String key) {
        return template(resolveLocaleId(playerId), key);
    }

    public Component component(UUID playerId, String key, TagResolver... resolvers) {
        String template = normalizePlaceholderSyntax(template(resolveLocaleId(playerId), key));
        return miniMessage.deserialize(template, resolvers);
    }

    public Component parse(String template) {
        return miniMessage.deserialize(normalizePlaceholderSyntax(template == null ? "" : template));
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
        String template = normalizePlaceholderSyntax(template(resolveLocaleId(sender), key));
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

    private String configuredLocaleId() {
        String raw = forcedLocaleId.get();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return normalizeBundledLocaleId(raw);
    }

    private String normalizeBundledLocaleId(String raw) {
        String language = raw.toLowerCase(Locale.ROOT).trim();
        if (bundles.containsKey(language)) {
            return language;
        }
        if (language.startsWith("ru") && bundles.containsKey("ru")) {
            return "ru";
        }
        if (language.startsWith("en") && bundles.containsKey("en")) {
            return "en";
        }
        int separator = language.indexOf('-');
        if (separator > 0) {
            String base = language.substring(0, separator);
            if (bundles.containsKey(base)) {
                return base;
            }
        }
        return DEFAULT_LOCALE;
    }

    private String normalizeLocaleId(Locale locale) {
        if (locale == null) {
            return DEFAULT_LOCALE;
        }
        String language = locale.getLanguage().toLowerCase(Locale.ROOT);
        if (!language.isBlank() && bundles.containsKey(language)) {
            return language;
        }
        String tag = locale.toLanguageTag().toLowerCase(Locale.ROOT);
        if (bundles.containsKey(tag)) {
            return tag;
        }
        int separator = tag.indexOf('-');
        if (separator > 0) {
            String base = tag.substring(0, separator);
            if (bundles.containsKey(base)) {
                return base;
            }
        }
        if (tag.startsWith("ru") && bundles.containsKey("ru")) {
            return "ru";
        }
        return DEFAULT_LOCALE;
    }

    private FileConfiguration bundle(String localeId) {
        FileConfiguration config = bundles.get(localeId);
        if (config != null) {
            return config;
        }
        FileConfiguration fallback = bundles.get(DEFAULT_LOCALE);
        return fallback == null ? new YamlConfiguration() : fallback;
    }

    private String template(String localeId, String key) {
        String value = bundle(localeId).getString(key);
        if (value != null) {
            return value;
        }
        if (!DEFAULT_LOCALE.equals(localeId)) {
            value = bundle(DEFAULT_LOCALE).getString(key);
            if (value != null) {
                return value;
            }
        }
        return key;
    }

    static String normalizePlaceholderSyntax(String template) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(template.length());
        int index = 0;
        while (index < template.length()) {
            char current = template.charAt(index);
            if (current == '{') {
                int end = template.indexOf('}', index + 1);
                if (end > index + 1) {
                    String name = template.substring(index + 1, end);
                    if (name.matches("[A-Za-z0-9_]+")) {
                        builder.append('<').append(name).append('>');
                        index = end + 1;
                        continue;
                    }
                }
            }
            builder.append(current);
            index++;
        }
        return builder.toString();
    }
}
