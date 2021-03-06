package com.creativemd.littletiles.common.container;

import java.util.Iterator;

import com.creativemd.creativecore.gui.container.SubContainer;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.tiles.LittleTile;
import com.creativemd.littletiles.common.tiles.LittleTileBlock;
import com.creativemd.littletiles.common.tiles.LittleTileBlockColored;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

public class SubContainerScrewdriver extends SubContainer {

	public SubContainerScrewdriver(EntityPlayer player) {
		super(player);
	}

	@Override
	public void createControls() {
		
	}

	@Override
	public void onPacketReceive(NBTTagCompound nbt) {
		int firstX = nbt.getInteger("x1");
		int firstY = nbt.getInteger("y1");
		int firstZ = nbt.getInteger("z1");
		int secX = nbt.getInteger("x2");
		int secY = nbt.getInteger("y2");
		int secZ = nbt.getInteger("z2");
		int minX = Math.min(firstX, secX);
		int maxX = Math.max(firstX, secX);
		int minY = Math.min(firstY, secY);
		int maxY = Math.max(firstY, secY);
		int minZ = Math.min(firstZ, secZ);
		int maxZ = Math.max(firstZ, secZ);
		
		boolean colorize = nbt.hasKey("color");
		int color = nbt.getInteger("color");
		
		Block filter = null;
		int meta = -1;
		if(nbt.hasKey("filterBlock"))
		{
			filter = Block.getBlockFromName(nbt.getString("filterBlock"));
			if(nbt.hasKey("filterMeta"))
				meta = nbt.getInteger("filterMeta");
		}
		
		Block replacement = null;
		int metaReplacement = -1;
		if(nbt.hasKey("replaceBlock"))
		{
			replacement = Block.getBlockFromName(nbt.getString("replaceBlock"));
			if(nbt.hasKey("replaceMeta"))
				metaReplacement = nbt.getInteger("replaceMeta");
		}
		
		int effected = 0;
		
		for (int posX = minX; posX <= maxX; posX++) {
			for (int posY = minY; posY <= maxY; posY++) {
				for (int posZ = minZ; posZ <= maxZ; posZ++) {
					BlockPos pos = new BlockPos(posX, posY, posZ);
					TileEntity tileEntity = player.world.getTileEntity(pos);
					boolean hasChanged = false;
					if(tileEntity instanceof TileEntityLittleTiles)
					{
						TileEntityLittleTiles te = (TileEntityLittleTiles) tileEntity;
						((TileEntityLittleTiles) tileEntity).preventUpdate = true;
						for (Iterator iterator = te.getTiles().iterator(); iterator.hasNext();) {
							LittleTile tile = (LittleTile) iterator.next();
							boolean shouldEffect = tile.getClass() == LittleTileBlock.class || tile instanceof LittleTileBlockColored;
							if(filter != null)
							{
								if(((LittleTileBlock) tile).getBlock() != filter)
									shouldEffect = false;
								if(meta != -1 && ((LittleTileBlock) tile).getMeta() != meta)
									shouldEffect = false;
							}
							
							if(shouldEffect)
							{
								hasChanged = true;
								
								if(replacement != null)
								{
									((LittleTileBlock) tile).setBlock(replacement);
									if(metaReplacement != -1)
										((LittleTileBlock) tile).setMeta(metaReplacement);
								}
								
								if(colorize)
								{
									LittleTile newTile = LittleTileBlockColored.setColor((LittleTileBlock) tile, color);
									
									if(newTile != null)
										te.getTiles().set(te.getTiles().indexOf(tile), newTile);
								}
								effected++;
							}
						}
						((TileEntityLittleTiles) tileEntity).preventUpdate = false;
						if(hasChanged)
							te.updateTiles();
					}
				}
			}
		}
		player.sendMessage(new TextComponentTranslation("Done! Effected " + effected + " tiles!"));
	}

}
