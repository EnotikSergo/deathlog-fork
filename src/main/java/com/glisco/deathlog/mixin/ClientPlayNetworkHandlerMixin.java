package com.glisco.deathlog.mixin;

import com.glisco.deathlog.client.DeathLogClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.Screenshot;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonPacketListenerImpl {

    protected ClientPlayNetworkHandlerMixin(Minecraft client, Connection connection, CommonListenerCookie connectionState) {
        super(client, connection, connectionState);
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "handlePlayerCombatKill", at = @At("HEAD"))
    private void onClientDeath(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
        if (!RenderSystem.isOnRenderThread()) return;
        DeathLogClient.getClientStorage().store(packet.message(), this.minecraft.player);

        if (DeathLogClient.CONFIG.screenshotsEnabled()) {
            Screenshot.grab(FabricLoader.getInstance().getGameDir().toFile(), this.minecraft.getMainRenderTarget(), text -> {
                text = Component.literal("§7[§bDeathLog§7] ").append(((MutableComponent) text).withStyle(ChatFormatting.GRAY));
                this.minecraft.player.displayClientMessage(text, false);
            });
        }
    }

    @Inject(method = "handleSetHealth", at = @At("HEAD"))
    private void onLegacyClientDeath(ClientboundSetHealthPacket packet, CallbackInfo ci) {
        if (!DeathLogClient.CONFIG.useLegacyDeathDetection() || !RenderSystem.isOnRenderThread()) return;
        if (packet.getHealth() > 0 || this.minecraft.player.isDeadOrDying()) return;

        DeathLogClient.getClientStorage().store(Component.empty(), this.minecraft.player);

        if (DeathLogClient.CONFIG.screenshotsEnabled()) {
            Screenshot.grab(FabricLoader.getInstance().getGameDir().toFile(), this.minecraft.getMainRenderTarget(), text -> {
                text = Component.literal("§7[§bDeathLog§7] ").append(((MutableComponent) text).withStyle(ChatFormatting.GRAY));
                this.minecraft.player.displayClientMessage(text, false);
            });
        }
    }

}
