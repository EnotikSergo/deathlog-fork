package com.glisco.deathlog.mixin;

import com.glisco.deathlog.duck.MinecraftServerAccessorDuck;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor extends MinecraftServerAccessorDuck {

    @Accessor("storageSource")
    LevelStorageSource.LevelStorageAccess deathlog_getSession();
}
