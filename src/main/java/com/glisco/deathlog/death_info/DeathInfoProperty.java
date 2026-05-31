package com.glisco.deathlog.death_info;

import io.wispforest.endec.Endec;
import net.minecraft.network.chat.Component;

public interface DeathInfoProperty {

    Endec<DeathInfoProperty> ENDEC = Endec.dispatchedStruct(DeathInfoPropertyType::endec, DeathInfoProperty::getType, DeathInfoPropertyType.ENDEC);

    default Component getName() {
        return DeathInfoPropertyType.decorateName(getType().getName());
    }

    DeathInfoPropertyType<?> getType();

    Component formatted();

    String toSearchableString();
}
