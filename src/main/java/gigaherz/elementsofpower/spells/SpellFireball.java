package gigaherz.elementsofpower.spells;

import gigaherz.elementsofpower.entities.EntityFlameball;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class SpellFireball extends SpellBase {

    int power;

    public SpellFireball(int power) {
        this.power = power;
    }

    @Override
    public void castSpell(ItemStack stack, EntityPlayer player) {
        World world = player.worldObj;

        world.spawnEntityInWorld(new EntityFlameball(world, power, player));
    }
}
