package io.github.foundationgames.controllerexpansion.util.crafting;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Predicate;

public record ItemCategory(Predicate<ItemStack> filter, String name, ItemStack... icons) {
    public static final ItemCategory ALL = new ItemCategory(item -> true, "all", stack(Items.FILLED_MAP), stack(Items.COMPASS));
    public static final ItemCategory CONSTRUCTION = new ItemCategory(item ->
            inAnyOf(item, ItemGroups.BUILDING_BLOCKS, ItemGroups.COLORED_BLOCKS, ItemGroups.FUNCTIONAL),
            "construction", stack(Blocks.BRICKS), stack(Blocks.OAK_DOOR));
    public static final ItemCategory NATURE_DECOR = new ItemCategory(item ->
            inAnyOf(item, ItemGroups.NATURAL, ItemGroups.FUNCTIONAL),
            "nature_decor", stack(Blocks.OAK_LEAVES), stack(Items.LANTERN), stack(Items.OAK_HANGING_SIGN));
    public static final ItemCategory UTILITIES_COMBAT = new ItemCategory(item ->
            inAnyOf(item, ItemGroups.TOOLS, ItemGroups.COMBAT),
            "utilities_combat", stack(Items.MINECART), stack(Items.LEATHER_CHESTPLATE), stack(Items.DIAMOND_SWORD));
    public static final ItemCategory RESOURCES = new ItemCategory(item -> inAnyOf(item, ItemGroups.INGREDIENTS),
            "resources", stack(Items.LEATHER), stack(Items.PAPER));
    public static final ItemCategory REDSTONE = new ItemCategory(item -> inAnyOf(item, ItemGroups.REDSTONE),
            "redstone", stack(Items.POWERED_RAIL), stack(Items.COMPARATOR));
    public static final ItemCategory FOOD_DRINK = new ItemCategory(item -> inAnyOf(item, ItemGroups.FOOD_AND_DRINK),
            "food_drink", stack(Items.CAKE), stack(Items.APPLE), stack(Items.BREAD));

    public static final List<ItemCategory> CATEGORIES =
            List.of(CONSTRUCTION, NATURE_DECOR, UTILITIES_COMBAT, RESOURCES, REDSTONE, FOOD_DRINK, ALL);

    public void drawIcon(MatrixStack matrices, MinecraftClient client, int x, int y) {
        matrices.push();
        switch (this.icons.length) {
            default -> client.getItemRenderer().renderInGui(matrices, this.icons[0], x, y);
            case 2 -> {
                client.getItemRenderer().renderInGui(matrices, this.icons[0], x - 3, y - 2);
                matrices.translate(0, 0, 16);
                client.getItemRenderer().renderInGui(matrices, this.icons[1], x + 3, y + 1);
            }
            case 3 -> {
                client.getItemRenderer().renderInGui(matrices, this.icons[0], x - 3, y - 2);
                matrices.translate(0, 0, 16);
                client.getItemRenderer().renderInGui(matrices, this.icons[1], x + 3, y);
                matrices.translate(0, 0, 16);
                client.getItemRenderer().renderInGui(matrices, this.icons[2], x, y + 1);
            }
        }
        matrices.pop();
    }

    public Text createTitle() {
        return Text.translatable("catalog.category." + this.name());
    }

    private static ItemStack stack(ItemConvertible item) {
        return new ItemStack(item);
    }

    private static boolean inAnyOf(ItemStack stack, ItemGroup ... groups) {
        for (var group : groups) {
            if (group.getDisplayStacks().size() <= 0) {
                var world = MinecraftClient.getInstance().world;
                group.updateEntries(new ItemGroup.DisplayContext(world.getEnabledFeatures(), false, world.getRegistryManager()));
            }

            if (group.getDisplayStacks().stream().anyMatch(stack::isItemEqual)) {
                return true;
            }
        }

        return false;
    }
}
