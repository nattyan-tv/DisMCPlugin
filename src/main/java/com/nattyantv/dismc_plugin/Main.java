package com.nattyantv.dismc_plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import org.jetbrains.annotations.NotNull;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;


public class Main extends JavaPlugin implements Listener {
    public Long channelID;
    public String webhookurl;
    public JDA bot;

    TextChannel channel;
    WebhookClient client;

    private static final String mesSTART = ":arrow_forward:";
    private static final String mesSTOP = ":stop_button:";
    private static final String mesJOIN = ":inbox_tray:";
    private static final String mesLEAVE = ":outbox_tray:";
    private static final String mesDEATH = ":skull_crossbones:";

    public void writeConfig() {
        FileConfiguration config = getConfig();
        config.set("token", "Your Discord Bot Token");
        config.set("channel", "Your Discord Channel ID");
        saveConfig();
    }

    public String convText(Component comp) {
        return PlainTextComponentSerializer.plainText().serialize(comp);
    }

    @Override
    public void onEnable(){
		getServer().getPluginManager().registerEvents(this, this);
        FileConfiguration config = getConfig();
        getLogger().info("Loading...");
        try {
            String token = config.getString("token");
            channelID = config.getLong("channel");
            webhookurl = config.getString("webhook");
            if (token == null || token.isEmpty()) {
                getLogger().warning("Token is null!");
                writeConfig();
                return;
            }
            if (channelID == null) {
                getLogger().warning("Channel ID is null!");
                writeConfig();
                return;
            }

            bot = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .setRawEventsEnabled(true)
                .setActivity(Activity.competing("Minecraft Server"))
                .addEventListeners(new ListenerAdapter() {
                    @Override
                    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                        if (event.getChannel().getIdLong() != channelID) return;
                        if (event.getAuthor().isBot()) return;
                        String message = event.getMessage().getContentStripped();
                        String author = event.getAuthor().getName();
                        getServer().broadcastMessage(String.format("§1[Discord:%s]§r %s", author, message));
                    }
                })
                .build();
            bot.awaitReady();

            channel = bot.getTextChannelById(channelID);

            if (webhookurl == null || webhookurl.equals("")) {
                channel.retrieveWebhooks().queue(webhooks -> {
                    Boolean webhookExists = false;
                    if (webhooks.isEmpty()) {
                        webhookExists = false;
                    } else {
                        int size = webhooks.size();
                        for(int i = 0; i < size; i++)
                        {
                            Webhook w = webhooks.get(i);
                            if (w.getName().equals("DisMC")) {
                                webhookurl = w.getUrl();
                                webhookExists = true;
                                break;
                            }
                        }
                    }

                    if (!webhookExists) {
                        channel.createWebhook("DisMC").queue(wh -> {
                            webhookurl = wh.getUrl();
                            config.set("webhook", webhookurl);
                            client = WebhookClient.withUrl(webhookurl);
                        });
                    }
                    else {
                        config.set("webhook", webhookurl);
                        client = WebhookClient.withUrl(webhookurl);
                    }
                });
            } else {
                client = WebhookClient.withUrl(webhookurl);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        getLogger().info("Discord Bot has been enabled!");
        channel.sendMessage(mesSTART + "プラグインが正常に読み込まれました。").queue();
    }

    @Override
    public void onDisable() {
        channel.sendMessage(mesSTOP + "プラグインが正常に停止しました。").queue();
        getLogger().info("DisMC Plugin has been disabled!");
        bot.shutdownNow();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        String author = e.getPlayer().getName();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(mesJOIN + "プレイヤーがサーバーに参加しました！");
        eb.setDescription(author + "が参加しました。");
        channel.sendMessageEmbeds(eb.build()).queue();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        String author = e.getPlayer().getName();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(mesLEAVE + "プレイヤーがサーバーから退出しました。");
        eb.setDescription(author + "が退出しました。");
        channel.sendMessageEmbeds(eb.build()).queue();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(mesDEATH + "プレイヤーが死亡しました...");
        eb.setDescription(convText(e.deathMessage()));
        channel.sendMessageEmbeds(eb.build()).queue();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        String message = e.getMessage();
        String author = e.getPlayer().getName();
        String avatarUrl = "https://minotar.net/avatar/" + e.getPlayer().getUniqueId().toString() + "/128";

        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setUsername(author);
        builder.setAvatarUrl(avatarUrl); // use this avatar
        builder.setContent(message);
        client.send(builder.build());
    }
}
