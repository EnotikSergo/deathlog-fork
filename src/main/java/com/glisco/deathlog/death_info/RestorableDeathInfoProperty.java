package com.glisco.deathlog.death_info;

import net.minecraft.server.level.ServerPlayer;

public interface RestorableDeathInfoProperty extends DeathInfoProperty {

    void restore(ServerPlayer player);

}
