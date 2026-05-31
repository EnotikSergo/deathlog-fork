package com.glisco.deathlog.death_info;

import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.death_info.properties.MissingDeathInfoProperty;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

public abstract class DeathInfoPropertyType<P extends DeathInfoProperty> {

    public static final Endec<DeathInfoPropertyType<?>> ENDEC = MinecraftEndecs.IDENTIFIER.xmap(
            identifier -> DeathLogCommon.PROPERTY_TYPES.getOptional(identifier).orElse(new MissingDeathInfoProperty.Type(identifier)),
            DeathInfoPropertyType::getId
    );

    private final String translationKey;
    private final Identifier id;

    public DeathInfoPropertyType(String translationKey, Identifier id) {
        this.translationKey = translationKey;
        this.id = id;
    }

    public MutableComponent getName() {
        return Component.translatable(translationKey);
    }

    public static Component decorateName(MutableComponent name) {
        return name.withStyle(ChatFormatting.BLUE);
    }

    public Identifier getId() {
        return id;
    }

    public abstract boolean displayedInInfoView();

    public abstract StructEndec<P> endec();
}
