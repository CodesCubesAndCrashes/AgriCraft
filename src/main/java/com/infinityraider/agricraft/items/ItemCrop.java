package com.infinityraider.agricraft.items;

import com.agricraft.agricore.core.AgriCore;
import com.infinityraider.agricraft.api.v1.AgriApi;
import com.infinityraider.agricraft.api.v1.util.MethodResult;
import com.infinityraider.agricraft.init.AgriBlocks;
import com.infinityraider.agricraft.items.tabs.AgriTabs;
import com.infinityraider.agricraft.reference.AgriCraftConfig;
import com.infinityraider.agricraft.tiles.TileEntityCrop;
import com.infinityraider.agricraft.utility.StackHelper;
import com.infinityraider.infinitylib.item.IItemWithModel;
import com.infinityraider.infinitylib.item.ItemBase;
import com.infinityraider.infinitylib.utility.IRecipeRegister;
import com.infinityraider.infinitylib.utility.WorldHelper;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemCrop extends ItemBase implements IItemWithModel, IRecipeRegister {

    public ItemCrop() {
        super("crop_sticks");
        this.setCreativeTab(AgriTabs.TAB_AGRICRAFT);
    }

    //I'm overriding this just to be sure
    @Override
    public boolean canItemEditBlocks() {
        return true;
    }

    /**
     * This method manages the creation of BlockCrops in the world using ItemCrops.
     * It targets a relative position based on the face provided.
     * It checks that the target is an empty space, and that it is above a valid soil.
     * It tries to be reasonably generic in order to support whatever blocks other mods might use.
     */
    @Override
    @Nonnull
    public EnumActionResult onItemUse(@Nonnull ItemStack stack, @Nullable EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos,
                                      @Nullable EnumHand hand, @Nullable EnumFacing side, float hitX, float hitY, float hitZ) {
        // Step 1: Calculate where the crop is supposed to go. If `side` is null, use `pos` instead.
        BlockPos cropPos = side == null ? pos : pos.offset(side);

        // Step 2: Verify that the conditions are valid for creating the BlockCrop.
        if (world.isRemote) {
            // Only the server handles creating the BlockCrop.
            return EnumActionResult.PASS;
        } else if (!world.isAirBlock(cropPos) || !AgriApi.getSoilRegistry().contains(world.getBlockState(cropPos.down()))) {
            // The placement position must be empty space, and above a valid block.
            return EnumActionResult.PASS;
        } else if (StackHelper.decreaseStackSize(player, stack, 1) == MethodResult.FAIL) {
            // This should never fail, but we're being careful just in case there's something wrong with the stack.
            AgriCore.getLogger("agricraft").error("Couldn't decrease stack size by one when creating a BlockCrop by right clicking with an ItemCrop.");
            return EnumActionResult.PASS;
        }

        // Step 3: Create a BlockCrop at the position.
        world.setBlockState(cropPos, AgriBlocks.getInstance().CROP.getDefaultState());

        // Step 4: Turn the crop into a cross crop if the player is crouched and has another crop stick available.
        if (player != null && player.isSneaking() && StackHelper.decreaseStackSize(player, stack, 1) != MethodResult.FAIL) {
            WorldHelper
                    .getTile(world, cropPos, TileEntityCrop.class)
                    .ifPresent(c -> c.setCrossCrop(true));
        }

        // Step 5: Play the placement sound.
        SoundType type = SoundType.PLANT;
        world.playSound(player, pos, type.getPlaceSound(), SoundCategory.BLOCKS, (type.getVolume() + 1.0F) / 4.0F, type.getPitch() * 0.8F);

        // Step 6: Report that the action was a success.
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void registerRecipes() {
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(this, AgriCraftConfig.cropsPerCraft), "ss", "ss", 's', "stickWood"));
    }

}
