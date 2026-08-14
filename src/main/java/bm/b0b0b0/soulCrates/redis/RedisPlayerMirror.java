package bm.b0b0b0.soulCrates.redis;

import bm.b0b0b0.soulCrates.config.settings.RedisSettings;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.RedisClient;

public final class RedisPlayerMirror implements AutoCloseable {

    private final Plugin plugin;
    private final String serverId;
    private final RedisClient jedis;
    private final HostAndPort redisAddress;
    private final JedisClientConfig redisClientConfig;
    private final String channel;
    private final boolean pubSubEnabled;
    private Thread subscriberThread;
    private JedisPubSub pubSub;

    public RedisPlayerMirror(Plugin plugin, boolean active, RedisSettings settings) {
        this.plugin = plugin;
        this.serverId = UUID.randomUUID().toString().substring(0, 8);
        this.channel = settings.channel == null || settings.channel.isBlank()
                ? "soulcrates:sync"
                : settings.channel;
        this.pubSubEnabled = settings.pubSubEnabled;
        if (!active || !settings.enabled) {
            this.jedis = null;
            this.redisAddress = null;
            this.redisClientConfig = null;
            return;
        }
        DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(settings.timeoutMs)
                .socketTimeoutMillis(settings.timeoutMs)
                .database(settings.database);
        if (settings.password != null && !settings.password.isBlank()) {
            clientConfigBuilder.password(settings.password);
        }
        this.redisClientConfig = clientConfigBuilder.build();
        this.redisAddress = new HostAndPort(settings.host, settings.port);
        this.jedis = RedisClient.builder()
                .hostAndPort(redisAddress)
                .clientConfig(redisClientConfig)
                .build();
    }

    public boolean enabled() {
        return jedis != null;
    }

    public void publishKeys(UUID playerId, String crateId, int amount) {
        publishRaw("K|" + playerId + "|" + crateId.toLowerCase(Locale.ROOT) + "|" + amount);
    }

    public void publishPity(UUID playerId, String crateId, int counter) {
        publishRaw("P|" + playerId + "|" + crateId.toLowerCase(Locale.ROOT) + "|" + counter);
    }

    public void publishInvalidate(UUID playerId) {
        publishRaw("I|" + playerId);
    }

    public void startSubscriber(
            BiConsumer<UUID, String> onKeys,
            BiConsumer<UUID, String> onPity,
            Consumer<UUID> onInvalidate
    ) {
        if (jedis == null || !pubSubEnabled) {
            return;
        }
        stopSubscriber();
        pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String subscribedChannel, String message) {
                if (!channel.equals(subscribedChannel) || message == null || message.isBlank()) {
                    return;
                }
                String[] parts = message.split("\\|", -1);
                if (parts.length < 2) {
                    return;
                }
                if (parts[0].endsWith("@" + serverId)) {
                    return;
                }
                try {
                    if ("K".equals(parts[0]) && parts.length >= 4) {
                        UUID playerId = UUID.fromString(parts[1]);
                        String payload = parts[2] + "|" + parts[3];
                        onKeys.accept(playerId, payload);
                    } else if ("P".equals(parts[0]) && parts.length >= 4) {
                        UUID playerId = UUID.fromString(parts[1]);
                        String payload = parts[2] + "|" + parts[3];
                        onPity.accept(playerId, payload);
                    } else if ("I".equals(parts[0])) {
                        onInvalidate.accept(UUID.fromString(parts[1]));
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        };
        subscriberThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Jedis subscriber = new Jedis(redisAddress, redisClientConfig)) {
                    subscriber.subscribe(pubSub, channel);
                } catch (Exception exception) {
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "SoulCrates-Redis-Sub");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    public void stopSubscriber() {
        if (pubSub != null) {
            try {
                pubSub.unsubscribe();
            } catch (Exception ignored) {
            }
            pubSub = null;
        }
        if (subscriberThread != null) {
            subscriberThread.interrupt();
            subscriberThread = null;
        }
    }

    @Override
    public void close() {
        stopSubscriber();
        if (jedis != null) {
            jedis.close();
        }
    }

    private void publishRaw(String payload) {
        if (plugin == null || jedis == null) {
            return;
        }
        String tagged = payload + "@" + serverId;
        PluginSchedulers.runAsync(plugin, () -> {
            try {
                jedis.publish(channel, tagged);
            } catch (Exception ignored) {
            }
        });
    }
}
