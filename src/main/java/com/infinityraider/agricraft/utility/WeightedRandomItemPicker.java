package com.infinityraider.agricraft.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class WeightedRandomItemPicker<T> {
    /**
     * A list of the possible items to pick from.
     */
    private final ArrayList<T> items;
    /**
     * The sum of the weights of all the items in
     */
    private int weightSum;
    /**
     *
     */
    private final ArrayList<Integer> bounds;

    public WeightedRandomItemPicker() {
        bounds = new ArrayList<>();
        items = new ArrayList<>();
        weightSum = 0;
    }

    public void addItem (T item, int weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be a positive int.");
        }
        weightSum += weight;
        if (weightSum <= 0) {
            throw new RuntimeException("Sum of weights exceeded int MAX_VALUE.");
        }
        bounds.add(weightSum);
        items.add(item);
    }

    public T getRandomItem (Random rand) {
        // Get a random number from 1 to weightSum, inclusive of both.
        int target = rand.nextInt(weightSum) + 1;
        // Find
        int index = Collections.binarySearch(bounds, target);
        //
        if (index <= 0) {
            index = -1 * (index + 1);
        }
        return items.get(index);
    }
}
