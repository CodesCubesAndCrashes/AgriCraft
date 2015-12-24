package com.InfinityRaider.AgriCraft.renderers.blocks;

import com.InfinityRaider.AgriCraft.init.Blocks;
import com.InfinityRaider.AgriCraft.reference.Constants;
import com.InfinityRaider.AgriCraft.reference.Reference;
import com.InfinityRaider.AgriCraft.renderers.TessellatorV2;
import com.InfinityRaider.AgriCraft.renderers.models.ModelSprinkler;
import com.InfinityRaider.AgriCraft.tileentity.irrigation.TileEntitySprinkler;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RenderSprinkler extends RenderBlockBase {
    private ResourceLocation texture;
    private final ModelSprinkler model;
    private final TileEntitySprinkler sprinklerDummy;

    public RenderSprinkler() {
        super(Blocks.blockSprinkler, new TileEntitySprinkler(), true);
        this.texture = new ResourceLocation(Reference.MOD_ID.toLowerCase()+":textures/blocks/sprinkler.png");
        this.model = new ModelSprinkler();
        this.sprinklerDummy = new TileEntitySprinkler();
    }

    @Override
    protected boolean doWorldRender(TessellatorV2 tessellator, IBlockAccess world, double x, double y, double z, BlockPos pos, IBlockState state, Block block, TileEntity tile, int modelId, float f) {
        TileEntitySprinkler sprinkler= (TileEntitySprinkler) tile;
        //render the model
        GL11.glDisable(GL11.GL_LIGHTING);
        Minecraft.getMinecraft().renderEngine.bindTexture(this.texture);
        GL11.glTranslatef(0.5F, 1.5F, 0.5F);
        float angle = sprinkler.angle;
        GL11.glRotatef(angle, 0, -1, 0);
        GL11.glPushMatrix();
        GL11.glRotatef(180, 0F, 0F, 1F);
        this.model.render(null, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        GL11.glPopMatrix();
        GL11.glRotatef(-angle, 0, -1, 0);
        GL11.glTranslatef(-0.5F, -1.5F, -0.5F);
        GL11.glEnable(GL11.GL_LIGHTING);
        renderConnection(sprinkler);
        return true;
    }

    private void renderConnection(TileEntitySprinkler sprinkler) {
        //set up tessellator
        TessellatorV2 tessellator = TessellatorV2.instance;
        //start GL
        GL11.glPushMatrix();
            //disable lighting so the plants render bright
            GL11.glDisable(GL11.GL_LIGHTING);
            //bind the texture
            Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
            tessellator.startDrawingQuads();
            tessellator.addTranslation(0, 4 * Constants.UNIT, 0);
            drawScaledPrism(tessellator, 4, 8, 4, 12, 16, 12, net.minecraft.init.Blocks.planks.colorMultiplier(sprinkler.getWorld(), sprinkler.getPos()), sprinkler.getTexture(null));
            tessellator.addTranslation(0, -4 * Constants.UNIT, 0);
            tessellator.draw();
            //don't forget to enable lighting again
            GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }
}
