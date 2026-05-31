package com.glisco.deathlog.client.gui;

import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class DeathLogToast extends SystemToast {

    public DeathLogToast(SystemToastId type, Component title, @Nullable Component description) {
        super(type, title, description);
    }

}
