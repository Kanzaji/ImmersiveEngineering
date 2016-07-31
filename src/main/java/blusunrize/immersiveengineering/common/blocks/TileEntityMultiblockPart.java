package blusunrize.immersiveengineering.common.blocks;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IDirectionalTile;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityAssembler;
import com.sun.istack.internal.NotNull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankPropertiesWrapper;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

public abstract class TileEntityMultiblockPart<T extends TileEntityMultiblockPart<T>> extends TileEntityIEBase implements ITickable, IDirectionalTile, IBlockBounds
{
	public boolean formed = false;
	public int pos=-1;
	public int[] offset = {0,0,0};
	public boolean mirrored = false;
	public EnumFacing facing = EnumFacing.NORTH;

	@Override
	public EnumFacing getFacing()
	{
		return this.facing;
	}
	@Override
	public void setFacing(EnumFacing facing)
	{
		this.facing = facing;
	}
	@Override
	public int getFacingLimitation()
	{
		return 2;
	}
	@Override
	public boolean mirrorFacingOnPlacement(EntityLivingBase placer)
	{
		return false;
	}
	@Override
	public boolean canHammerRotate(EnumFacing side, float hitX, float hitY, float hitZ, EntityLivingBase entity)
	{
		return false;
	}


	//	=================================
	//		DATA MANAGEMENT
	//	=================================
	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		formed = nbt.getBoolean("formed");
		pos = nbt.getInteger("pos");
		offset = nbt.getIntArray("offset");
		mirrored = nbt.getBoolean("mirrored");
		facing = EnumFacing.getFront(nbt.getInteger("facing"));
	}
	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		nbt.setBoolean("formed", formed);
		nbt.setInteger("pos", pos);
		nbt.setIntArray("offset", offset);
		nbt.setBoolean("mirrored", mirrored);
		nbt.setInteger("facing", facing.ordinal());
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing)
	{
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && facing!=null&&this.getAccessibleFluidTanks(facing).length>0)
			return true;
		return super.hasCapability(capability, facing);
	}
	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
	{
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && facing!=null&&this.getAccessibleFluidTanks(facing).length>0)
			return (T)new MultiblockFluidWrapper(this, facing);
		return super.getCapability(capability, facing);
	}

	//	=================================
	//		FLUID MANAGEMENT
	//	=================================
	@NotNull
	protected abstract FluidTank[] getAccessibleFluidTanks(EnumFacing side);
	protected abstract boolean canFillTankFrom(int iTank, EnumFacing side, FluidStack resource);
	protected abstract boolean canDrainTankFrom(int iTank, EnumFacing side);

	public static class MultiblockFluidWrapper implements IFluidHandler
	{
		final TileEntityMultiblockPart multiblock;
		final EnumFacing side;

		public MultiblockFluidWrapper(TileEntityMultiblockPart multiblock, EnumFacing side)
		{
			this.multiblock = multiblock;
			this.side = side;
		}
		@Override
		public IFluidTankProperties[] getTankProperties()
		{
			if(!this.multiblock.formed)
				return new IFluidTankProperties[0];
			FluidTank[] tanks = this.multiblock.getAccessibleFluidTanks(side);
			IFluidTankProperties[] array = new IFluidTankProperties[tanks.length];
			for(int i=0; i<tanks.length; i++)
				array[i] = new FluidTankPropertiesWrapper(tanks[i]);
			return array;
		}
		@Override
		public int fill(FluidStack resource, boolean doFill)
		{
			if(!this.multiblock.formed || resource==null)
				return 0;
			FluidTank[] tanks = this.multiblock.getAccessibleFluidTanks(side);
			int fill = -1;
			for(int i=0; i<tanks.length; i++)
			{
				FluidTank tank = tanks[i];
				if(tank != null && this.multiblock.canFillTankFrom(i, side, resource) && tank.getFluid()!= null && tank.getFluid().isFluidEqual(resource))
				{
					fill = tank.fill(resource, doFill);
					if(fill>0)
						break;
				}
			}
			if(fill==-1)
				for(int i=0; i<tanks.length; i++)
				{
					FluidTank tank = tanks[i];
					if(tank != null && this.multiblock.canFillTankFrom(i, side, resource))
					{
						fill = tank.fill(resource, doFill);
						if(fill>0)
							break;
					}
				}
			if(fill>0)
				this.multiblock.updateMasterBlock(null, true);
			return fill<0?0:fill;
		}
		@Nullable
		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain)
		{
			if(!this.multiblock.formed || resource==null)
				return null;
			FluidTank[] tanks = this.multiblock.getAccessibleFluidTanks(side);
			FluidStack drain = null;
			for(int i=0; i<tanks.length; i++)
			{
				FluidTank tank = tanks[i];
				if(tank != null && this.multiblock.canDrainTankFrom(i, side))
				{
					drain = tank.drain(resource, doDrain);
					if(drain!=null)
						break;
				}
			}
			if(drain!=null)
				this.multiblock.updateMasterBlock(null, true);
			return drain;
		}
		@Nullable
		@Override
		public FluidStack drain(int maxDrain, boolean doDrain)
		{
			if(!this.multiblock.formed || maxDrain==0)
				return null;
			FluidTank[] tanks = this.multiblock.getAccessibleFluidTanks(side);
			FluidStack drain = null;
			for(int i=0; i<tanks.length; i++)
			{
				FluidTank tank = tanks[i];
				if(tank!=null && this.multiblock.canDrainTankFrom(i, side))
				{
					drain = tank.drain(maxDrain, doDrain);
					if(drain!=null)
						break;
				}
			}
			if(drain!=null)
				this.multiblock.updateMasterBlock(null, true);
			return drain;
		}
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
	}

	public static boolean _Immovable()
	{
		return true;
	}
	public T master()
	{
		if(offset[0]==0&&offset[1]==0&&offset[2]==0)
			return (T)this;
		TileEntity te = worldObj.getTileEntity(getPos().add(-offset[0],-offset[1],-offset[2]));
		return this.getClass().isInstance(te)?(T)te: null;
	}
	public void updateMasterBlock(IBlockState state, boolean blockUpdate)
	{
		T master = master();
		if(master!=null)
		{
			master.markDirty();
			if(blockUpdate)
				master.markContainingBlockForUpdate(state);
		}
	}
	public boolean isDummy()
	{
		return offset[0]!=0 || offset[1]!=0 || offset[2]!=0;
	}
	public abstract ItemStack getOriginalBlock();
	public abstract void disassemble();
}