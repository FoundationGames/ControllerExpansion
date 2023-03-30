package io.github.foundationgames.controllerexpansion.util;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.slot.Slot;

public class SlotPositionRemapper {
    private final Pos[] oldPositions;
    private final Slot[] slots;
    private int remapIndex = -1;

    public SlotPositionRemapper(Slot[] slots) {
        this.slots = slots;
        this.oldPositions = new Pos[slots.length];
    }

    private void trySaveOldPos() {
        if (this.oldPositions[remapIndex] == null) {
            this.oldPositions[remapIndex] = new Pos(this.slots[remapIndex].x, this.slots[remapIndex].y);
        }
    }

    public int index() {
        return remapIndex;
    }

    public Inventory slotInv() {
        return this.slots[remapIndex].inventory;
    }

    public int slotId() {
        return this.slots[remapIndex].id;
    }

    public void move(int x, int y) {
        this.trySaveOldPos();

        this.slots[remapIndex].x += x;
        this.slots[remapIndex].y += y;
    }

    public void set(int x, int y) {
        this.trySaveOldPos();

        this.slots[remapIndex].x = x;
        this.slots[remapIndex].y = y;
    }

    public boolean next() {
        if (this.remapIndex >= this.slots.length - 1) {
            return false;
        }

        this.remapIndex++;
        return true;
    }

    public void rewind() {
        for (int i = 0; i < oldPositions.length; i++) {
            var pos = oldPositions[i];
            if (pos != null) {
                this.slots[i].x = pos.x;
                this.slots[i].y = pos.y;
            }
        }
    }

    private record Pos(int x, int y) {}
}
