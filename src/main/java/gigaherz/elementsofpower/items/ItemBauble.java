package gigaherz.elementsofpower.items;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import gigaherz.elementsofpower.database.ContainerInformation;
import gigaherz.elementsofpower.database.MagicAmounts;
import gigaherz.elementsofpower.gemstones.Gemstone;
import gigaherz.elementsofpower.gemstones.Quality;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;

@Optional.Interface(modid = "baubles", iface = "baubles.api.IBauble")
public abstract class ItemBauble extends ItemGemContainer implements IBauble
{
    private static final float MAX_TRANSFER_TICK = 1 / 20.0f;

    protected ItemBauble(String name)
    {
        super(name);
    }

    @Nullable
    private static Triple<ItemStack, IInventory, Integer>
    findInInventory(ItemStack thisStack, @Nullable IInventory b, MagicAmounts available)
    {
        if (b == null)
            return null;

        for (int i = 0; i < b.getSizeInventory(); i++)
        {
            ItemStack s = b.getStackInSlot(i);
            if (s == thisStack)
                continue;
            if (ContainerInformation.canItemContainMagic(s))
            {
                if (ContainerInformation.canTransferAnything(s, available))
                {
                    return Triple.of(s, b, i);
                }
            }
        }

        return null;
    }

    protected MagicAmounts adjustInsertedMagic(MagicAmounts am)
    {
        return am.multiply(1.5f);
    }

    protected MagicAmounts adjustRemovedMagic(MagicAmounts am)
    {
        return am.multiply(1 / 1.5f);
    }

    @Optional.Method(modid = "baubles")
    @Override
    public abstract BaubleType getBaubleType(ItemStack itemstack);

    @Override
    public boolean willAutoSync(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player)
    {
        if (player.world.isRemote)
            return;

        if (player instanceof EntityPlayer)
            tryTransferToWands(itemstack, (EntityPlayer) player);
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player)
    {

    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player)
    {

    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player)
    {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player)
    {
        return true;
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected)
    {
        super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);

        if (worldIn.isRemote)
            return;

        if (entityIn instanceof EntityPlayer)
            tryTransferToWands(stack, (EntityPlayer) entityIn);
    }

    protected void tryTransferToWands(ItemStack thisStack, EntityPlayer p)
    {
        MagicAmounts available = ContainerInformation.getContainedMagic(thisStack);

        if (available.isEmpty())
            return;

        Triple<ItemStack, IInventory, Integer> triple;

        triple = findInInventory(thisStack, p.inventory, available);

        if (triple == null)
        {
            triple = findInInventory(thisStack, BaublesApi.getBaubles(p), available);
        }

        if (triple == null)
            return;

        doTransfer(thisStack, available, triple);
    }

    private void doTransfer(ItemStack thisStack, MagicAmounts available,
                            Triple<ItemStack, IInventory, Integer> triple)
    {
        ItemStack stack = triple.getLeft();
        MagicAmounts limits = ContainerInformation.getMagicLimits(stack);
        MagicAmounts amounts = ContainerInformation.getContainedMagic(stack);

        if (limits.isEmpty())
            return;

        float totalTransfer = getTotalTransfer(thisStack, available, stack, limits, amounts);

        if (totalTransfer > 0)
        {
            ItemStack stack2 = ContainerInformation.setContainedMagic(stack, amounts);
            if (stack2 != stack)
                triple.getMiddle().setInventorySlotContents(triple.getRight(), stack2);

            if (!isInfinite(thisStack))
                ContainerInformation.setContainedMagic(thisStack, available);
        }
    }

    private float getTotalTransfer(ItemStack thisStack,
                                   MagicAmounts available, ItemStack stack,
                                   MagicAmounts limits, MagicAmounts amounts)
    {
        Gemstone g = getGemstone(thisStack);
        Quality q = getQuality(thisStack);

        if (g == null || q == null)
            return 0;

        float boost = q.getTransferSpeed();

        float totalTransfer = 0;
        for (int i = 0; i < MagicAmounts.ELEMENTS; i++)
        {
            float maxTransfer = ItemBauble.MAX_TRANSFER_TICK;

            if (g == Gemstone.Diamond || g.ordinal() == i)
                maxTransfer *= boost;

            float transfer = Math.min(maxTransfer, limits.get(i) - amounts.get(i));
            if (!isInfinite(stack))
                transfer = Math.min(available.get(i), transfer);
            if (transfer > 0)
            {
                totalTransfer += transfer;
                amounts = amounts.add(i, transfer);
                if (!isInfinite(stack))
                    available = available.add(i, -transfer);
            }
        }

        return totalTransfer;
    }
}
