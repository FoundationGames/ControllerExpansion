package io.github.foundationgames.controllerexpansion.util.menu;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BannerPatternItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.recipe.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public record GroupingStrategy(Predicate<ItemStack> applies, BiPredicate<ItemStack, ItemStack> shouldGroup) {
    // Functional Items
    public static final GroupingStrategy TOOL_TYPE = new GroupingStrategy(i -> i.getItem() instanceof ToolItem, GroupingStrategy::sameItemClass);
    public static final GroupingStrategy ARMOR_TYPE = new GroupingStrategy(i -> i.getItem() instanceof ArmorItem,
            (a, b)  -> {
                if (a.getItem() instanceof ArmorItem ai && b.getItem() instanceof ArmorItem bi) {
                    return ai.getType().equals(bi.getType());
                }
                return false;
            });
    public static final GroupingStrategy BANNER_PATTERN = new GroupingStrategy(i -> i.getItem() instanceof BannerPatternItem, GroupingStrategy::always);
    public static final GroupingStrategy UNPACK_RESOURCE_BLOCK = GroupingStrategy.makeSingleWhitelistGroup(Items.COAL, Items.IRON_INGOT,
            Items.COPPER_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.LAPIS_LAZULI, Items.EMERALD, Items.NETHERITE_INGOT);
    public static final GroupingStrategy UNPACK_RAW_BLOCK = GroupingStrategy.makeSingleWhitelistGroup(Items.RAW_COPPER, Items.RAW_IRON, Items.RAW_GOLD);

    // Blocks
    public static final GroupingStrategy FURNACES = new GroupingStrategy(i ->
            i.getItem() instanceof BlockItem block && block.getBlock() instanceof AbstractFurnaceBlock, GroupingStrategy::always);
    public static final GroupingStrategy WORKSTATIONS = new GroupingStrategy(GroupingStrategy::workstation, GroupingStrategy::always);
    public static final GroupingStrategy STORAGE = new GroupingStrategy(GroupingStrategy::storage, GroupingStrategy::always);
    public static final GroupingStrategy PACK_RESOURCE_BLOCK = GroupingStrategy.makeSingleWhitelistGroup(Items.COAL_BLOCK, Items.IRON_BLOCK,
            Items.COPPER_BLOCK, Items.GOLD_BLOCK, Items.DIAMOND_BLOCK, Items.LAPIS_BLOCK, Items.EMERALD_BLOCK, Items.NETHERITE_BLOCK);
    public static final GroupingStrategy PACK_RAW_BLOCK = GroupingStrategy.makeSingleWhitelistGroup(Items.RAW_COPPER_BLOCK, Items.RAW_IRON_BLOCK, Items.RAW_GOLD_BLOCK);

    public static final List<GroupingStrategy> STRATEGIES = List.of(TOOL_TYPE, ARMOR_TYPE, BANNER_PATTERN, UNPACK_RESOURCE_BLOCK, UNPACK_RAW_BLOCK,
            FURNACES, WORKSTATIONS, STORAGE, PACK_RESOURCE_BLOCK, PACK_RAW_BLOCK);


    public static GroupingStrategy makeSingleWhitelistGroup(Item ... items) {
        final var itemList = ImmutableList.copyOf(items);

        return new GroupingStrategy(i -> itemList.contains(i.getItem()), GroupingStrategy::always);
    }

    public static boolean workstation(ItemStack i) {
        if (i.getItem() instanceof BlockItem item) {
            var block = item.getBlock();

            return block instanceof CraftingTableBlock ||
                    block instanceof CartographyTableBlock ||
                    block instanceof LoomBlock ||
                    block instanceof StonecutterBlock;
        }

        return false;
    }

    public static boolean storage(ItemStack i) {
        if (i.getItem() instanceof BlockItem item) {
            var block = item.getBlock();

            return block instanceof ChestBlock ||
                    block instanceof BarrelBlock ||
                    block instanceof ShulkerBoxBlock;
        }

        return false;
    }

    public static boolean always(ItemStack a, ItemStack b) {
        return true;
    }

    public static boolean sameItemClass(ItemStack a, ItemStack b) {
        return a.getItem().getClass().equals(b.getItem().getClass());
    }

    public static List<List<Recipe<?>>> group(List<RecipeResultCollection> list) {
        List<List<Recipe<?>>> groups = new ArrayList<>();

        for (var coll : list) {
            var recipes = coll.getAllRecipes();
            nextInputGroup:
            for (var recipe : recipes) {
                var drm = coll.getRegistryManager();
                var recipeOutput = recipe.getOutput(drm);

                for (var strategy : STRATEGIES) {
                    if (strategy.applies().test(recipe.getOutput(drm))) {
                        boolean foundGroup = false;
                        for (var group : groups) {
                            var groupOutput = group.get(0).getOutput(drm);
                            if (strategy.applies().test(groupOutput) && strategy.shouldGroup().test(recipeOutput, groupOutput)) {
                                group.addAll(recipes);
                                foundGroup = true;
                                break;
                            }
                        }

                        if (!foundGroup) {
                            groups.add(new ArrayList<>(recipes));
                        }

                        break nextInputGroup;
                    }
                }

                groups.add(new ArrayList<>(recipes));
                break;
            }
        }

        return groups;
    }
}
