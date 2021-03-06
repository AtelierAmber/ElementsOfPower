package gigaherz.elementsofpower.essentializer;

import com.google.common.collect.Lists;
import gigaherz.elementsofpower.essentializer.gui.EssentializerContainer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class EssentializerBlock extends Block
{

    public EssentializerBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult p_225533_6_)
    {
        TileEntity tileEntity = worldIn.getTileEntity(pos);

        if (!(tileEntity instanceof EssentializerTileEntity))
            return ActionResultType.FAIL;

        if (player.isSneaking())
            return ActionResultType.PASS;

        if (worldIn.isRemote)
            return ActionResultType.SUCCESS;

        NetworkHooks.openGui((ServerPlayerEntity) player, new SimpleNamedContainerProvider((id, playerInventory, playerEntity) ->
                new EssentializerContainer(id, (EssentializerTileEntity) tileEntity, playerInventory),
                new TranslationTextComponent("container.elementsofpower.essentializer")), pos);

        return ActionResultType.SUCCESS;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
    {
        return VoxelShapes.or(
                Block.makeCuboidShape(0, 0, 0, 16, 7, 16),

                Block.makeCuboidShape(0, 7, 0, 4, 12, 4),
                Block.makeCuboidShape(12, 7, 0, 16, 12, 4),
                Block.makeCuboidShape(0, 7, 12, 4, 12, 16),
                Block.makeCuboidShape(12, 7, 12, 16, 12, 16),

                Block.makeCuboidShape(4, 12, 4, 12, 16, 12)
        );
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    {
        return new EssentializerTileEntity();
    }

    @Override
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand)
    {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof EssentializerTileEntity)
        {
            EssentializerTileEntity essentializer = (EssentializerTileEntity) te;
            if (!essentializer.remainingToConvert.isEmpty())
            {
                double x = (double) pos.getX() + 0.5;
                double y = (double) pos.getY() + (8.5 / 16.0);
                double z = (double) pos.getZ() + 0.5;
                double rx = rand.nextDouble() * 0.2D - 0.1D;
                double rz = rand.nextDouble() * 0.2D - 0.1D;

                int sides = rand.nextInt(16);
                if ((sides & 1) != 0)
                    worldIn.addParticle(ColoredSmokeData.withRandomColor(essentializer.remainingToConvert), x + rx + 0.4, y, z + rz, 0.0D, 0.05D, 0.0D);
                if ((sides & 2) != 0)
                    worldIn.addParticle(ColoredSmokeData.withRandomColor(essentializer.remainingToConvert), x + rx - 0.4, y, z + rz, 0.0D, 0.05D, 0.0D);
                if ((sides & 4) != 0)
                    worldIn.addParticle(ColoredSmokeData.withRandomColor(essentializer.remainingToConvert), x + rx, y, z + rz + 0.4, 0.0D, 0.05D, 0.0D);
                if ((sides & 8) != 0)
                    worldIn.addParticle(ColoredSmokeData.withRandomColor(essentializer.remainingToConvert), x + rx, y, z + rz - 0.4, 0.0D, 0.05D, 0.0D);
            }
        }
    }
}
