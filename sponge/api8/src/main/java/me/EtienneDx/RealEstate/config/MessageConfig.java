/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.EtienneDx.RealEstate.config;

import me.EtienneDx.RealEstate.Messages;
import me.EtienneDx.RealEstate.RealEstate;
import me.EtienneDx.RealEstate.SendPlayerMessageTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;

public class MessageConfig {

    private static final String header = "RealEstate wiki and newest versions are available at http://www.github.com/EtienneDx/RealEstate";
    private static final ConfigurationOptions LOADER_OPTIONS = ConfigurationOptions.defaults().serializers(RealEstate.instance.typeSerializerCollection);
    private HoconConfigurationLoader loader;
    private CommentedConfigurationNode root;
    private ObjectMapper<Messages> configMapper;
    private Messages data;

    public MessageConfig(Path path) {

        try {
            if (Files.notExists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().path(path).defaultOptions(LOADER_OPTIONS).build();
            this.configMapper = RealEstate.OBJECTMAPPER_FACTORY.get(Messages.class);

            if (reload()) {
                save();
            }
        } catch (Exception e) {
            RealEstate.instance.getLogger().error("Failed to initialize configuration", e);
        }
    }

    public Messages getData() {
        return this.data;
    }

    public void save() {
            try {
                this.configMapper.save(this.data, this.root.node(RealEstate.MOD_ID));
                this.loader.save(this.root);
            } catch (IOException e) {
                RealEstate.instance.getLogger().error("Failed to save configuration", e);
            }
    }

    public boolean reload() {
        try {
            this.root = this.loader.load();
            this.data = this.configMapper.load(this.root.node(RealEstate.MOD_ID));
        } catch (Exception e) {
            RealEstate.instance.getLogger().error("Failed to load configuration", e);
            return false;
        }
        return true;
    }

    public static String getMessage(String msgTemplate, String... args) {
        return getMessage(msgTemplate, true, args);
    }

    public static String getMessage(String msgTemplate, boolean withPrefix, String... args) {
        if (withPrefix) {
            msgTemplate = RealEstate.instance.config.chatPrefix + msgTemplate;
        }

        msgTemplate = msgTemplate.replace('$', Messages.COLOR_CHAR);

        for (int i = 0; i < args.length; i++) {
            String param = args[i];
            msgTemplate = msgTemplate.replaceAll("\\{" + i + "\\}", Matcher.quoteReplacement(param));
        }

        return msgTemplate;
    }

    public static void sendMessage(ServerPlayer player, String msgTemplate, String... args) {
        sendMessage(player, msgTemplate, 0, args);
    }


    public static void sendMessage(ServerPlayer player, String msgTemplate, long delayInTicks, String... args) {
        String message = getMessage(msgTemplate, args);
        sendMessage(player, message, delayInTicks);
    }


    public static void sendMessage(ServerPlayer player, String message) {
        sendMessage(player, getMessage(message), 0);
    }

    public static void sendMessage(ServerPlayer player, String message, long delayInTicks) {
        SendPlayerMessageTask task = new SendPlayerMessageTask(player, message);

        if (delayInTicks > 0) {
            Sponge.server().scheduler().submit(Task.builder().plugin(RealEstate.instance.pluginContainer).delay(Ticks.of(delayInTicks)).execute(task).build());
        } else {
            task.run();
        }
    }
}
