/*
 */
package com.infinityraider.agricraft.api.v1.seed;

import com.infinityraider.agricraft.api.v1.plant.IAgriPlant;
import com.infinityraider.agricraft.api.v1.stat.IAgriStat;
import java.util.Objects;
import javax.annotation.Nonnull;

import com.infinityraider.agricraft.reference.AgriCraftConfig;
import net.minecraft.item.ItemStack;

/**
 * A simple class for representing seeds. Seeds are immutable objects, for
 * safety reasons.
 *
 *
 */
public final class AgriSeed {

    @Nonnull
    private final IAgriPlant plant;
    @Nonnull
    private final IAgriStat stat;

    public AgriSeed(@Nonnull IAgriPlant plant, @Nonnull IAgriStat stat) {
        this.plant = Objects.requireNonNull(plant, "The plant in an AgriSeed may not be null!");
        this.stat = Objects.requireNonNull(stat, "The stat in an AgriSeed may not be null!");
    }

    @Nonnull
    public IAgriPlant getPlant() {
        return this.plant;
    }

    @Nonnull
    public IAgriStat getStat() {
        return this.stat;
    }

    @Nonnull
    public double getEffectiveGrowthChance() {
        return (                          this.plant.getGrowthChanceBase()
                + this.stat.getGrowth() * this.plant.getGrowthChanceBonus())
                * AgriCraftConfig.growthMultiplier;
    }

    @Nonnull
    public AgriSeed withPlant(@Nonnull IAgriPlant plant) {
        return new AgriSeed(plant, stat);
    }

    @Nonnull
    public AgriSeed withStat(@Nonnull IAgriStat stat) {
        return new AgriSeed(plant, stat);
    }

    public ItemStack toStack() {
        ItemStack stack = this.plant.getSeed().copy();
        this.stat.writeToNBT(stack.getTagCompound());
        return stack;
    }

    public ItemStack toStack(int size) {
        ItemStack stack = this.plant.getSeed().copy();
        this.stat.writeToNBT(stack.getTagCompound());
        stack.stackSize = size;
        return stack;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AgriSeed) {
            final AgriSeed other = (AgriSeed) obj;
            return (this.plant.equals(other.plant))
                    && (this.stat.equals(other.stat));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + Objects.hashCode(this.plant);
        hash = 71 * hash + Objects.hashCode(this.stat);
        return hash;
    }

}
