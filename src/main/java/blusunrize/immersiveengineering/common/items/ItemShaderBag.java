package blusunrize.immersiveengineering.common.items;

import java.util.List;

import blusunrize.immersiveengineering.api.shader.ShaderRegistry;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.util.IEAchievements;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.omg.PortableInterceptor.SUCCESSFUL;

public class ItemShaderBag extends ItemIEBase
{
	public ItemShaderBag()
	{
		super("shaderBag", 64);
	}

//	@Override
//	@SideOnly(Side.CLIENT)
//	public int getColorFromItemStack(ItemStack stack, int pass)
//	{
//		EnumRarity rarity = this.getRarity(stack);
//		return ClientUtils.getFormattingColour(rarity.rarityColor);
//	}
	@Override
	public boolean hasCustomItemColours()
{
	return true;
}
	@Override
	public int getColourForIEItem(ItemStack stack, int pass)
	{
		return ClientUtils.getFormattingColour(this.getRarity(stack).rarityColor);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list)
	{
		for(int i=ShaderRegistry.sortedRarityMap.size()-1; i>=0; i--)
		{
			EnumRarity rarity = ShaderRegistry.sortedRarityMap.get(i);
			ItemStack s = new ItemStack(item);
			ItemNBTHelper.setString(s, "rarity", rarity.toString());
			list.add(s);
		}
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack)
	{
		return getRarity(stack).rarityName +" "+ super.getItemStackDisplayName(stack);
	}

	@Override
	public EnumRarity getRarity(ItemStack stack)
	{
		String r = ItemNBTHelper.getString(stack, "rarity");
		for(EnumRarity rarity : EnumRarity.values())
			if(rarity.toString().equalsIgnoreCase(r))
				return rarity;
		return EnumRarity.COMMON;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)
	{
		if(!world.isRemote)
			if(ShaderRegistry.totalWeight.containsKey(stack.getRarity()))
			{
				String shader = ShaderRegistry.getRandomShader(player.getName(), player.getRNG(), stack.getRarity(), true);
				if(shader==null || shader.isEmpty())
					return new ActionResult(EnumActionResult.FAIL, stack);
				ItemStack shaderItem = new ItemStack(IEContent.itemShader);
				ItemNBTHelper.setString(shaderItem, "shader_name", shader);
				if(ShaderRegistry.sortedRarityMap.indexOf(ShaderRegistry.shaderRegistry.get(shader).getRarity())<=ShaderRegistry.sortedRarityMap.indexOf(EnumRarity.EPIC) && ShaderRegistry.sortedRarityMap.indexOf(stack.getRarity())>=ShaderRegistry.sortedRarityMap.indexOf(EnumRarity.COMMON))
					player.addStat(IEAchievements.secret_luckOfTheDraw);
				stack.stackSize--;
				if(stack.stackSize<=0)
					return new ActionResult(EnumActionResult.SUCCESS, shaderItem);
				if(!player.inventory.addItemStackToInventory(shaderItem))
					player.dropItem(shaderItem, false, true);
			}
		return new ActionResult(EnumActionResult.PASS, stack);
	}
}