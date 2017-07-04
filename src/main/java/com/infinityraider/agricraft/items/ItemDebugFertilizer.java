package com.infinityraider.agricraft.items;

import com.infinityraider.agricraft.api.v1.adapter.IAgriAdapter;
import com.infinityraider.agricraft.api.v1.fertilizer.IAgriFertilizable;
import com.infinityraider.agricraft.api.v1.fertilizer.IAgriFertilizer;
import com.infinityraider.agricraft.api.v1.util.MethodResult;
import com.infinityraider.agricraft.init.AgriItems;
import com.infinityraider.agricraft.items.tabs.AgriTabs;
import com.infinityraider.infinitylib.handler.ConfigurationHandler;
import com.infinityraider.infinitylib.item.ItemBase;
import com.infinityraider.infinitylib.utility.WorldHelper;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ItemDebugFertilizer extends ItemBase implements IAgriFertilizer,IAgriAdapter<IAgriFertilizer> {

    public static final ItemDebugFertilizer INSTANCE = new ItemDebugFertilizer();
    //private static final ItemStack DEBUG_FERTILIZER = new ItemStack(AgriItems.getInstance().DEBUG_FERTILIZER, 1, 0);

    public ItemDebugFertilizer() {
        super("debug_fertilizer");
        this.setMaxStackSize(1);
        if (ConfigurationHandler.getInstance().debug) {
            this.setCreativeTab(AgriTabs.TAB_AGRICRAFT);
            //this.setTextureName(Main.MODID + ":tutorialItem");
        }
    }


    // ************************
    // Item and IGrowable stuff
    // <editor-fold>

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {
        if (!world.isRemote && player.isSneaking()) {
            final int mode = changeFertMode(stack, 1);
            player.addChatComponentMessage(new TextComponentString("Set debug mode to " + mode));
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if(!world.isRemote && !player.isSneaking()) {
            Optional<IGrowable> blockGrowable = WorldHelper.getBlock(world, pos, IGrowable.class);
            player.addChatComponentMessage(new TextComponentString("Clicked block at: (" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")"));
            player.addChatComponentMessage(new TextComponentString("Name?             " + world.getBlockState(pos).getBlock().getLocalizedName()));
            player.addChatComponentMessage(new TextComponentString("IGrowable?        " + blockGrowable.isPresent()));
            blockGrowable.ifPresent(ig -> {
                IBlockState state = world.getBlockState(pos);
                player.addChatComponentMessage(new TextComponentString(" - canGrow?        " + ig.canGrow(world, pos, state, false)));
                player.addChatComponentMessage(new TextComponentString(" - canUseBonemeal? " + ig.canUseBonemeal(world, world.rand, pos, state)));
                int mode = readFertMode(stack);
                if (mode % 2 == 1) {
                    player.addChatComponentMessage(new TextComponentString(" - Calling grow..."));
                    ig.grow(world, world.rand, pos, state);
                } else {
                    player.addChatComponentMessage(new TextComponentString(" - Grow is disabled currently."));
                }
            });
        }
        return EnumActionResult.PASS;
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        player.addChatComponentMessage(new TextComponentString("Moo."));
        return false;
    }

    // </editor-fold>
    // ************************


    // *****************************
    // Modes and NBT and information
    // <editor-fold>

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
        int mode = readFertMode(stack);
        boolean modeDoGrowable = mode % 2 == 1;
        mode /= 2;
        int modeFertilizer = mode % 3;
        // mode /= 3;
        String fertMode;
        switch (modeFertilizer) {
            case 0:  fertMode = "off"; break;
            case 1:  fertMode = "blocks usage"; break;
            case 2:  fertMode = "causes growth"; break;
            default: fertMode = "ERROR";
        }
        tooltip.add("Right Click to use the fertilizer in its current mode");
        tooltip.add("Shift + Right Click to cycle modes");
        tooltip.add("Current fertilizer mode:");
        tooltip.add(" - Attempt to use IGrowable: " + modeDoGrowable);
        tooltip.add(" - IAgriFertilizer adapter:  " + fertMode);
    }

    private int readFertMode(ItemStack stack) {
        return changeFertMode(stack, 0);
    }

    private int changeFertMode(ItemStack stack, int offset) {
        NBTTagCompound tag;
        if(!stack.hasTagCompound()) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        } else {
            tag = stack.getTagCompound();
        }
        assert tag != null;
        String FERT_MODE_NBT = "debugFertilizerMode";
        int mode = tag.getInteger(FERT_MODE_NBT); // getInteger returns 0 if key DNE.
        int FERT_MODE_COUNT = 6;
        if (offset != 0 || mode < 0 || mode >= FERT_MODE_COUNT) {
            mode = (mode + offset) % FERT_MODE_COUNT;
            tag.setInteger(FERT_MODE_NBT, mode);
        }
        return mode;
    }

    // </editor-fold>
    // *****************************

    // ***************
    // IAgriFertilizer
    // <editor-fold>

   @Override
    public boolean canTriggerMutation() {
        return true;
    }

    @Override
    public boolean applyFertilizer(EntityPlayer player, World world, BlockPos pos, IAgriFertilizable target, ItemStack stack, Random random) {
        int modeFertilizer = (readFertMode(stack) / 2) % 3;
        switch (modeFertilizer) {
            default:
            case 0:
                player.addChatComponentMessage(new TextComponentString("IAgriFertilizer is not interfering."));
                return false; // Allow other effects.
            case 1:
                player.addChatComponentMessage(new TextComponentString("IAgriFertilizer blocks usage."));
                return true;  // Block any other effects.
            case 2:
                if (target.acceptsFertilizer(this) && target.onApplyFertilizer(this, random) == MethodResult.SUCCESS) {
                    player.addChatComponentMessage(new TextComponentString("IAgriFertilizer successfully triggered growth."));
                    return true;
                } else {
                    player.addChatComponentMessage(new TextComponentString("IAgriFertilizer failed to trigger growth"));
                    return false;
                }
        }
    }

    @Override
    public void performClientAnimations(int meta, World world, BlockPos pos) {
        // noop
    }

    // </editor-fold>
    // ***************


    // *****************************
    // IAgriAdapter<IAgriFertilizer>
    // <editor-fold>

    @Override
    public boolean accepts(@Nullable Object obj) {
        final ItemStack DEBUG_FERTILIZER = new ItemStack(AgriItems.getInstance().DEBUG_FERTILIZER);
        return obj instanceof ItemStack && DEBUG_FERTILIZER.isItemEqual((ItemStack) obj);
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
    // *****************************
}
