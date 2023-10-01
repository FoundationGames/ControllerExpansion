package io.github.foundationgames.controllerexpansion.screen.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.foundationgames.controllerexpansion.screen.ControllerCraftingScreen;
import io.github.foundationgames.controllerexpansion.util.menu.GroupingStrategy;
import io.github.foundationgames.controllerexpansion.util.menu.ItemCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.recipebook.RecipeBookGhostSlots;
import net.minecraft.client.gui.screen.recipebook.RecipeDisplayListener;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeGridAligner;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class RecipeCatalogWidget extends DrawableHelper implements RecipeGridAligner<Ingredient>, Drawable, Element, Selectable, RecipeDisplayListener {
    private static final Identifier TEXTURE = ControllerCraftingScreen.TEXTURE;

    private final RecipeMatcher recipeMatcher = new RecipeMatcher();
    private final ClientRecipeBook recipes;

    private final MinecraftClient client;
    private final RecipeBookGhostSlots uncraftableGhostSlots = new RecipeBookGhostSlots();
    private final RecipeBookGhostSlots craftableGhostSlots = new CraftableGhostSlots();
    private int x, y, width, maxRowSize;
    private AbstractRecipeScreenHandler<?> screenHandler;

    private RecipeBookGhostSlots activeGhostSlots = craftableGhostSlots;
    private boolean focused;
    private ItemCategory currentCategory;
    private Text categoryTitle;
    private List<CatalogRecipeResult> results = List.of();
    private int startOffset;
    private int selected;
    private float recipeTooltipTime;
    private boolean takeCraftResult = false;

    public RecipeCatalogWidget(MinecraftClient client) {
        this.client = client;
        this.recipes = client.player.getRecipeBook();
    }

    public void init(int x, int y, int width, AbstractRecipeScreenHandler<?> screenHandler) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.maxRowSize = (width - 28) / 23;
        this.screenHandler = screenHandler;
    }

    public void open(ItemCategory category, boolean resetSelect) {
        Recipe<?> lastRecipe = null;
        if (this.getSelected() != null) {
            lastRecipe = this.getSelected().getSelectedRecipe();
        }

        this.currentCategory = category;
        this.categoryTitle = category.createTitle();
        this.recipeMatcher.clear();
        this.screenHandler.populateRecipeFinder(this.recipeMatcher);
        this.client.player.getInventory().populateRecipeFinder(this.recipeMatcher);

        var ungrouped = Lists.newArrayList(this.recipes.getResultsForGroup(RecipeBookGroup.CRAFTING_SEARCH));
        final var drm = this.client.world.getRegistryManager();

        var results = GroupingStrategy.group(ungrouped).stream().map(list -> {
            var recipe = new RecipeResultCollection(drm, list);
            recipe.initialize(this.recipes);
            return recipe;
        }).collect(Collectors.toList());

        results.forEach(res -> res.computeCraftables(this.recipeMatcher, this.screenHandler.getCraftingWidth(), this.screenHandler.getCraftingHeight(), this.recipes));
        results.removeIf(res ->
                !res.isInitialized() ||
                !res.hasFittingRecipes() ||
                res.getAllRecipes().stream()
                    .noneMatch(rec ->
                            rec.fits(this.screenHandler.getCraftingWidth(), this.screenHandler.getCraftingHeight()) &&
                            category.filter().test(rec.getOutput(res.getRegistryManager()))
                    )
        );

        var craftables = Lists.newArrayList(results);
        var uncraftables = Lists.newArrayList(results);

        craftables.removeIf(res -> !res.hasCraftableRecipes());
        uncraftables.removeIf(RecipeResultCollection::hasCraftableRecipes);

        craftables.addAll(uncraftables);

        this.results = craftables.stream().map(coll -> new CatalogRecipeResult(this.client, coll)).collect(Collectors.toList());

        if (resetSelect || lastRecipe == null) {
            this.selected = 0;
            this.startOffset = 0;
        } else {
            int selectDiff = this.selected - this.startOffset;
            int toSelect = 0;
            for (; toSelect < this.results.size(); toSelect++) {
                var result = this.results.get(toSelect);
                if (result.trySetRecipe(lastRecipe)) {
                    break;
                }
            }

            toSelect = MathHelper.clamp(toSelect, 0, this.results.size() - 1);
            this.setSelectionPosition(toSelect, this.results.size() < this.maxRowSize ? this.startOffset : toSelect - selectDiff);
        }

        this.setFocused(true);
    }

    public void refresh(boolean resetSelect) {
        this.open(this.currentCategory, resetSelect);
    }

    public @Nullable CatalogRecipeResult getSelected() {
        if (this.isFocused() && this.results.size() > 0) {
            return this.results.get(this.selected);
        }

        return null;
    }

    public ItemCategory getCurrentCategory() {
        return this.currentCategory;
    }

    public void setSelectionPosition(int selected, int offset) {
        if (this.results.size() > 0) {
            this.selected = MathHelper.clamp(selected, 0, this.results.size() - 1);

            int maxOffset = this.selected;
            int minOffset = Math.max(Math.min(this.results.size() - (this.maxRowSize - 1), this.selected - (this.maxRowSize - 1)), 0);
            this.startOffset = MathHelper.clamp(offset, minOffset, maxOffset);
        } else {
            this.selected = 0;
            this.startOffset = 0;
        }
    }

    public void setSelected(int selected) {
        this.setSelectionPosition(selected, this.startOffset);
    }

    public boolean changeSelected(int by) {
        int prev = this.selected;
        this.setSelected(this.selected + by);
        return this.selected != prev;
    }

    public void populateRecipe(CatalogRecipeResult result, List<Slot> slots) {
        var recipe = result.getSelectedRecipe();

        this.activeGhostSlots = result.isCraftable() ? this.craftableGhostSlots : this.uncraftableGhostSlots;

        var output = recipe.getOutput(this.client.world.getRegistryManager());
        this.activeGhostSlots.setRecipe(recipe);
        this.activeGhostSlots.addSlot(Ingredient.ofStacks(output), slots.get(0).x, slots.get(0).y);
        this.alignRecipeToGrid(this.screenHandler.getCraftingWidth(), this.screenHandler.getCraftingHeight(),
                this.screenHandler.getCraftingResultSlotIndex(), recipe, recipe.getIngredients().iterator(), 0);
    }

    public void refreshRecipe() {
        if (this.getSelected() != null) {
            this.resetGhostSlots();
            this.populateRecipe(this.getSelected(), this.screenHandler.slots);
            this.recipeTooltipTime = 35;
        }
    }

    public RecipeBookGhostSlots getGhostSlots() {
        return this.activeGhostSlots;
    }

    public void tick() {
        var resultSlot = this.screenHandler.slots.get(0);
        if (this.takeCraftResult && resultSlot.hasStack()) {
            this.client.interactionManager.clickSlot(this.screenHandler.syncId, resultSlot.id, 0, SlotActionType.QUICK_MOVE, this.client.player);
            this.takeCraftResult = false;

            this.refresh(false);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.categoryTitle != null) {
            int titleX = this.x + (this.width / 2);
            titleX -= this.client.textRenderer.getWidth(this.categoryTitle) / 2;
            this.client.textRenderer.draw(matrices, this.categoryTitle, titleX, this.y + 6, 0xFFFFFF);
        }

        if (this.results.size() > 0) {
            int x = this.x + (this.width - this.maxRowSize * 23) / 2;
            int count = Math.min(this.maxRowSize, this.results.size() - this.startOffset);
            int index;

            for (int i = 0; i < count; i++) {
                index = i + this.startOffset;
                this.results.get(index).render(matrices, x + i * 23, this.y + 19, delta,
                        this.isFocused() && this.selected == index);
            }
            for (int i = 0; i < count; i++) {
                index = i + this.startOffset;
                this.results.get(index).renderForeground(matrices, x + i * 23, this.y + 19,
                        this.isFocused() && this.selected == index);
            }
        }

        if (this.recipeTooltipTime > 0) {
            this.recipeTooltipTime -= delta;
        }

        RenderSystem.setShaderTexture(0, TEXTURE);
        boolean recipesLeft = this.startOffset > 0;
        boolean recipesRight = (this.startOffset + this.maxRowSize) < this.results.size();
        int sideSpace = (this.width - (23 * this.maxRowSize)) / 2;
        drawTexture(matrices, (this.x + sideSpace) - 10, this.y + 25, 162, recipesLeft ? 199 : 188, 7, 11);
        drawTexture(matrices, (this.x + sideSpace + 23 * this.maxRowSize) + 3, this.y + 25, 169, recipesRight ? 199 : 188, 7, 11);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + 61) {
            if (!this.isFocused()) {
                this.setFocused(true);
            }

            int catalogX = this.x + (this.width - this.maxRowSize * 23) / 2;
            int positionBasedIndex = this.startOffset + (int) Math.floor((mouseX - catalogX) / 23);
            int maxIndex = MathHelper.clamp(this.startOffset + this.maxRowSize, 0, this.results.size());
            int minIndex = Math.max(this.startOffset - 1, 0);
            this.setSelected(MathHelper.clamp(positionBasedIndex, minIndex, maxIndex));

            this.refreshRecipe();
        } else {
            this.setFocused(false);
        }

        return Element.super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (scanCode == 0) {
            return false;
        }

        if (Screen.hasShiftDown()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> this.changeCategory(-1);
                case GLFW.GLFW_KEY_RIGHT -> this.changeCategory(1);
                default -> {
                    return false;
                }
            }
            return true;
        } else if (this.getSelected() != null) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_UP -> this.navigate(NavigationDirection.UP);
                case GLFW.GLFW_KEY_DOWN -> this.navigate(NavigationDirection.DOWN);
                case GLFW.GLFW_KEY_LEFT -> this.navigate(NavigationDirection.LEFT);
                case GLFW.GLFW_KEY_RIGHT -> this.navigate(NavigationDirection.RIGHT);
                case GLFW.GLFW_KEY_ENTER -> {
                    this.craft();
                    this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                default -> {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    public void changeCategory(int by) {
        int idx = ItemCategory.CRAFTING_CATEGORIES.indexOf(this.currentCategory);
        idx = MathHelper.floorMod(idx + by, ItemCategory.CRAFTING_CATEGORIES.size());
        this.open(ItemCategory.CRAFTING_CATEGORIES.get(idx), true);
    }

    public boolean navigate(NavigationDirection nav) {
        if (this.getSelected() != null) {
            boolean changed = switch (nav) {
                case UP -> this.getSelected().changeRecipe(1);
                case DOWN -> this.getSelected().changeRecipe(-1);
                case LEFT -> this.changeSelected(-1);
                case RIGHT -> this.changeSelected(1);
            };

            this.refreshRecipe();
            return changed;
        }

        return false;
    }

    public boolean craft() {
        var result = this.getSelected();
        if (result != null && result.isCraftable()) {
            this.client.interactionManager.clickRecipe(this.client.player.currentScreenHandler.syncId,
                    this.getSelected().getSelectedRecipe(), false);
            this.takeCraftResult = true;
            return true;
        }

        return false;
    }

    @Override
    public SelectionType getType() {
        return this.isFocused() ? SelectionType.FOCUSED : SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        // TODO
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;

        if (this.getSelected() != null) {
            this.refreshRecipe();
        } else {
            this.resetGhostSlots();
        }
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public void onRecipesDisplayed(List<Recipe<?>> recipes) {
        for (var recipe : recipes) {
            this.client.player.onRecipeDisplayed(recipe);
        }
    }

    @Override
    public void acceptAlignedInput(Iterator<Ingredient> inputs, int slotId, int amount, int gridX, int gridY) {
        Ingredient ingredient = inputs.next();

        if (!ingredient.isEmpty()) {
            var slot = this.screenHandler.slots.get(slotId);
            this.activeGhostSlots.addSlot(ingredient, slot.x, slot.y);
        }
    }

    public void resetGhostSlots() {
        this.uncraftableGhostSlots.reset();
        this.craftableGhostSlots.reset();
    }

    public static class CatalogRecipeResult {
        public final RecipeResultCollection results;
        private final MinecraftClient client;
        private final List<Recipe<?>> craftableRecipes;
        private final List<Recipe<?>> allRecipes;
        private int selectedRecipe;
        private float time;

        public CatalogRecipeResult(MinecraftClient client, RecipeResultCollection recipes) {
            this.results = recipes;
            this.client = client;

            this.craftableRecipes = this.results.getRecipes(true);
            this.allRecipes = Lists.newArrayList(this.results.getAllRecipes());
            this.allRecipes.removeIf(re -> !client.player.getRecipeBook().contains(re));

            if (this.craftableRecipes.size() > 0) {
                this.selectedRecipe = this.allRecipes.indexOf(this.craftableRecipes.get(0));
            }
        }

        private int estimateIndexOf(Recipe<?> other) {
            if (this.allRecipes.contains(other)) {
                return this.allRecipes.indexOf(other);
            }

            var drm = this.results.getRegistryManager();
            return this.allRecipes.stream()
                    .filter(recipe ->
                            recipe.getGroup().equals(other.getGroup()) && recipe.getOutput(drm).isItemEqual(other.getOutput(drm)))
                    .findFirst().map(this.allRecipes::indexOf).orElse(-1);
        }

        public boolean isCraftable() {
            return this.craftableRecipes.contains(this.getSelectedRecipe());
        }

        public Recipe<?> getSelectedRecipe() {
            return this.allRecipes.get(selectedRecipe);
        }

        public Recipe<?> getOffsetRecipe(int by) {
            return this.allRecipes.get(MathHelper.floorMod(this.selectedRecipe + by, this.allRecipes.size()));
        }

        public boolean changeRecipe(int by) {
            int prev = this.selectedRecipe;
            this.setRecipe(this.selectedRecipe + by);
            return prev != this.selectedRecipe;
        }

        public void setRecipe(int recipe) {
            this.selectedRecipe = MathHelper.floorMod(recipe, this.allRecipes.size());
        }

        public boolean trySetRecipe(Recipe<?> recipe) {
            int idx = this.estimateIndexOf(recipe);
            if (idx >= 0) {
                this.setRecipe(idx);
                return true;
            }
            return false;
        }

        public void render(MatrixStack matrices, int x, int y, float delta, boolean selected) {
            RenderSystem.setShaderTexture(0, TEXTURE);

            boolean craftable = this.isCraftable();
            drawTexture(matrices, x, y, 162, craftable ? 233 : 210, 23, 23);

            this.renderRecipeIcon(matrices, this.getSelectedRecipe(), x + 3, y + 3);

            if (selected && this.allRecipes.size() > 1) {
                this.drawOffsetRecipe(matrices, this.getOffsetRecipe(1), x + 1, y - 19);
                this.drawOffsetRecipe(matrices, this.getOffsetRecipe(-1), x + 1, y + 21);
            }

            this.time += delta;
        }

        public void renderForeground(MatrixStack matrices, int x, int y, boolean selected) {
            if (selected) {
                RenderSystem.setShaderTexture(0, TEXTURE);
                if (this.allRecipes.size() > 1) {
                    drawTexture(matrices, x - 2, y - 24, 185, 186, 26, 70);
                } else {
                    drawTexture(matrices, x - 2, y - 2, 211, 230, 26, 26);
                }
            }
        }

        protected void drawOffsetRecipe(MatrixStack matrices, Recipe<?> recipe, int x, int y) {
            boolean craftable = this.craftableRecipes.contains(recipe);
            RenderSystem.setShaderTexture(0, TEXTURE);

            drawTexture(matrices, x, y, 211, craftable ? 210 : 190, 20, 20);
            this.renderRecipeIcon(matrices, recipe, x + 2, y + 2);
        }

        protected void renderRecipeIcon(MatrixStack matrices, Recipe<?> recipe, int x, int y) {
            var output = recipe.getOutput(this.results.getRegistryManager());

            if (this.allRecipes.stream().anyMatch(re -> re != recipe && re.getOutput(this.results.getRegistryManager()).isItemEqual(output))) {
                var ingredients = recipe.getIngredients();
                var stacks = ingredients.get((int)((this.time / 60) % ingredients.size())).getMatchingStacks();

                if (stacks.length > 0) {
                    var ingredient = stacks[(int)((this.time / 20) % stacks.length)];

                    matrices.push();
                    matrices.scale(0.5f, 0.5f, 0.5f);
                    client.getItemRenderer().renderInGui(matrices, ingredient, (x * 2) - 1, (y * 2));
                    matrices.pop();
                }
            }

            client.getItemRenderer().renderInGui(matrices, output, x, y);
        }
    }
}
