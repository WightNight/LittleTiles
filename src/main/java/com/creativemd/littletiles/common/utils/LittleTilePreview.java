package com.creativemd.littletiles.common.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import com.creativemd.creativecore.client.rendering.RenderCubeObject;
import com.creativemd.creativecore.common.utils.Rotation;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.items.ItemTileContainer.BlockEntry;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.nbt.LittleNBTCompressionTools;
import com.creativemd.littletiles.common.utils.place.FixedHandler;
import com.creativemd.littletiles.common.utils.place.PlacePreviewTile;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;
import com.creativemd.littletiles.common.utils.small.LittleTileVec;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LittleTilePreview {
	
	//================Type ID================
	
	private static HashMap<String, Class<? extends LittleTilePreview>> previewTypes = new HashMap<>();
	
	public static void registerPreviewType(String id, Class<? extends LittleTilePreview> type)
	{
		previewTypes.put(id, type);
	}
	
	public String getTypeID()
	{
		if(!isCustomPreview())
			return "";
		for (Entry<String, Class<? extends LittleTilePreview>> type : previewTypes.entrySet())
			if(type.getValue() == this.getClass())
				return type.getKey();
		return "";
	}
	
	public boolean isCustomPreview()
	{
		return this.getClass() != LittleTilePreview.class;
	}
	
	//================Data================
	
	public boolean canSplit = true;
	public LittleTileSize size = null;
	
	protected NBTTagCompound tileData;
	
	public LittleTileBox box;
	
	public ArrayList<FixedHandler> fixedhandlers = new ArrayList<FixedHandler>();
	
	//================Constructors================
	
	/**This constructor needs to be implemented in every subclass**/
	public LittleTilePreview(NBTTagCompound nbt) {
		if(nbt.hasKey("bBoxminX") || nbt.hasKey("bBox"))
		{
			box = new LittleTileBox("bBox", nbt);
			size = box.getSize();
		}else if(nbt.hasKey("sizex") || nbt.hasKey("size"))
			size = new LittleTileSize("size", nbt);
		else
			new LittleTileSize(0,0,0);
		
		if(nbt.hasKey("tile"))
			tileData = nbt.getCompoundTag("tile");
		else{
			tileData = nbt.copy();
			tileData.removeTag("bBox");
			tileData.removeTag("size");
		}
	}
	
	public LittleTilePreview(LittleTileBox box, NBTTagCompound tileData)
	{
		this(box.getSize(), tileData);
		this.box = box;
	}
	
	public LittleTilePreview(LittleTileSize size, NBTTagCompound tileData)
	{
		this.size = size;
		this.tileData = tileData;
	}
	
	//================Preview================
	
	public boolean isOrdinaryTile()
	{
		String id = tileData.getString("tID");
		return id.equals("BlockTileBlock") || id.equals("BlockTileColored");
	}
	
	/**Rendering inventory**/
	public String getPreviewBlockName()
	{
		return tileData.getString("block");
	}
	
	/**Rendering inventory**/
	public Block getPreviewBlock()
	{
		if(tileData.hasKey("block"))
			return Block.getBlockFromName(getPreviewBlockName());
		return Blocks.AIR;
	}
	
	/**Rendering inventory**/
	public int getPreviewBlockMeta()
	{
		return tileData.getInteger("meta");
	}
	
	/**Rendering inventory**/
	public boolean hasColor()
	{
		return tileData.hasKey("color");
	}
	
	/**Rendering inventory**/
	public int getColor()
	{
		if(tileData.hasKey("color"))
			return tileData.getInteger("color");
		return -1;
	}
	
	/**Rendering inventory**/
	@SideOnly(Side.CLIENT)
	public RenderCubeObject getCubeBlock()
	{
		RenderCubeObject cube = new RenderCubeObject(box.getCube(), null);
		if(tileData.hasKey("block"))
		{
			cube.block = getPreviewBlock();
			cube.meta = getPreviewBlockMeta();
		}else{
			cube.block = Blocks.STONE;
		}
		if(tileData.hasKey("color"))
			cube.color = tileData.getInteger("color");
		return cube;
	}
	
	public boolean isInvisible()
	{
		return tileData.getBoolean("invisible");
	}
	
	public void setInvisibile(boolean invisible)
	{
		tileData.setBoolean("invisible", invisible);
	}
	
	public NBTTagCompound getTileData()
	{
		return tileData;
	}
	
	public BlockEntry getBlockEntry()
	{
		return new BlockEntry(getPreviewBlock(), getPreviewBlockMeta(), size.getPercentVolume());
	}
	
	//================Copy================
	
	public LittleTilePreview copy() {
		NBTTagCompound nbt = new NBTTagCompound();
		this.writeToNBT(nbt);
		LittleTilePreview preview = loadPreviewFromNBT(nbt);
		
		if(preview == null)
		{
			if(this.box != null)
				preview = new LittleTilePreview(box, tileData.copy());
			else
				preview = new LittleTilePreview(size != null ? size.copy() : null, tileData.copy());
		}
		preview.canSplit = this.canSplit;
		preview.fixedhandlers = new ArrayList<>(this.fixedhandlers);
		return preview;
	}
	
	//================Placing================
	
	/**Used for placing the tile**/
	public LittleTile getLittleTile(TileEntityLittleTiles te)
	{
		return LittleTile.CreateandLoadTile(te, te.getWorld(), tileData);
	}
	
	public PlacePreviewTile getPlaceableTile(LittleTileBox box, boolean fixed, LittleTileVec offset)
	{
		if(this.box == null)
			return new PlacePreviewTile(box.copy(), this);
		else{
			LittleTileBox newBox = this.box.copy();
			if(!fixed)
				newBox.addOffset(offset);
			return new PlacePreviewTile(newBox, this);
		}
	}
	
	//================Rotating/Flipping================
	
	public void flipPreview(EnumFacing direction)
	{
		if(box != null)
			box.flipBoxWithCenter(direction, null);
	}
	
	public void rotatePreview(Rotation direction)
	{
		size.rotateSize(direction);
		if(box != null)
		{
			box.rotateBoxWithCenter(direction, new Vec3d(1/32D, 1/32D, 1/32D));
			size = box.getSize();
		}
	}
	
	public void rotatePreview(EnumFacing direction)
	{
		size.rotateSize(direction);
		if(box != null)
			box.rotateBox(direction);
	}
	
	//================Save & Loading================
	
	public static LittleTilePreview loadPreviewFromNBT(NBTTagCompound nbt)
	{
		if(nbt == null)
			return null;
		if(nbt.hasKey("type"))
		{
			Class<? extends LittleTilePreview> type = previewTypes.get(nbt.getString("type"));
			if(type != null)
			{
				LittleTilePreview preview = null;
				try {
					preview = type.getConstructor(NBTTagCompound.class).newInstance(nbt);
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
				return preview;
			}
		}else
			return new LittleTilePreview(nbt);
		return null;
	}
	
	public void writeToNBT(NBTTagCompound nbt)
	{
		if(box != null)
			box.writeToNBT("bBox", nbt);
		else
			size.writeToNBT("size", nbt);
		nbt.setTag("tile", tileData);
		if(isCustomPreview() && !getTypeID().equals(""))
			nbt.setString("type", getTypeID());
	}
	
	//================Grouping================
	
	public List<NBTTagCompound> extractNBTFromGroup(NBTTagCompound nbt)
	{
		List<NBTTagCompound> tags = new ArrayList<>();
		NBTTagList list = nbt.getTagList("boxes", 11);
		
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound copy = nbt.copy();
			copy.removeTag("boxes");
			copy.setIntArray("bBox", list.getIntArrayAt(i));
			tags.add(copy);
		}
		return tags;
	}
	
	public boolean canBeNBTGrouped(LittleTilePreview preview)
	{
		return this.box != null && preview.box != null && preview.canSplit == preview.canSplit && preview.getTileData().equals(this.getTileData());
	}
	
	public NBTTagCompound startNBTGrouping()
	{
		NBTTagCompound nbt = new NBTTagCompound();
		writeToNBT(nbt);
		nbt.removeTag("bBox");
		NBTTagList list = new NBTTagList();
		list.appendTag(box.getNBTIntArray());
		nbt.setTag("boxes", list);
		return nbt;
	}
	
	public void groupNBTTile(NBTTagCompound nbt, LittleTilePreview preview)
	{
		NBTTagList list = nbt.getTagList("boxes", 11);
		list.appendTag(preview.box.getNBTIntArray());
	}
	
	//================Static Helpers================
	
	public static int lowResolutionMode = 2000;
	
	public static List<LittleTilePreview> getPreview(ItemStack stack)
	{
		return getPreview(stack, false);
	}
	
	public static List<LittleTilePreview> getPreview(ItemStack stack, boolean allowLowResolution)
	{
		if(stack.getTagCompound().getTag("tiles") instanceof NBTTagInt)
		{
			ArrayList<LittleTilePreview> result = new ArrayList<LittleTilePreview>();
			int tiles = stack.getTagCompound().getInteger("tiles");
			for (int i = 0; i < tiles; i++) {
				NBTTagCompound nbt = stack.getTagCompound().getCompoundTag("tile" + i);
				LittleTilePreview preview = LittleTilePreview.loadPreviewFromNBT(nbt);
				if(preview != null)
					result.add(preview);
			}
			return result;
		}else{
			if(allowLowResolution && stack.getTagCompound().hasKey("pos"))
			{
				ArrayList<LittleTilePreview> result = new ArrayList<LittleTilePreview>();
				NBTTagCompound tileData = new NBTTagCompound();
				LittleTile tile = new LittleTileBlock(LittleTiles.coloredBlock);
				tile.saveTileExtra(tileData);
				tileData.setString("tID", tile.getID());	
				
				NBTTagList list = stack.getTagCompound().getTagList("pos", 11);
				for (int i = 0; i < list.tagCount(); i++) {
					int[] array = list.getIntArrayAt(i);
					BlockPos pos = new BlockPos(array[0], array[1], array[2]);
					LittleTileVec max = new LittleTileVec(pos);
					max.addVec(new LittleTileVec(LittleTile.gridSize, LittleTile.gridSize, LittleTile.gridSize));
					result.add(new LittleTilePreview(new LittleTileBox(new LittleTileVec(pos), max), tileData));
				}
				return result;
			}
			return LittleNBTCompressionTools.readPreviews(stack.getTagCompound().getTagList("tiles", 10));
		}
	}
	
	public static LittleTileSize getSize(ItemStack stack)
	{
		if(stack.hasTagCompound() && stack.getTagCompound().hasKey("size"))
			return new LittleTileSize("size", stack.getTagCompound());
		return new LittleTileSize(1, 1, 1);
	}
	
	public static void savePreviewTiles(List<LittleTilePreview> previews, ItemStack stack)
	{
		if(!stack.hasTagCompound())
			stack.setTagCompound(new NBTTagCompound());
		
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		
		for (int i = 0; i < previews.size(); i++) {
			LittleTilePreview preview = previews.get(i);
			minX = Math.min(minX, preview.box.minX);
			minY = Math.min(minY, preview.box.minY);
			minZ = Math.min(minZ, preview.box.minZ);
			maxX = Math.max(maxX, preview.box.maxX);
			maxY = Math.max(maxY, preview.box.maxY);
			maxZ = Math.max(maxZ, preview.box.maxZ);
			
		}
		
		new LittleTileSize(maxX-minX, maxY-minY, maxZ-minZ).writeToNBT("size", stack.getTagCompound());
		
		if(previews.size() >= lowResolutionMode)
		{
			NBTTagList list = new NBTTagList();
			//BlockPos lastPos = null;
			
			HashSet<BlockPos> positions = new HashSet<>();
			
			for (int i = 0; i < previews.size(); i++) { //Will not be sorted after rotating
				BlockPos pos = previews.get(i).box.getMinVec().getBlockPos();
				if(!positions.contains(pos))
				{
					positions.add(pos);
					list.appendTag(new NBTTagIntArray(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
				}
			}
			stack.getTagCompound().setTag("pos", list);
		}else
			stack.getTagCompound().removeTag("pos");
		
		NBTTagList list = LittleNBTCompressionTools.writePreviews(previews);
		stack.getTagCompound().setTag("tiles", list);
		
		stack.getTagCompound().setInteger("count", previews.size());
		
		/*stack.getTagCompound().setInteger("tiles", previews.size());
		for (int i = 0; i < previews.size(); i++) {
			NBTTagCompound nbt = new NBTTagCompound();
			previews.get(i).writeToNBT(nbt);			
			stack.getTagCompound().setTag("tile" + i, nbt);
		}*/
	}
	
	public static void saveTiles(World world, List<LittleTile> tiles, ItemStack stack)
	{
		stack.setTagCompound(new NBTTagCompound());
		
		List<LittleTilePreview> previews = new ArrayList<>();
		for (int i = 0; i < tiles.size(); i++) {
			LittleTilePreview preview = tiles.get(i).getPreviewTile();
			previews.add(preview);
		}
		
		savePreviewTiles(previews, stack);
	}
	
	/*@SideOnly(Side.CLIENT)
	public static void updateSize(ArrayList<RenderCubeObject> cubes, ItemStack stack)
	{
		if(stack.hasTagCompound())
		{
			Vec3d size = CubeObject.getSizeOfCubes(cubes);
			stack.getTagCompound().setDouble("size", Math.max(1, Math.max(size.xCoord, Math.max(size.yCoord, size.zCoord))));
		}
	}*/
	
	@SideOnly(Side.CLIENT)
	public static ArrayList<RenderCubeObject> getCubes(ItemStack stack)
	{
		ArrayList<RenderCubeObject> cubes = new ArrayList<RenderCubeObject>();
		if(stack.hasTagCompound() && stack.getTagCompound().getInteger("count") >= lowResolutionMode)
		{
			NBTTagList list = stack.getTagCompound().getTagList("pos", 11);
			for (int i = 0; i < list.tagCount(); i++) {
				int[] array = list.getIntArrayAt(i);
				cubes.add(new RenderCubeObject(array[0], array[1], array[2], array[0]+1, array[1]+1, array[2]+1, LittleTiles.coloredBlock));
			}
		}else{
			List<LittleTilePreview> previews = getPreview(stack);
			for (int i = 0; i < previews.size(); i++) {
				cubes.add(previews.get(i).getCubeBlock());
			}
		}
		return cubes;
	}
}
