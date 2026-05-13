package org.example.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FishBag {

    private int maxSlots = 10;

    private final List<CaughtFish> slots = new ArrayList<>();

    public boolean add(CaughtFish caught) {
        if (isFull()) return false;
        slots.add(caught);
        return true;
    }

    public CaughtFish remove(int index) {
        return slots.remove(index);
    }

    public void clear() {
        slots.clear();
    }

    public int totalValue() {
        int total = 0;
        for (CaughtFish c : slots) total += c.value();
        return total;
    }

    public List<CaughtFish> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    public void expandSlots(int count)  { maxSlots += count; }
    public void setMaxSlots(int n)      { maxSlots = n; }
    public int  getMaxSlots()           { return maxSlots; }

    public boolean isFull()  { return slots.size() >= maxSlots; }
    public boolean isEmpty() { return slots.isEmpty(); }
    public int     size()    { return slots.size(); }
}
