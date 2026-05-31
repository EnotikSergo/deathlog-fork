package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import com.glisco.deathlog.death_info.RestorableDeathInfoProperty;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.NonNullList;

import java.util.List;

public class InventoryProperty implements RestorableDeathInfoProperty {

    private static final StructEndec<InventoryProperty> ENDEC = StructEndecBuilder.of(
            defaulted(MinecraftEndecs.ITEM_STACK.listOf()).fieldOf("items", s -> s.playerItems),
            defaulted(MinecraftEndecs.ITEM_STACK.listOf()).fieldOf("armor", s -> s.playerArmor),
            InventoryProperty::new
    );

    private final NonNullList<ItemStack> playerItems;
    private final NonNullList<ItemStack> playerArmor;

    public InventoryProperty(NonNullList<ItemStack> playerItems, NonNullList<ItemStack> playerArmor) {
        this.playerItems = playerItems;
        this.playerArmor = playerArmor;
    }

    public InventoryProperty(Inventory playerInventory) {
        this.playerItems = NonNullList.withSize(37, ItemStack.EMPTY);
        this.playerArmor = NonNullList.withSize(4, ItemStack.EMPTY);

        var inventory = playerInventory.player.getInventory();
        NonNullList<ItemStack> armorList = NonNullList.withSize(4, ItemStack.EMPTY);
        armorList.set(0, inventory.getItem(36));
        armorList.set(1, inventory.getItem(37));
        armorList.set(2, inventory.getItem(38));
        armorList.set(3, inventory.getItem(39));

        copy(armorList, playerArmor);
        copy(playerInventory.player.getInventory().getNonEquipmentItems(), playerItems);
        playerItems.set(36, playerInventory.player.getInventory().getItem(40).copy());
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Component formatted() {
        return null;
    }

    @Override
    public String toSearchableString() {
        StringBuilder builder = new StringBuilder();

        playerItems.forEach(stack -> builder.append(stack.getHoverName().getString()));
        playerArmor.forEach(stack -> builder.append(stack.getHoverName().getString()));

        return builder.toString();
    }

    @Override
    public void restore(ServerPlayer player) {
        final var inventory = player.getInventory();
        inventory.clearContent();

        copy(playerItems, inventory.getNonEquipmentItems(), 36);
        inventory.setItem(40,playerItems.get(36));
        inventory.setItem(36,playerArmor.get(0));
        inventory.setItem(37,playerArmor.get(1));
        inventory.setItem(38,playerArmor.get(2));
        inventory.setItem(39,playerArmor.get(3));
    }

    public NonNullList<ItemStack> getPlayerArmor() {
        return playerArmor;
    }

    public NonNullList<ItemStack> getPlayerItems() {
        return playerItems;
    }

    private static void copy(NonNullList<ItemStack> list, NonNullList<ItemStack> other) {
        copy(list, other, list.size());
    }

    private static void copy(NonNullList<ItemStack> list, NonNullList<ItemStack> other, int maxItems) {
        for (int i = 0; i < maxItems; i++) other.set(i, list.get(i).copy());
    }

    private static <T> Endec<NonNullList<T>> defaulted(Endec<List<T>> endec) {
        return endec.xmap(ts -> {
                    var defaulted = NonNullList.<T>create();
                    defaulted.addAll(ts);
                    return defaulted;
                },
                defaulted -> defaulted
        );
    }

    public static class Type extends DeathInfoPropertyType<InventoryProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlog.deathinfoproperty.inventory", Identifier.fromNamespaceAndPath("deathlog", "inventory"));
        }

        @Override
        public boolean displayedInInfoView() {
            return false;
        }

        @Override
        public StructEndec<InventoryProperty> endec() {
            return InventoryProperty.ENDEC;
        }
    }
}
