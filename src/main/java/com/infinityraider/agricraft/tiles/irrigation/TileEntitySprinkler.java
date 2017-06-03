package com.infinityraider.agricraft.tiles.irrigation;

import com.agricraft.agricore.core.AgriCore;
import com.infinityraider.agricraft.api.v1.irrigation.IConnectable;
import com.infinityraider.agricraft.api.v1.irrigation.IIrrigationComponent;
import com.infinityraider.agricraft.api.v1.irrigation.IrrigationConnectionType;
import com.infinityraider.agricraft.api.v1.misc.IAgriDisplayable;
import com.infinityraider.agricraft.blocks.irrigation.BlockWaterChannel;
import com.infinityraider.agricraft.reference.AgriCraftConfig;
import com.infinityraider.agricraft.reference.AgriNBT;
import com.infinityraider.agricraft.reference.Constants;
import com.infinityraider.agricraft.renderers.particles.LiquidSprayFX;
import com.infinityraider.agricraft.utility.BaseIcons;
import com.infinityraider.infinitylib.block.tile.TileEntityBase;
import com.infinityraider.infinitylib.utility.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.function.Consumer;

public class TileEntitySprinkler extends TileEntityBase implements ITickable, IIrrigationComponent, IAgriDisplayable {

    private int counter = 0;
    private float angle = 0.0F;
    private static final int BUFFER_CAP        = 100;
    private static final int TICKS_PER_SECOND  = 20;
    private static final int COVERAGE_HEIGHT   = 5; // Configure here. Note: the lowest y-level will be farmland only.
    private static final int COVERAGE_RADIUS   = 3; // Configure here.
    private static final int COVERAGE_DIAMETER = 1 + 2 * COVERAGE_RADIUS;
    private static final int COVERAGE_AREA     = COVERAGE_DIAMETER * COVERAGE_DIAMETER;
    private boolean active;
    private int buffer;
    private int columnCounter;
    private int waterUsageRemainingMb;
    private int waterUsageRemainingTicks;

    public TileEntitySprinkler() {
        this.active                   = false;
        this.buffer                   = 0;
        this.columnCounter            = 0;
        this.waterUsageRemainingMb    = Integer.MAX_VALUE;
        this.waterUsageRemainingTicks = 0;
    }

    /**
     * Retrieves the current angle of the sprinkler head.
     *
     * @return The sprinkler head angle.
     */
    public float getAngle() {
        return angle;
    }

    //this saves the data on the tile entity
    @Override
    public void writeTileNBT(NBTTagCompound tag) {
        if (this.counter > 0) {
            tag.setInteger(AgriNBT.LEVEL, this.counter);
        }
        if (this.active) {
            tag.setBoolean(AgriNBT.ACTIVE, this.active);
        }
        if (this.buffer > 0) {
            tag.setInteger(AgriNBT.BUFFER, this.buffer);
        }
        if (this.columnCounter > 0) {
            tag.setInteger(AgriNBT.COLUMN_COUNTER, this.columnCounter);
        }
        if (this.waterUsageRemainingMb < Integer.MAX_VALUE) {
            tag.setInteger(AgriNBT.WATER_USAGE_REMAINING_MB, this.waterUsageRemainingMb);
        }
        if (this.waterUsageRemainingTicks > 0) {
            tag.setInteger(AgriNBT.WATER_USAGE_REMAINING_TICKS, this.waterUsageRemainingTicks);
        }
    }

    //this loads the saved data for the tile entity
    // Note: tag.get* methods *should* return zero/false if the key does not exist.
    @Override
    public void readTileNBT(NBTTagCompound tag) {
        this.counter                  = tag.getInteger(AgriNBT.LEVEL);
        this.active                   = tag.getBoolean(AgriNBT.ACTIVE);
        this.buffer                   = tag.getInteger(AgriNBT.BUFFER);
        this.columnCounter            = tag.getInteger(AgriNBT.COLUMN_COUNTER);
        this.waterUsageRemainingMb    = tag.hasKey(    AgriNBT.WATER_USAGE_REMAINING_MB) ?
                                        tag.getInteger(AgriNBT.WATER_USAGE_REMAINING_MB) : Integer.MAX_VALUE;
        this.waterUsageRemainingTicks = tag.getInteger(AgriNBT.WATER_USAGE_REMAINING_TICKS);
    }

    //checks if the sprinkler is CONNECTED to an irrigation channel
    public boolean isConnected() {
        return WorldHelper.getBlock(this.worldObj, this.pos.up(), BlockWaterChannel.class).isPresent();
    }

    @Override
    public void update() {
        if (!this.worldObj.isRemote) {
            // Step 1: Check if we need to refresh the water usage variables
            if (this.waterUsageRemainingTicks <= 0 || this.waterUsageRemainingMb < 0) {
                this.waterUsageRemainingMb    = Math.abs(AgriCraftConfig.sprinklerRatePerSecond);
                this.waterUsageRemainingTicks = TICKS_PER_SECOND;
            }

            // Step 2: Update the per-tick rate.
            // Note: This math executes regardless of actual consumption occuring, so
            //       that the usage rate refresh still happens every twenty ticks.
            // Also: waterUsageThisTick should be calculated using int/int division, and not with floats.
            final int waterUsageThisTick   = Math.abs(this.waterUsageRemainingMb / this.waterUsageRemainingTicks);
            this.waterUsageRemainingMb    -= waterUsageThisTick;
            this.waterUsageRemainingTicks -= 1;

            // Step 3: Check if there is enough water to irrigate this tick, and if this is a change.
            final boolean currentActiveness = (this.buffer >= waterUsageThisTick && this.buffer > 0);
            if (currentActiveness != this.active) {
                this.active = currentActiveness;
                this.markForUpdate();
            }

            // Step 4: If we can, sprinkle!
            if (this.active) {
                this.buffer -= waterUsageThisTick;
                this.doSprinkle();
            }
        } else if (this.active) {
            this.renderLiquidSpray();
        }
    }

    public void doSprinkle() {
        // Step 1: if we're within bounds, search the next column.
        if (this.columnCounter >= 0 && this.columnCounter < COVERAGE_AREA) {
            final int targetX = this.pos.getX() - COVERAGE_RADIUS + (this.columnCounter % COVERAGE_DIAMETER);
            final int targetZ = this.pos.getZ() - COVERAGE_RADIUS + (this.columnCounter / COVERAGE_DIAMETER);
            final int lowestY = Math.max(this.pos.getY() - COVERAGE_HEIGHT, 0); // Avoid the void.
            for (int targetY = this.pos.getY()-1; targetY >= lowestY; targetY -= 1) {
                // First gather data.
                BlockPos    target = new BlockPos(targetX, targetY, targetZ);
                IBlockState state  = this.worldObj.getBlockState(target);
                Block       block  = state.getBlock();
                // Option A: Skip empty/air blocks.
                // TODO: Is there a way to use isSideSolid to ignore minor obstructions? (Farmland isn't solid.)
                if (block.isAir(state, this.worldObj, target)) {
                    continue;
                }
                // Option B: Give plants a chance to grow, and then continue onward to irrigate the farmland too.
                if ((block instanceof IPlantable || block instanceof IGrowable) && targetY != lowestY) {
                    if (this.getRandom().nextInt(100) < AgriCraftConfig.sprinklerGrowthChance) {
                        block.updateTick(this.worldObj, target, state, this.getRandom());
                    }
                    continue;
                }
                // Option C: Dry farmland gets set as moist.
                if (block instanceof BlockFarmland) {
                    if (state.getValue(BlockFarmland.MOISTURE) < 7) {
                       this.worldObj.setBlockState(target, state.withProperty(BlockFarmland.MOISTURE, 7), 2);
                    }
                    break; // Explicitly expresses the intent to stop.
                }
                // Option D: If it's none of the above, it blocks the sprinkler's irrigation. Stop.
                break;
            }
        }

        // Step 2: Update the counter.
        this.columnCounter += 1;

        // Step 3: If the counter exceeds both minimums, or if it is (incorrectly) negative, reset it.
        if (   this.columnCounter >= COVERAGE_AREA
            && this.columnCounter >= AgriCraftConfig.sprinklerGrowthIntervalTicks
            || this.columnCounter < 0) {
            this.columnCounter = 0;
        }
    }

    @Override
    public boolean canConnectTo(EnumFacing side, IConnectable component) {
        return side.equals(EnumFacing.UP) && component instanceof TileEntityChannel;
    }

    @Override
    public boolean canAcceptFluid(int y, int amount, boolean partial) {
        if (buffer + amount <= BUFFER_CAP) {
            return true;
        } else {
            return partial;
        }
    }

    @Override
    public int acceptFluid(int y, int amount, boolean partial) {
        if (canAcceptFluid(y, amount, partial)) {
            this.buffer += amount;
            if (this.buffer > BUFFER_CAP) {
                amount = this.buffer - BUFFER_CAP;
                this.buffer = BUFFER_CAP;
            } else {
                amount = 0;
            }
        }
        return amount;
    }

    @Override
    public int getFluidAmount(int y) {
        return this.buffer;
    }

    @Override
    public int getCapacity() {
        return BUFFER_CAP;
    }

    @Override
    public void setFluidLevel(int lvl) {
        // This can be skipped... Shhh!
    }

    @Override
    public void syncFluidLevel() {
        // This can be skipped... Shhh!
    }

    @Override
    public int getFluidHeight() {
        return this.buffer;
    }

    @Override
    public float getFluidHeight(int lvl) {
        return (this.buffer * 16.0f / BUFFER_CAP);
    }

    @Override
    public IrrigationConnectionType getConnectionType(EnumFacing side) {
        if (side == EnumFacing.UP) {
            return IrrigationConnectionType.PRIMARY;
        } else {
            return IrrigationConnectionType.NONE;
        }
    }

    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite getChannelIcon() {
        // Fetch the Icon using the handy world helper class.
        return WorldHelper
                .getTile(worldObj, pos.up(), TileEntityChannel.class)
                .map(c -> c.getIcon())
                .orElse(BaseIcons.OAK_PLANKS.getIcon());
    }

    @SideOnly(Side.CLIENT)
    private void renderLiquidSpray() {
        if (AgriCraftConfig.disableParticles) {
            return;
        }
        this.angle = (this.angle + 5F) % 360;
        int particleSetting = Minecraft.getMinecraft().gameSettings.particleSetting;    //0 = all, 1 = decreased; 2 = minimal;
        counter = (counter + 1) % (particleSetting + 1);
        if (counter == 0) {
            for (int i = 0; i < 4; i++) {
                float alpha = -(this.angle + 90 * i) * ((float) Math.PI) / 180;
                double xOffset = (4 * Constants.UNIT) * Math.cos(alpha);
                double zOffset = (4 * Constants.UNIT) * Math.sin(alpha);
                float radius = 0.3F;
                for (int j = 0; j <= 4; j++) {
                    float beta = -j * ((float) Math.PI) / (8.0F);
                    Vec3d vector = new Vec3d(radius * Math.cos(alpha), radius * Math.sin(beta), radius * Math.sin(alpha));
                    this.spawnLiquidSpray(xOffset * (4 - j) / 4, zOffset * (4 - j) / 4, vector);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void spawnLiquidSpray(double xOffset, double zOffset, Vec3d vector) {
        LiquidSprayFX liquidSpray = new LiquidSprayFX(this.worldObj, this.xCoord() + 0.5F + xOffset, this.yCoord() + 8 * Constants.UNIT, this.zCoord() + 0.5F + zOffset, 0.3F, 0.7F, vector);
        Minecraft.getMinecraft().effectRenderer.addEffect(liquidSpray);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addDisplayInfo(Consumer<String> information) {
        information.accept(AgriCore.getTranslator().translate("agricraft_tooltip.waterLevel") + ": " + this.getFluidAmount(0) + "/" + BUFFER_CAP);
    }
}
