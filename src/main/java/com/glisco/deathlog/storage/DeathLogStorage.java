package com.glisco.deathlog.storage;

import com.glisco.deathlog.client.DeathInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface DeathLogStorage {

    List<DeathInfo> getDeathInfoList(@Nullable UUID profile);

    void delete(DeathInfo info, @Nullable UUID profile);

    void store(Component deathMessage, Player player);

    void restore(int index, @Nullable UUID profile);

    boolean isErrored();

    RegistryAccess registries();
}
