package com.glisco.deathlog.client;

import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.client.gui.DeathLogScreen;
import com.glisco.deathlog.network.DeathLogPackets;
import com.glisco.deathlog.storage.DirectDeathLogStorage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class DeathLogClient implements ClientModInitializer {

    public static final com.glisco.deathlog.client.DeathLogConfig CONFIG = com.glisco.deathlog.client.DeathLogConfig.createAndLoad();

    public static final KeyMapping OPEN_DEATH_SCREEN = new KeyMapping("key.deathlog.death_screen", GLFW.GLFW_KEY_END, KeyMapping.Category.MISC);
    private static ClientDeathLogStorage storage;

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            storage = new ClientDeathLogStorage(client);
            DeathLogCommon.setStorage(storage);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            storage = null;
            DeathLogCommon.setStorage(null);
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof StatsScreen)) return;
            Screens.getButtons(screen).add(Button.builder(Component.nullToEmpty("DeathLog"), button -> {
                openScreen(getClientStorage());
            }).size(60, 20).pos(10, 5).build());
        });

        KeyBindingHelper.registerKeyBinding(OPEN_DEATH_SCREEN);
        ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
            if (OPEN_DEATH_SCREEN.consumeClick()) {
                openScreen(getClientStorage());
            }
        });

        DeathLogPackets.initClient();
    }

    private void openScreen(DirectDeathLogStorage clientStorage) {
        openScreen(clientStorage, Minecraft.getInstance().getCurrentServer() == null);
    }

    public static void openScreen(DirectDeathLogStorage storage, boolean canRestore) {
        final var screen = new DeathLogScreen(Minecraft.getInstance().screen, storage);
        Minecraft.getInstance().setScreen(screen);
        if (!canRestore) screen.disableRestoring();
    }

    public static DirectDeathLogStorage getClientStorage() {
        return storage;
    }
}
