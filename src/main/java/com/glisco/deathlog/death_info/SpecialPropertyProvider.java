package com.glisco.deathlog.death_info;

import com.glisco.deathlog.client.DeathInfo;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class SpecialPropertyProvider {

    private static final List<BiConsumer<DeathInfo, Player>> applyFunctions = new ArrayList<>();

    public static void register(BiConsumer<DeathInfo, Player> applyFunction) {
        applyFunctions.add(applyFunction);
    }

    public static void apply(DeathInfo info, Player player) {
        applyFunctions.forEach(deathInfoConsumer -> deathInfoConsumer.accept(info, player));
    }

}
