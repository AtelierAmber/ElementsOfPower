package gigaherz.elementsofpower.entities;

import gigaherz.elementsofpower.spells.InitializedSpellcast;
import gigaherz.elementsofpower.spells.SpellManager;
import gigaherz.elementsofpower.spells.Spellcast;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.registries.ObjectHolder;

import javax.annotation.Nullable;
import java.util.Objects;

public class BallEntity extends ThrowableEntity implements IEntityAdditionalSpawnData
{
    @ObjectHolder("elementsofpower:ball")
    public static EntityType<BallEntity> TYPE;

    InitializedSpellcast spellcast;

    private static final DataParameter<CompoundNBT> SEQ = EntityDataManager.createKey(BallEntity.class, DataSerializers.COMPOUND_NBT);

    public BallEntity(World worldIn, LivingEntity thrower, InitializedSpellcast spellcast)
    {
        super(TYPE, thrower, worldIn);

        this.spellcast = spellcast;
        spellcast.setProjectile(this);

        CompoundNBT tag = new CompoundNBT();
        tag.putInt("caster", spellcast.player.getEntityId());
        tag.put("sequence", spellcast.getSequenceNBT());
        getDataManager().set(SEQ, tag);
    }

    public BallEntity(EntityType<BallEntity> type, World world)
    {
        super(type, world);
    }

    @Override
    protected void registerData()
    {
        CompoundNBT tag = new CompoundNBT();
        tag.put("sequence", new ListNBT());
        getDataManager().register(SEQ, tag);
    }

    @Override
    protected float getGravityVelocity()
    {
        return 0.001F;
    }

    @Override
    protected void onImpact(RayTraceResult pos)
    {
        if (!this.world.isRemote)
        {
            if (getSpellcast() != null)
                spellcast.onImpact(pos, rand);

            this.remove();
        }
    }

    public float getScale()
    {
        if (getSpellcast() != null)
            return 0.6f * (1 + spellcast.getDamageForce());
        return 0;
    }

    public int getColor()
    {
        if (getSpellcast() != null)
            return spellcast.getColor();
        return 0xFFFFFF;
    }

    @Nullable
    public InitializedSpellcast getSpellcast()
    {
        if (spellcast == null)
        {
            CompoundNBT tag = getDataManager().get(SEQ);
            if (tag.contains("sequence", Constants.NBT.TAG_LIST) && tag.contains("caster", Constants.NBT.TAG_INT))
            {
                PlayerEntity e = (PlayerEntity) this.world.getEntityByID(tag.getInt("caster"));
                if (e != null)
                {
                    ListNBT sequence = tag.getList("sequence", Constants.NBT.TAG_STRING);
                    Spellcast ccast = SpellManager.makeSpell(sequence);
                    if (ccast != null)
                    {
                        spellcast = ccast.init(world, e);
                    }
                }
            }
        }
        return spellcast;
    }

    @Override
    public IPacket<?> createSpawnPacket()
    {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer)
    {

    }

    @Override
    public void readSpawnData(PacketBuffer additionalData)
    {

    }
}
