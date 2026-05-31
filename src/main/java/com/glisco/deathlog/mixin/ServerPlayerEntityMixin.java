package com.glisco.deathlog.mixin;

import com.glisco.deathlog.server.DeathLogServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "die", at = @At("HEAD"))
    public void onServerDeath(DamageSource source, CallbackInfo ci) {
        var player = (Player) (Object) this;
        DeathLogServer.getStorage().store(player.getCombatTracker().getDeathMessage(), player);
    }

}
