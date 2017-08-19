package com.infinityraider.agricraft.farming.mutation.statcalculator;

import com.agricraft.agricore.util.MathHelper;
import com.infinityraider.agricraft.api.v1.plant.IAgriPlant;
import com.infinityraider.agricraft.api.v1.stat.IAgriStatCalculator;
import com.infinityraider.agricraft.reference.AgriCraftConfig;
import java.util.Optional;
import java.util.Random;

public class StatCalculatorNormal extends StatCalculatorBase {

    /**
     * calculates the new stats based on an input stat, the nr of neighbours and
     * a divisor
     */
    @Override
    protected int calculateStat(int input, int neighbours, int divisor, Random rand) {
        if (neighbours == 1 && AgriCraftConfig.singleSpreadsIncrement) {
            neighbours = 2;
        } else if (neighbours <= 0) {
            neighbours = 1;
        }
        // nextInt(x) returns values from 0 to x-1.
        int newStat = (input + rand.nextInt(neighbours)) / divisor;
        return MathHelper.inRange(newStat, 1, AgriCraftConfig.cropStatCap);
    }

    @Override
    public boolean accepts(Object obj) {
        return (!AgriCraftConfig.hardCoreStats) && (obj instanceof IAgriPlant);
    }

    @Override
    public Optional<IAgriStatCalculator> valueOf(Object obj) {
        return accepts(obj) ? Optional.of(this) : Optional.empty();
    }
}
