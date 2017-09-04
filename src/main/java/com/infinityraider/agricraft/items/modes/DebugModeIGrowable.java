/*
 */
package com.infinityraider.agricraft.items.modes;

import com.infinityraider.agricraft.api.v1.AgriApi;
import com.infinityraider.agricraft.api.v1.util.FuzzyStack;
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

import java.util.Optional;

/**
 *
 *
 */
public class DebugModeIGrowable extends DebugMode {

    @Override
    public String debugName() {
        return "igrowable interface test";
    }

    @Override
    public void debugActionBlockClicked(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return;
        }

        player.addChatComponentMessage(new TextComponentString("Searching for an IGrowable TileEntity at: " + pos));
        IGrowable crop = WorldHelper.getBlock(world, pos, IGrowable.class).orElse(null);

        if (crop == null) {
            player.addChatComponentMessage(new TextComponentString("None found there."));
            return;
        } else {
            player.addChatComponentMessage(new TextComponentString("Found: " + crop));
        }

        IBlockState state = world.getBlockState(pos);

        player.addChatComponentMessage(new TextComponentString("Calling canGrow."));
        boolean cangrow = crop.canGrow(world, pos, state, false);
        player.addChatComponentMessage(new TextComponentString("Result: " + cangrow));

        if (!cangrow) {
            player.addChatComponentMessage(new TextComponentString("Stopping. Should NOT consume bonemeal."));
            return;
        }

        player.addChatComponentMessage(new TextComponentString("Calling canUseBonemeal."));
        boolean canuseb = crop.canUseBonemeal(world, world.rand, pos, state);
        player.addChatComponentMessage(new TextComponentString("Result: " + canuseb));

        if (!canuseb) {
            player.addChatComponentMessage(new TextComponentString("Stopping. Should consume bonemeal anyhow."));
            return;
        }

        player.addChatComponentMessage(new TextComponentString("Calling grow."));
        crop.grow(world, world.rand, pos, state);

        world.playEvent(2000, pos, 4);
        player.addChatComponentMessage(new TextComponentString("Finished."));

    }

    @Override
    public void debugActionClicked(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {
/*
        if (!world.isRemote) {
            player.addChatComponentMessage(new TextComponentString("FYI: debugActionClicked, server-side"));
        } else {
            player.addChatComponentMessage(new TextComponentString("FYI: debugActionClicked, client-side"));
        }
*/
    }

    @Override
    public void debugActionEntityClicked(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
/*
        player.addChatComponentMessage(new TextComponentString("FYI: debugActionEntityClicked on target: " + target));
*/
    }

}
