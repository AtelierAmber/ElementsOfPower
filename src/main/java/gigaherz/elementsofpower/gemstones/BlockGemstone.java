package gigaherz.elementsofpower.gemstones;

import gigaherz.common.BlockRegistered;
import gigaherz.elementsofpower.ElementsOfPower;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockGemstone extends BlockRegistered
{
    public static final PropertyEnum<GemstoneBlockType> TYPE = PropertyEnum.create("type", GemstoneBlockType.class);

    public BlockGemstone(String name)
    {
        super(name, Material.IRON, MapColor.DIAMOND);
        setHardness(5.0F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, TYPE);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).ordinal();
    }

    @Deprecated
    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        if (meta > GemstoneBlockType.values.size())
            return getDefaultState();
        return getDefaultState().withProperty(TYPE, GemstoneBlockType.values.get(meta));
    }

    public ItemStack getStack(GemstoneBlockType gemstoneBlockType)
    {
        return getStack(1, gemstoneBlockType);
    }

    public ItemStack getStack(int quantity, GemstoneBlockType gemstoneBlockType)
    {
        return new ItemStack(this, quantity, getMetaFromState(getDefaultState().withProperty(TYPE, gemstoneBlockType)));
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (GemstoneBlockType type : GemstoneBlockType.values)
        {
            list.add(getStack(1, type));
        }
    }

    @Override
    public boolean isBeaconBase(IBlockAccess worldObj, BlockPos pos, BlockPos beacon)
    {
        return true;
    }

    @Override
    public ItemBlock createItemBlock()
    {
        return (ItemBlock) new ItemForm(this).setRegistryName(getRegistryName());
    }

    public static class ItemForm extends ItemBlock
    {
        public ItemForm(Block block)
        {
            super(block);
            setHasSubtypes(true);
        }

        @Override
        public int getMetadata(int damage)
        {
            return damage;
        }

        @Override
        public String getUnlocalizedName(ItemStack stack)
        {
            if (stack.getMetadata() > GemstoneBlockType.values.size())
                return block.getUnlocalizedName();
            return "tile." + ElementsOfPower.MODID + "." + GemstoneBlockType.values.get(stack.getMetadata()) + "Block";
        }
    }
}
