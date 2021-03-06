package com.creativemd.littletiles.common.packet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.creativecore.common.utils.ColorUtils;
import com.creativemd.creativecore.common.utils.HashMapList;
import com.creativemd.creativecore.common.utils.TickUtils;
import com.creativemd.creativecore.common.utils.WorldUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.blocks.BlockTile;
import com.creativemd.littletiles.common.events.LittleEvent;
import com.creativemd.littletiles.common.items.ItemColorTube;
import com.creativemd.littletiles.common.items.ItemLittleChisel;
import com.creativemd.littletiles.common.items.ItemRubberMallet;
import com.creativemd.littletiles.common.items.ItemTileContainer;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.tiles.LittleTile;
import com.creativemd.littletiles.common.tiles.LittleTileBlock;
import com.creativemd.littletiles.common.tiles.LittleTileBlockColored;
import com.creativemd.littletiles.common.tiles.vec.LittleTileBox;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LittleBlockPacket extends CreativeCorePacket{
	
	public static enum BlockPacketAction {
		
		COLOR_TUBE(true) {
			@Override
			public void action(World world, TileEntityLittleTiles te, LittleTile tile, ItemStack stack,
					EntityPlayer player, RayTraceResult moving, BlockPos pos, NBTTagCompound nbt) {
				if((tile.getClass() == LittleTileBlock.class || tile instanceof LittleTileBlockColored))
				{
					int color = ColorUtils.WHITE;
					if(tile instanceof LittleTileBlockColored)
						color = ((LittleTileBlockColored) tile).color;
					ItemColorTube.setColor(player.getHeldItemMainhand(), color);
				}
			}
		},
		CHISEL(false) {
			@Override
			public void action(World world, TileEntityLittleTiles te, LittleTile tile, ItemStack stack,
					EntityPlayer player, RayTraceResult moving, BlockPos pos, NBTTagCompound nbt) {
				if(player.isSneaking() && tile instanceof LittleTileBlock)
				{
					ItemLittleChisel.setBlockState(stack, ((LittleTileBlock) tile).getBlockState());
					ItemLittleChisel.setColor(stack, tile instanceof LittleTileBlockColored ? ((LittleTileBlockColored) tile).color : ColorUtils.WHITE);
				}
			}
		};
		
		public final boolean rightClick;
		
		private BlockPacketAction(boolean rightClick) {
			this.rightClick = rightClick;
		}
		
		public abstract void action(World world, TileEntityLittleTiles te, LittleTile tile, ItemStack stack, EntityPlayer player, RayTraceResult moving, BlockPos pos, NBTTagCompound nbt);
	}
	
	public BlockPos blockPos;
	public Vec3d pos;
	public Vec3d look;
	public BlockPacketAction action;
	public NBTTagCompound nbt;
	
	public LittleBlockPacket()
	{
		
	}
	
	public LittleBlockPacket(BlockPos blockPos, EntityPlayer player, BlockPacketAction action)
	{
		this(blockPos, player, action, new NBTTagCompound());
	}
	
	public LittleBlockPacket(BlockPos blockPos, EntityPlayer player, BlockPacketAction action, NBTTagCompound nbt)
	{
		this.blockPos = blockPos;
		this.action = action;
		this.pos = player.getPositionEyes(TickUtils.getPartialTickTime());
		double d0 = player.capabilities.isCreativeMode ? 5.0F : 4.5F;
		Vec3d look = player.getLook(TickUtils.getPartialTickTime());
		this.look = pos.addVector(look.xCoord * d0, look.yCoord * d0, look.zCoord * d0);
		this.nbt = nbt;
	}
	
	@Override
	public void writeBytes(ByteBuf buf) {
		writePos(buf, blockPos);
		writeVec3d(pos, buf);
		writeVec3d(look, buf);
		buf.writeInt(action.ordinal());
		writeNBT(buf, nbt);
	}
	
	@Override
	public void readBytes(ByteBuf buf) {
		blockPos = readPos(buf);
		pos = readVec3d(buf);
		look = readVec3d(buf);
		action = BlockPacketAction.values()[buf.readInt()];
		nbt = readNBT(buf);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void executeClient(EntityPlayer player) {
		
	
	}
	
	@Override
	public void executeServer(EntityPlayer player) {
		TileEntity tileEntity = player.world.getTileEntity(blockPos);
		World world = player.world;
		if(tileEntity instanceof TileEntityLittleTiles)
		{
			TileEntityLittleTiles te = (TileEntityLittleTiles) tileEntity;
			LittleTile tile = te.getFocusedTile(pos, look);
			
			if(!LittleAction.isAllowedToInteract(player, blockPos, action.rightClick, EnumFacing.EAST))
			{
				te.updateBlock();
				return ;
			}
			
			if(tile != null)
			{
				ItemStack stack = player.getHeldItem(EnumHand.MAIN_HAND);
				RayTraceResult moving = te.getMoving(pos, look);
				action.action(world, te, tile, stack, player, moving, blockPos, nbt);
			}
		}
	}
	
}
