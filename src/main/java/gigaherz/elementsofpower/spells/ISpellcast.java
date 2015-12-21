package gigaherz.elementsofpower.spells;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public interface ISpellcast<T extends ISpellEffect>
{
    float getRemainingCastTime();

    void init(World world, EntityPlayer player);

    T getEffect();

    void update();

    void readFromNBT(NBTTagCompound tagData);
    void writeToNBT(NBTTagCompound tagData);
}
