package com.creativemd.littletiles.common.action;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.littletiles.common.action.block.NotEnoughIngredientsException;
import com.creativemd.littletiles.common.blocks.ILittleTile;
import com.creativemd.littletiles.common.container.SubContainerHammer;
import com.creativemd.littletiles.common.ingredients.BlockIngredient;
import com.creativemd.littletiles.common.ingredients.BlockIngredient.BlockIngredients;
import com.creativemd.littletiles.common.ingredients.ColorUnit;
import com.creativemd.littletiles.common.ingredients.CombinedIngredients;
import com.creativemd.littletiles.common.items.ItemTileContainer;
import com.creativemd.littletiles.common.tiles.LittleTile;
import com.creativemd.littletiles.common.tiles.PlacementHelper;
import com.creativemd.littletiles.common.tiles.preview.LittleTilePreview;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class LittleAction extends CreativeCorePacket {
	
	public static int maxSavedActions = 10;
	
	private static List<LittleAction> lastActions = new ArrayList<>(maxSavedActions);
	
	private static int index = 0;
	
	public static void rememberAction(LittleAction action)
	{
		if(!action.canBeReverted())
			return ;
		
		index = 0;
		if(lastActions.size() == maxSavedActions)
			lastActions.remove(maxSavedActions-1);
		
		lastActions.add(0, action);
	}
	
	public static void registerLittleAction(String id, Class<? extends LittleAction> classType)
	{
		CreativeCorePacket.registerPacket(classType, "ac" + id);
	}	
	
	/**Must be implemented by every action**/
	public LittleAction() {
		
	}
	
	@SideOnly(Side.CLIENT)
	public abstract boolean canBeReverted();
	
	/**
	 * 
	 * @return null if an revert action is not available
	 */
	@SideOnly(Side.CLIENT)
	public abstract LittleAction revert();
	
	protected abstract boolean action(EntityPlayer player) throws LittleActionException;
	
	@SideOnly(Side.CLIENT)
	public boolean execute()
	{		
		EntityPlayer player = Minecraft.getMinecraft().player;
		
		try {
			if(action(player))
			{
				rememberAction(this);
				
				PacketHandler.sendPacketToServer(this);
				return true;
			}
		} catch (LittleActionException e) {
			player.sendStatusMessage(new TextComponentString(e.getLocalizedMessage()), true);
			return false;
		}
		
		return false;
	}
	
	@Override
	public void executeClient(EntityPlayer player) {
		//Not used yet
		
		try {
			action(player);
		} catch (LittleActionException e) {
			player.sendStatusMessage(new TextComponentString(e.getLocalizedMessage()), true);
		}
	}

	@Override
	public void executeServer(EntityPlayer player) {
		try {
			action(player);
		} catch (LittleActionException e) {
			player.sendStatusMessage(new TextComponentString(e.getLocalizedMessage()), true);
		}
	}
	
	private static Method loadWorldEditEvent()
	{
		try
		{
			Class clazz = Class.forName("com.sk89q.worldedit.forge.ForgeWorldEdit");
			worldEditInstance = clazz.getField("inst").get(null);
			return clazz.getMethod("onPlayerInteract", PlayerInteractEvent.class);
		}catch(Exception e){
			
		}
		return null;
	}
	
	private static Method WorldEditEvent = loadWorldEditEvent();
	private static Object worldEditInstance = null;
	
	public static boolean isAllowedToInteract(EntityPlayer player, BlockPos pos, boolean rightClick, EnumFacing facing)
	{
		if(player.world.isRemote)
			return true;
		if(WorldEditEvent != null)
		{
			PlayerInteractEvent event = rightClick ? new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, pos, facing, new Vec3d(pos)) : new PlayerInteractEvent.LeftClickBlock(player, pos, facing, new Vec3d(pos));
			try {
				if(worldEditInstance == null)
				{
					loadWorldEditEvent();
				}
				WorldEditEvent.invoke(worldEditInstance, event);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
			if(event.isCanceled())
				return false;
		}
		
		return !player.getServer().isBlockProtected(player.world, pos, player);
		
	}
	
	public static boolean needIngredients(EntityPlayer player)
	{
		return !player.isCreative();
	}
	
	public static boolean drainPreviews(EntityPlayer player, List<LittleTilePreview> previews) throws NotEnoughIngredientsException
	{
		if(needIngredients(player))
		{
			ColorUnit color = new ColorUnit();
			BlockIngredients ingredients = new BlockIngredients();
			for (LittleTilePreview preview : previews) {
				if(preview.canBeConvertedToBlockEntry())
				{
					ingredients.addIngredient(preview.getBlockIngredient());
					color.addColorUnit(ColorUnit.getRequiredColors(preview));
				}
			}
			return drainIngredients(player, ingredients, color);
		}
		return true;
	}
	
	public static BlockIngredient getIngredientsOfStackSimple(ItemStack stack)
	{
		Block block = Block.getBlockFromItem(stack.getItem());
		
		if(block != null && !(block instanceof BlockAir) && SubContainerHammer.isBlockValid(block))
			return new BlockIngredient(block, stack.getItemDamage(), 1);
		return null;
	}
	
	/**
	 * @return does not take care of stackSize
	 */
	public static CombinedIngredients getIngredientsOfStack(ItemStack stack)
	{
		if(!stack.isEmpty())
		{
			ILittleTile tile = PlacementHelper.getLittleInterface(stack);
			
			if(tile != null && tile.hasLittlePreview(stack) && tile.containsIngredients(stack))
			{
				List<LittleTilePreview> tiles = tile.getLittlePreview(stack);
				if(tiles != null)
				{
					CombinedIngredients ingredients = new CombinedIngredients();
					for (int i = 0; i < tiles.size(); i++) {
						LittleTilePreview preview = tiles.get(i);
						if(preview.canBeConvertedToBlockEntry())
						{
							ingredients.block.addIngredient(preview.getBlockIngredient());
							ingredients.color.addColorUnit(ColorUnit.getRequiredColors(preview));
						}
					}
					return ingredients;
				}
			}
			
			Block block = Block.getBlockFromItem(stack.getItem());
			
			if(block != null && !(block instanceof BlockAir))
			{
				if(SubContainerHammer.isBlockValid(block))
				{
					CombinedIngredients ingredients = new CombinedIngredients();
					ingredients.block.addIngredient(new BlockIngredient(block, stack.getItemDamage(), 1));
					return ingredients;
				}
			}
		}
		return null;
	}
	
	public static boolean drainIngredients(EntityPlayer player, BlockIngredients ingredients, ColorUnit unit) throws NotEnoughIngredientsException
	{
		if(needIngredients(player))
		{
			List<ItemStack> bags = getBags(player);
			List<ItemStack> usedBags = new ArrayList<>(); //Those bags will be drained in order to simulate the action.
			
			{ //Simulation
				BlockIngredients toCheck = ingredients; //Temporary
				ColorUnit color = unit; //Temporary
				
				for (ItemStack stack : bags) {
					ItemStack used = stack.copy();
					
					if(toCheck != null)
						toCheck = ItemTileContainer.drainBlocks(used, toCheck, false);
					if(color != null)
						color = ItemTileContainer.drainColor(used, color, false);
					
					usedBags.add(used);
				}
				
				if(color != null)
					throw new NotEnoughIngredientsException.NotEnoughColorException(color);
				
				if(toCheck != null)
				{
					BlockIngredients additionalIngredients = toCheck.copy();
					for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
						BlockIngredient leftOver = toCheck.drainItemStack(player.inventory.getStackInSlot(i).copy());
						if(leftOver != null)
							additionalIngredients.addIngredient(leftOver);
						
						if(toCheck.isEmpty())
							break;
					}
					
					if(!toCheck.isEmpty())
						throw new NotEnoughIngredientsException.NotEnoughVolumeExcepion(toCheck);
					
					addIngredients(usedBags, additionalIngredients, null); //Check whether there is space for the additional ingredients (drain from ordinary itemstacks)
				}
			}
			
			//enough ingredients and enough space (if it needs to drain additional itemstacks)
			for (ItemStack stack : bags) {
				if(ingredients != null)
					ingredients = ItemTileContainer.drainBlocks(stack, ingredients, false);
				if(unit != null)
					unit = ItemTileContainer.drainColor(stack, unit, false);
			}
			
			if(ingredients != null)
			{
				BlockIngredients additionalIngredients = ingredients.copy();
				for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
					BlockIngredient leftOver = ingredients.drainItemStack(player.inventory.getStackInSlot(i));
					if(leftOver != null)
						additionalIngredients.addIngredient(leftOver);
					
					if(ingredients.isEmpty())
						break;
				}
				
				addIngredients(player, additionalIngredients, null);
			}
			
		}
		return true;
	}
	
	public static boolean addTileToInventory(EntityPlayer player, LittleTile tile) throws NotEnoughIngredientsException
	{
		List<LittleTilePreview> previews = new ArrayList<>();
		previews.add(tile.getPreviewTile());
		return addPreviewToInventory(player, previews);
	}
	
	public static boolean addTilesToInventory(EntityPlayer player, List<LittleTile> tiles) throws NotEnoughIngredientsException
	{
		if(needIngredients(player))
		{
			List<LittleTilePreview> previews = new ArrayList<>();
			for (LittleTile tile : tiles) {
				previews.add(tile.getPreviewTile());
			}
			
			return addPreviewToInventory(player, previews);
		}
		return true;
	}
	
	public static boolean addPreviewToInventory(EntityPlayer player, List<LittleTilePreview> previews) throws NotEnoughIngredientsException
	{
		if(needIngredients(player))
		{
			ColorUnit color = new ColorUnit();
			BlockIngredients ingredients = new BlockIngredients();
			for (LittleTilePreview preview : previews) {
				if(preview.canBeConvertedToBlockEntry())
				{
					ingredients.addIngredient(preview.getBlockIngredient());
					color.addColorUnit(ColorUnit.getRequiredColors(preview));
				}
			}
			return addIngredients(player, ingredients, color);
		}
		return true;
	}
	
	private static boolean store(List<ItemStack> bags, BlockIngredients toCheck, ColorUnit color, boolean simulate) throws NotEnoughIngredientsException
	{
		for (ItemStack stack : bags) {
			if(toCheck != null)
				toCheck = ItemTileContainer.storeBlocks(stack, toCheck, true, simulate);
			if(color != null)
				color = ItemTileContainer.storeColor(stack, color, simulate);
			
			if(toCheck == null && color == null)
				break;
		}
		
		if(color == null && toCheck != null)
		{
			for (ItemStack stack : bags) {
				toCheck = ItemTileContainer.storeBlocks(stack, toCheck, false, simulate);
				if(toCheck == null)
					break;
			}
		}
		
		if(color != null)
			throw new NotEnoughIngredientsException.NotEnoughColorSpaceException();
		
		if(toCheck != null)
			throw new NotEnoughIngredientsException.NotEnoughVolumeSpaceException();
		
		
		
		return true;
	}
	
	public static boolean addIngredients(EntityPlayer player, BlockIngredients ingredients, ColorUnit unit) throws NotEnoughIngredientsException
	{
		if(needIngredients(player))
		{
			List<ItemStack> bags = getBags(player);
			
			if(store(bags, ingredients, unit, true))
				store(bags, ingredients, unit, false);
		}
		
		return true;
	}
	
	public static boolean addIngredients(List<ItemStack> bags, BlockIngredients ingredients, ColorUnit unit) throws NotEnoughIngredientsException
	{
		if(store(bags, ingredients, unit, true))
			store(bags, ingredients, unit, false);
		
		return true;
	}
	
	public static List<ItemStack> getBags(EntityPlayer player)
	{
		List<ItemStack> bags = new ArrayList<>();
		for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
			ItemStack stack = player.inventory.getStackInSlot(i);
			if(stack.getItem() instanceof ItemTileContainer)
				bags.add(stack);
		}
		return bags;
	}
	
}
