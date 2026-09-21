package com.glisco.deathlog.client.gui;

import com.glisco.deathlog.client.DeathInfo;
import com.glisco.deathlog.network.RemoteDeathLogStorage;
import com.glisco.deathlog.storage.DirectDeathLogStorage;
import io.wispforest.owo.config.ui.ConfigScreenProviders;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.parsing.UIParsing;
import io.wispforest.owo.util.Observable;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeathLogScreen extends BaseUIModelScreen<FlowLayout> {

    private final Screen parent;
    private final DirectDeathLogStorage storage;

    private FlowLayout detailPanel;

    private final Observable<String> currentSearchTerm = Observable.of("");
    private boolean canRestore = true;

    public DeathLogScreen(Screen parent, DirectDeathLogStorage storage) {
        super(FlowLayout.class, DataSource.asset(Identifier.fromNamespaceAndPath("deathlog", "deathlog")));
        this.parent = parent;
        this.storage = storage;

        this.currentSearchTerm.observe(s -> {
            this.buildDeathList();
        });
    }

    @Override
    protected void init() {
        super.init();

        var configButton = this.uiAdapter.rootComponent.childById(ButtonComponent.class, "config-button");
        if (configButton != null) {
            if (this.height >= 275) {
                configButton.positioning(Positioning.relative(100, 100)).margins(Insets.none());
            } else {
                configButton.positioning(Positioning.relative(100, 0)).margins(Insets.top(-5));
            }
        }
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    protected void build(FlowLayout rootComponent) {
        this.detailPanel = rootComponent.childById(FlowLayout.class, "detail-panel");

        rootComponent.childById(TextBoxComponent.class, "search-box").<TextBoxComponent>configure(searchBox -> {
            searchBox.onChanged().subscribe(value -> {
                this.currentSearchTerm.set(value.toLowerCase(Locale.ROOT));
            });
        });

        rootComponent.childById(ButtonComponent.class, "config-button").onPress(button -> {
            this.minecraft.setScreenAndShow(ConfigScreenProviders.get("deathlog").apply(this));
        });

        this.uiAdapter.rootComponent.childById(LabelComponent.class, "death-count-label").text(
                Component.translatable("text.deathlog.death_list_title", this.storage.getDeathInfoList().size())
        );

        this.buildDeathList();
    }

    public void updateInfo(DeathInfo info, int index) {
        this.storage.getDeathInfoList().set(index, info);
        this.selectInfo(this.storage.getDeathInfoList().get(index));
    }

    public void disableRestoring() {
        this.canRestore = false;
    }

    private void buildDeathList() {
        this.uiAdapter.rootComponent.childById(FlowLayout.class, "death-list").<FlowLayout>configure(deathList -> {
            deathList.clearChildren();

            for (int i = 0; i < this.storage.getDeathInfoList().size(); i++) {
                final int infoIndex = i;
                var deathInfo = this.storage.getDeathInfoList().get(infoIndex);

                if (!this.currentSearchTerm.get().isBlank() && !deathInfo.createSearchString().contains(this.currentSearchTerm.get())) {
                    continue;
                }

                deathList.child(this.model.expandTemplate(
                        DeathListEntryContainer.class,
                        "death-list-entry",
                        Map.of(
                                "death-time", deathInfo.getListName().getString(),
                                "death-message", deathInfo.getTitle().getString()
                        )
                ).<DeathListEntryContainer>configure(container -> {
                    container.onSelected().subscribe(c -> {
                        this.selectInfo(this.storage.getDeathInfoList().get(infoIndex));
                    });

                    container.mouseDown().subscribe((MouseButtonEvent click, boolean doubled) -> {
                        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return false;

                        var root = this.uiAdapter.rootComponent;
                        DropdownComponent.openContextMenu(
                                this,
                                root, FlowLayout::child,
                                container.x() - root.padding().get().left() + click.x(),
                                container.y() - root.padding().get().top() + click.y(),
                                dropdown -> {
                                    dropdown.surface(Surface.blur(3, 5).and(Surface.flat(0xC7000000)).and(Surface.outline(0xFF121212)));
                                    //dropdown.zIndex(100);

                                    if (this.canRestore) {
                                        dropdown.button(Component.translatable("text.deathlog.action.restore"), dropdown_ -> {
                                            this.storage.restore(infoIndex);
                                            dropdown.remove();
                                        });
                                    }

                                    dropdown.button(Component.translatable("text.deathlog.action.delete"), dropdown_ -> {
                                        this.storage.delete(deathInfo);
                                        this.buildDeathList();
                                        dropdown.remove();
                                    });
                                }
                        );

                        return true;
                    });
                }));
            }
        });
    }

    private void selectInfo(DeathInfo info) {
        if (this.storage instanceof RemoteDeathLogStorage remoteStorage) remoteStorage.fetchCompleteInfo(info);

        this.detailPanel.<FlowLayout>configure(panel -> {
            panel.clearChildren();

            if (info.isPartial()) {
                panel.child(UIComponents.label(Component.translatable("text.deathlog.death_info_loading")).margins(Insets.top(15)));
                return;
            }

            panel.child(UIComponents.label(info.getTitle()).shadow(true).margins(Insets.of(15, 10, 0, 0)));

            FlowLayout leftColumn;
            FlowLayout rightColumn;
            panel.child(UIContainers.horizontalFlow(Sizing.content(), Sizing.content())
                    .child(leftColumn = UIContainers.verticalFlow(Sizing.content(), Sizing.content()))
                    .child(rightColumn = UIContainers.verticalFlow(Sizing.content(), Sizing.content())));

            leftColumn.gap(2);
            for (var text : info.getLeftColumnText()) {
                leftColumn.child(UIComponents.label(text).shadow(true));
            }

            rightColumn.gap(2).margins(Insets.left(5));
            for (var text : info.getRightColumnText()) {
                rightColumn.child(UIComponents.label(text));
            }

            FlowLayout itemContainer;
            panel.child(itemContainer = UIContainers.verticalFlow(Sizing.content(), Sizing.content()));
            itemContainer.margins(Insets.top(5));

            itemContainer.child(UIComponents.texture(Identifier.fromNamespaceAndPath("deathlog", "textures/gui/inventory_overlay.png"), 0, 0, 210, 107));

            FlowLayout armorFlow;
            itemContainer.child(armorFlow = UIContainers.verticalFlow(Sizing.content(), Sizing.content()));

            armorFlow.positioning(Positioning.absolute(185, 28));
            for (int i = 0; i < info.getPlayerArmor().size(); i++) {
                armorFlow.child(0, this.makeItem(info.getPlayerArmor().get(i), Insets.of(1)));
            }

            GridLayout itemGrid;
            itemContainer.child(itemGrid = UIContainers.grid(Sizing.content(), Sizing.content(), 4, 9));

            var inventory = info.getPlayerItems();

            itemGrid.positioning(Positioning.absolute(7, 24));
            for (int i = 0; i < 9; i++) {
                itemGrid.child(this.makeItem(inventory.get(i), Insets.of(5, 1, 1, 1)), 3, i);
            }

            for (int i = 0; i < 27; i++) {
                itemGrid.child(this.makeItem(inventory.get(9 + i), Insets.of(1)), i / 9, i % 9);
            }

            if (!inventory.get(36).isEmpty()) {
                itemContainer.child(this.makeItem(inventory.get(36), Insets.none()).positioning(Positioning.absolute(186, 8)));
            }
        });
    }

    private ItemComponent makeItem(ItemStack stack, Insets margins) {
        var item = UIComponents.item(stack).showOverlay(true);
        item.margins(margins);

        if (!stack.isEmpty()) {
            var tooltip = stack.getTooltipLines(Item.TooltipContext.EMPTY, minecraft.player, minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
            tooltip.add(Component.translatable(this.minecraft.player.isCreative() ? "text.deathlog.action.give_item.spawn" : "text.deathlog.action.give_item.copy_give"));
            item.tooltip(tooltip);

            item.mouseDown().subscribe((MouseButtonEvent click, boolean doubled) -> {
                if (click.button() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return false;

                if (this.minecraft.player.isCreative()) {
                    this.minecraft.gameMode.handleCreativeModeItemDrop(stack);
                } else {
                    var command = "/give " + minecraft.player.getName().getString() +
                            " " +
                            BuiltInRegistries.ITEM.getKey(stack.getItem());

                    var ops = storage.registries().createSerializationContext(NbtOps.INSTANCE);
                    var components = stack.getComponentsPatch().entrySet().stream().flatMap(entry -> {
                        var componentType = entry.getKey();
                        var typeId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(componentType);
                        if (typeId == null) return Stream.empty();

                        var componentOptional = entry.getValue();
                        if (componentOptional.isPresent()) {
                            TypedDataComponent<?> component = TypedDataComponent.createUnchecked(componentType, componentOptional.get());
                            return component.encodeValue(ops).result().stream().map(value -> typeId + "=" + value);
                        } else {
                            return Stream.of("!" + typeId);
                        }
                    }).collect(Collectors.joining(String.valueOf(',')));

                    if (!components.isEmpty()) {
                        command += "[" + components + "]";
                    }

                    if (stack.getCount() > 1) {
                        command += " " + stack.getCount();
                    }

                    this.minecraft.keyboardHandler.setClipboard(command);
                }

                return true;
            });
        }

        return item;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    static {
        UIParsing.registerFactory(Identifier.fromNamespaceAndPath("deathlog", "death-list-entry-container"), element -> new DeathListEntryContainer());
    }
}
