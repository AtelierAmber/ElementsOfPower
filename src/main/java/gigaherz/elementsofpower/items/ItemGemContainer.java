package gigaherz.elementsofpower.items;

import gigaherz.common.state.IItemStateManager;
import gigaherz.common.state.implementation.ItemStateManager;
import gigaherz.elementsofpower.ElementsOfPower;
import gigaherz.elementsofpower.database.ContainerInformation;
import gigaherz.elementsofpower.database.MagicAmounts;
import gigaherz.elementsofpower.gemstones.Element;
import gigaherz.elementsofpower.gemstones.Gemstone;
import gigaherz.elementsofpower.gemstones.ItemGemstone;
import gigaherz.elementsofpower.gemstones.Quality;
import gigaherz.elementsofpower.network.SpellSequenceUpdate;
import gigaherz.elementsofpower.progression.DiscoveryHandler;
import gigaherz.elementsofpower.spells.SpellManager;
import gigaherz.elementsofpower.spells.Spellcast;
import gigaherz.elementsofpower.spells.SpellcastEntityData;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;

public abstract class ItemGemContainer extends ItemMagicContainer
{
    public static final PropertyInteger NORMAL = PropertyInteger.create("meta", 0, 1);


    @Override
    public IItemStateManager createStateManager()
    {
        return new ItemStateManager(this, NORMAL);
    }

    public ItemGemContainer(String name)
    {
        super(name);
    }

    @Override
    public boolean isInfinite(ItemStack stack)
    {
        return getGemstone(stack) == Gemstone.Creativite;
    }

    public ItemStack getStack(Gemstone gemstone, Quality quality)
    {
        return setQuality(getStack(1, gemstone), quality);
    }

    @Override
    public MagicAmounts getCapacity(ItemStack stack)
    {
        Gemstone g = getGemstone(stack);
        if (g == null)
            return MagicAmounts.EMPTY;

        Quality q = getQuality(stack);
        if (q == null)
            return MagicAmounts.EMPTY;

        MagicAmounts magic = ItemGemstone.capacities[q.ordinal()];

        Element e = g.getElement();
        if (e == null)
            magic = magic.all(magic.get(0) * 0.1f);
        else
            magic = magic.add(g.getElement(), magic.get(g.getElement()) * 0.25f);

        return magic;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack)
    {
        Quality q = getQuality(stack);
        if (q == null)
            return EnumRarity.COMMON;
        return q.getRarity();
    }

    @Nullable
    public Gemstone getGemstone(ItemStack stack)
    {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null)
            return null;

        if (!tag.hasKey("gemstone", Constants.NBT.TAG_INT))
            return null;

        int g = tag.getInteger("gemstone");
        if (g < 0 || g > Gemstone.values.length)
            return null;

        return Gemstone.values[g];
    }

    public ItemStack setGemstone(ItemStack stack, @Nullable Gemstone gemstone)
    {
        NBTTagCompound tag = stack.getTagCompound();
        if (gemstone == null)
        {
            if (tag != null)
            {
                tag.removeTag("gemstone");
            }
            return stack;
        }

        if (tag == null)
        {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }

        tag.setInteger("gemstone", gemstone.ordinal());

        return stack;
    }

    @Nullable
    public Quality getQuality(ItemStack stack)
    {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null)
            return null;

        if (!tag.hasKey("quality", Constants.NBT.TAG_INT))
            return null;

        int q = tag.getInteger("quality");
        if (q < 0 || q > Quality.values.length)
            return null;

        return Quality.values[q];
    }

    public ItemStack setQuality(ItemStack stack, @Nullable Quality q)
    {
        NBTTagCompound tag = stack.getTagCompound();

        if (q == null)
        {
            if (tag != null)
            {
                tag.removeTag("quality");
            }
            return stack;
        }

        if (tag == null)
        {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }

        tag.setInteger("quality", q.ordinal());

        return stack;
    }

    public ItemStack getContainedGemstone(ItemStack stack)
    {
        Gemstone gem = getGemstone(stack);
        Quality q = getQuality(stack);

        if (gem == null)
            return ItemStack.EMPTY;

        ItemStack t = ElementsOfPower.gemstone.getStack(gem);

        if (q != null)
        {
            t = ElementsOfPower.gemstone.setQuality(t, q);
        }

        MagicAmounts am = ContainerInformation.getContainedMagic(stack);

        if (!am.isEmpty())
        {
            am = adjustRemovedMagic(am);

            t = ContainerInformation.setContainedMagic(t, am);
        }

        return t;
    }

    public ItemStack setContainedGemstone(ItemStack stack, ItemStack gemstone)
    {
        if (gemstone.getCount() <= 0)
        {
            return ContainerInformation.setContainedMagic(setQuality(setGemstone(stack, null), null), MagicAmounts.EMPTY);
        }

        if (!(gemstone.getItem() instanceof ItemGemstone))
            return ItemStack.EMPTY;

        ItemGemstone g = ((ItemGemstone) gemstone.getItem());
        Gemstone gem = g.getGemstone(gemstone);
        Quality q = g.getQuality(gemstone);

        MagicAmounts am = ContainerInformation.getContainedMagic(gemstone);

        am = adjustInsertedMagic(am);

        return ContainerInformation.setContainedMagic(setQuality(setGemstone(stack, gem), q), am);
    }

    protected MagicAmounts adjustInsertedMagic(MagicAmounts am)
    {
        return am;
    }

    protected MagicAmounts adjustRemovedMagic(MagicAmounts am)
    {
        return am;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged || oldStack.getItem() != newStack.getItem() || oldStack.getMetadata() != newStack.getMetadata();
    }

    @Override
    public String getUnlocalizedName(ItemStack stack)
    {
        Gemstone g = getGemstone(stack);

        if (g == null)
            return getUnlocalizedName();

        return getUnlocalizedName() + g.getUnlocalizedName();
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        Quality q = getQuality(stack);

        @SuppressWarnings("deprecation")
        String namePart = net.minecraft.util.text.translation.I18n.translateToLocal(getUnlocalizedName(stack) + ".name");

        if (q == null)
            return namePart;

        @SuppressWarnings("deprecation")
        String quality = net.minecraft.util.text.translation.I18n.translateToLocal(ElementsOfPower.MODID + ".gemContainer.quality" + q.getUnlocalizedName());

        return quality + " " + namePart;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand)
    {
        ItemStack itemStackIn = playerIn.getHeldItem(hand);

        if (!worldIn.isRemote)
        {
            if (playerIn.isSneaking())
            {
                NBTTagCompound tag = itemStackIn.getTagCompound();
                if (tag != null)
                    tag.removeTag(ItemWand.SPELL_SEQUENCE_TAG);
            }
        }

        // itemInUse handled by TickEventWandControl

        return ActionResult.newResult(EnumActionResult.SUCCESS, itemStackIn);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack par1ItemStack)
    {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack par1ItemStack)
    {
        return EnumAction.BOW;
    }

    public boolean onSpellCommit(ItemStack stack, EntityPlayer player, @Nullable String sequence)
    {
        boolean updateSequenceOnWand = true;

        if (sequence == null)
        {
            updateSequenceOnWand = false;
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null)
            {
                sequence = tag.getString(ItemWand.SPELL_SEQUENCE_TAG);
            }
        }

        if (sequence == null || sequence.length() == 0)
            return false;

        Spellcast cast = SpellManager.makeSpell(sequence);

        if (cast == null)
            return false;

        MagicAmounts amounts = ContainerInformation.getContainedMagic(stack);
        MagicAmounts cost = cast.getSpellCost();

        if (!ContainerInformation.isInfiniteContainer(stack) && !amounts.hasEnough(cost))
            return false;

        cast = cast.getShape().castSpell(stack, player, cast);
        if (cast != null)
        {
            SpellcastEntityData data = SpellcastEntityData.get(player);
            data.begin(cast);
        }

        if (!ContainerInformation.isInfiniteContainer(stack))
            amounts = amounts.subtract(cost);

        ContainerInformation.setContainedMagic(stack, amounts);

        DiscoveryHandler.instance.onSpellcast(player, cast);
        return updateSequenceOnWand;
    }

    public void processSequenceUpdate(SpellSequenceUpdate message, ItemStack stack, EntityPlayer player)
    {
        if (message.changeMode == SpellSequenceUpdate.ChangeMode.COMMIT)
        {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt == null)
            {
                if (!ContainerInformation.isInfiniteContainer(stack))
                    return;

                nbt = new NBTTagCompound();
                stack.setTagCompound(nbt);
            }

            if (onSpellCommit(stack, player, message.sequence))
            {
                nbt.setString(ItemWand.SPELL_SEQUENCE_TAG, message.sequence);
            }
        }
    }
}
