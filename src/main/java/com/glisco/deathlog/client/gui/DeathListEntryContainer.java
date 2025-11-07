package com.glisco.deathlog.client.gui;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.UISounds;
import io.wispforest.owo.util.EventSource;
import io.wispforest.owo.util.EventStream;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

public class DeathListEntryContainer extends FlowLayout {

    protected final EventStream<OnSelected> selectedEvents = OnSelected.newStream();
    protected final Animation<Insets> slideAnimation;

    protected boolean focused = false;
    protected boolean selected = false;

    public DeathListEntryContainer() {
        super(Sizing.content(), Sizing.content(), Algorithm.HORIZONTAL);
        this.slideAnimation = this.padding.animate(150, Easing.QUADRATIC, this.padding.get().add(0, 0, 5, 0));
    }

    public EventSource<OnSelected> onSelected() {
        return this.selectedEvents.source();
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        super.draw(context, mouseX, mouseY, partialTicks, delta);
        if (this.selected) context.drawRectOutline(this.x, this.y, this.width, this.height, 0xFFAFAFAF);
    }

    @Override
    protected void parentUpdate(float delta, int mouseX, int mouseY) {
        if (this.hovered || this.focused || this.selected) {
            this.slideAnimation.forwards();
        } else {
            this.slideAnimation.backwards();
        }
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            super.onMouseDown(click, doubled);
            this.select();
            return true;
        }
        return super.onMouseDown(click, doubled);
    }

    @Override
    public boolean onKeyPress(KeyInput key) {
        boolean handledBySuper = super.onKeyPress(key);

        int code = key.getKeycode();
        if (code != GLFW.GLFW_KEY_ENTER
                && code != GLFW.GLFW_KEY_SPACE
                && code != GLFW.GLFW_KEY_KP_ENTER) {
            return handledBySuper;
        }

        this.select();
        return true;
    }

    private void select() {
        for (var sibling : this.parent.children()) {
            if (sibling == this || !(sibling instanceof DeathListEntryContainer container)) continue;
            container.selected = false;
        }

        this.selected = true;
        this.selectedEvents.sink().onSelected(this);

        UISounds.playInteractionSound();
    }

    @Override
    public boolean canFocus(FocusSource source) {
        return true;
    }

    @Override
    public void onFocusGained(FocusSource source) {
        super.onFocusGained(source);
        this.focused = true;
    }

    @Override
    public void onFocusLost() {
        super.onFocusLost();
        this.focused = false;
    }

    public interface OnSelected {
        void onSelected(DeathListEntryContainer container);

        static EventStream<OnSelected> newStream() {
            return new EventStream<>(subscribers -> container -> {
                for (var subscriber : subscribers) {
                    subscriber.onSelected(container);
                }
            });
        }
    }
}
