package com.infinityraider.agricraft.items.modes;

import com.infinityraider.agricraft.api.v1.adapter.IAgriAdapter;
import com.infinityraider.agricraft.api.v1.fertilizer.IAgriFertilizable;
import com.infinityraider.agricraft.api.v1.fertilizer.IAgriFertilizer;
import com.infinityraider.agricraft.init.AgriItems;
import com.infinityraider.agricraft.items.ItemDebugger;
import com.infinityraider.infinitylib.utility.WorldHelper;
import com.infinityraider.infinitylib.utility.debug.DebugMode;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;

public class DebugModeFertilize extends DebugMode implements IAgriFertilizer, IAgriAdapter<IAgriFertilizer> {

    public static final DebugModeFertilize INSTANCE = new DebugModeFertilize();
    //private static final ItemStack DEBUGGER = new ItemStack(new ItemDebugger(), 1, 0);

    public DebugModeFertilize () {
    }

    // DebugMode interface.
    // <editor-fold>

    @Override
    public String debugName() {
        return "fertilize";
    }

    @Override
    public void debugActionBlockClicked(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return;
        }
        Optional<IGrowable> blockGrowable = WorldHelper.getBlock(world, pos, IGrowable.class);
        player.addChatComponentMessage(new TextComponentString("Clicked block at: (" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")"));
        player.addChatComponentMessage(new TextComponentString("Name?             " + world.getBlockState(pos).getBlock().getLocalizedName()));
        player.addChatComponentMessage(new TextComponentString("IGrowable?        " + blockGrowable.isPresent()));
        blockGrowable.ifPresent(ig -> {
            IBlockState state = world.getBlockState(pos);
            player.addChatComponentMessage(new TextComponentString(" - canGrow?        " + ig.canGrow(world, pos, state, false)));
            player.addChatComponentMessage(new TextComponentString(" - canUseBonemeal? " + ig.canUseBonemeal(world, world.rand, pos, state)));
            player.addChatComponentMessage(new TextComponentString(" - Calling grow..."));
            ig.grow(world, world.rand, pos, state);
        });

    }

    @Override
    public void debugActionClicked(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {
        if (world.isRemote) {
            return;
        }
        player.addChatComponentMessage(new TextComponentString("dAC wonders why you would do that."));
    }

    @Override
    public void debugActionEntityClicked(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        if (!target.isServerWorld()) {
            return;
        }
        Optional<IAgriFertilizable> teFertilizable = WorldHelper.getTile(target.getEntityWorld(), target.getPosition(), IAgriFertilizable.class);
        player.addChatComponentMessage(new TextComponentString("dAEC says, that's " + target.getName()));
        player.addChatComponentMessage(new TextComponentString("IFertilizable?    " + teFertilizable.isPresent()));
        teFertilizable.ifPresent(te -> te.onApplyFertilizer(this, target.getRNG()));
    }

    // </editor-fold>
    // **************


    // IAgrifertilizer methods.
    // - These should be able to block being able to use this as a fertilizer.
    // <editor-fold>

    @Override
    public boolean canTriggerMutation() {
        return false;
    }

    @Override
    public boolean applyFertilizer(EntityPlayer player, World world, BlockPos pos, IAgriFertilizable target, ItemStack stack, Random random) {
        if (world.isRemote) {
            return false;
        }
        player.addChatComponentMessage(new TextComponentString("aF let's you know, we won't fertilize it."));
        return false;
    }

    @Override
    public void performClientAnimations(int meta, World world, BlockPos pos) {

    }

    // </editor-fold>
    // **************


    // IAgriAdapter<IAgriFertilizer>
    // - These are part of making the debugger appear to be a fertilizer.
    // <editor-fold>

    @Override
    public boolean accepts(@Nullable Object obj) {
        ItemStack debugger = new ItemStack(AgriItems.getInstance().DEBUGGER, 1, 0);
        return obj instanceof ItemStack && debugger.isItemEqual((ItemStack) obj);
    }

    @Nonnull
    @Override
    public Optional<IAgriFertilizer> valueOf(@Nullable Object obj) {
        if (this.accepts(obj)) {
            return Optional.of(this);
        } else {
            return Optional.empty();
        }
    }

    // </editor-fold>
    // **************
}
