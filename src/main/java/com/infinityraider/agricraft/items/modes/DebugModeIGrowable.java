/*
 */
package com.infinityraider.agricraft.items.modes;

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
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;

/**
 *
 *
 */
public class DebugModeIGrowable extends DebugMode {

    @Override
    public String debugName() {
        return "test igrowable interface";
    }

    // Saved copies of the texts that do not change.
    private static final String chatTrue  = "\u00A72True  \u00A7r";
    private static final String chatFalse = "\u00A74False \u00A7r";
    private static final String chatNotIG = "\u00A78----  ----  Block is not IGrowable.\u00A7r";
    private static final String chatInfo  =
            "x, y, z | canGrow | canUseBonemeal | BlockName\n" +
            "If this is a valid target, then all three methods\n" +
            "will get called (regardless of individual results).";

    @Override
    public void debugActionBlockClicked(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return;
        }

        // Start the string with the position of the clicked on block.
        StringBuilder outputRaw = new StringBuilder(String.format("\u00A77%1$4d,%2$4d,%3$4d\u00A7r ", pos.getX(), pos.getY(), pos.getZ()));

        // Check if the clicked on block has the IGrowable interface.
        IGrowable crop = WorldHelper.getBlock(world, pos, IGrowable.class).orElse(null);
        if (crop == null) {
            // If it does not, add a nicely formatted report, and skip the rest.
            outputRaw.append(chatNotIG);
        } else {
            // Otherwise run the tests and record the results.
            IBlockState state = world.getBlockState(pos);
            outputRaw.append(crop.canGrow(world, pos, state, false)     ? chatTrue : chatFalse);
            outputRaw.append(crop.canUseBonemeal(world, world.rand, pos, state) ? chatTrue : chatFalse);
            outputRaw.append("\u00A73");
            outputRaw.append(crop.toString().substring(5));
            outputRaw.append("\u00A7r");
            crop.grow(world, world.rand, pos, state);
        }

        // Turn the String into a chat message. Add explanatory information in a hover box.
        outputRaw.append(" \u00A78[...]\u00A7r");
        TextComponentString outputComponent = new TextComponentString(outputRaw.toString());
        TextComponentString hoverComponent  = new TextComponentString(chatInfo);
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent);
        outputComponent.getStyle().setHoverEvent(hoverEvent);

        // Now send the completed message.
        player.addChatComponentMessage(outputComponent);
    }

    @Override
    public void debugActionClicked(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {
        // NOP
    }

    @Override
    public void debugActionEntityClicked(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        // NOP
    }

}
