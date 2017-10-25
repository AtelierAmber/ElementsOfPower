package gigaherz.elementsofpower.client;

import gigaherz.common.client.ModelHandle;
import gigaherz.common.state.client.ItemStateMapper;
import gigaherz.elementsofpower.ElementsOfPower;
import gigaherz.elementsofpower.client.renderers.*;
import gigaherz.elementsofpower.common.IModProxy;
import gigaherz.elementsofpower.common.Used;
import gigaherz.elementsofpower.entities.EntityBall;
import gigaherz.elementsofpower.entities.EntityEssence;
import gigaherz.elementsofpower.essentializer.TileEssentializer;
import gigaherz.elementsofpower.essentializer.gui.ContainerEssentializer;
import gigaherz.elementsofpower.gemstones.Element;
import gigaherz.elementsofpower.gemstones.Gemstone;
import gigaherz.elementsofpower.gemstones.GemstoneBlockType;
import gigaherz.elementsofpower.items.ItemGemContainer;
import gigaherz.elementsofpower.network.AddVelocityPlayer;
import gigaherz.elementsofpower.network.EssentializerAmountsUpdate;
import gigaherz.elementsofpower.network.EssentializerTileUpdate;
import gigaherz.elementsofpower.network.SpellcastSync;
import gigaherz.elementsofpower.spells.SpellcastEntityData;
import gigaherz.guidebook.client.BookRegistryEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.security.InvalidParameterException;

import static gigaherz.common.client.ModelHelpers.registerBlockModelAsItem;
import static gigaherz.common.client.ModelHelpers.registerItemModel;

@Used
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy implements IModProxy
{
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        registerBlockModelAsItem(ElementsOfPower.essentializer);
        registerBlockModelAsItem(ElementsOfPower.cocoon, 0, "color=8,facing=north");

        registerItemModel(ElementsOfPower.analyzer);

        registerItemModel(ElementsOfPower.orb.getStack(Element.Fire), "element=fire");
        registerItemModel(ElementsOfPower.orb.getStack(Element.Water), "element=water");
        registerItemModel(ElementsOfPower.orb.getStack(Element.Air), "element=air");
        registerItemModel(ElementsOfPower.orb.getStack(Element.Earth), "element=earth");
        registerItemModel(ElementsOfPower.orb.getStack(Element.Light), "element=light");
        registerItemModel(ElementsOfPower.orb.getStack(Element.Darkness), "element=dark");
        registerItemModel(ElementsOfPower.orb.getStack(Element.Life), "element=life");
        registerItemModel(ElementsOfPower.orb.getStack(Element.Death), "element=death");

        new ItemStateMapper(ElementsOfPower.gemstone).registerAllModelsExplicitly();
        new ItemStateMapper(ElementsOfPower.spelldust).registerAllModelsExplicitly();

        for (GemstoneBlockType b : GemstoneBlockType.values)
        {
            registerBlockModelAsItem(ElementsOfPower.gemstoneBlock, b.ordinal(), "type=" + b.getName());
            registerBlockModelAsItem(ElementsOfPower.gemstoneOre, b.ordinal(), "type=" + b.getName());
        }

        registerGemMeshDefinition(ElementsOfPower.wand);
        registerGemMeshDefinition(ElementsOfPower.staff);
        registerGemMeshDefinition(ElementsOfPower.ring);
        registerGemMeshDefinition(ElementsOfPower.headband);
        registerGemMeshDefinition(ElementsOfPower.necklace);
    }

    @Optional.Method(modid = "gbook")
    @SubscribeEvent
    public static void registerBook(BookRegistryEvent event)
    {
        event.register(ElementsOfPower.location("xml/guidebook.xml"));
    }

    @SubscribeEvent
    public static void onTextureStitchEvent(TextureStitchEvent.Pre event)
    {
        event.getMap().registerSprite(ElementsOfPower.location("blocks/cone"));
    }

    public void preInit()
    {
        OBJLoader.INSTANCE.addDomain(ElementsOfPower.MODID);

        ModelHandle.init();

        registerClientEvents();
        registerEntityRenderers();
    }

    public void init()
    {
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler(
                (stack, tintIndex) ->
                {
                    if (tintIndex != 0)
                        return 0xFFFFFFFF;

                    int index = stack.getItemDamage();

                    if (index >= Gemstone.values.size())

                        return 0xFFFFFFFF;

                    return Gemstone.values.get(index).getTintColor();
                }, ElementsOfPower.spelldust);
    }

    private void registerClientEvents()
    {
        MinecraftForge.EVENT_BUS.register(new MagicContainerOverlay());
        MinecraftForge.EVENT_BUS.register(new MagicTooltips());
        MinecraftForge.EVENT_BUS.register(new SpellRenderOverlay());
        MinecraftForge.EVENT_BUS.register(new TickEventWandControl());
    }

    @Override
    public void handleSpellcastSync(SpellcastSync message)
    {
        Minecraft.getMinecraft().addScheduledTask(() ->
        {
            World world = Minecraft.getMinecraft().world;
            EntityPlayer player = (EntityPlayer) world.getEntityByID(message.casterID);
            SpellcastEntityData data = SpellcastEntityData.get(player);

            data.sync(message.changeMode, message.spellcast);
        });
    }

    @Override
    public void handleRemainingAmountsUpdate(EssentializerAmountsUpdate message)
    {
        Minecraft.getMinecraft().addScheduledTask(() ->
        {
            EntityPlayer player = Minecraft.getMinecraft().player;

            if (message.windowId != -1)
            {
                if (message.windowId == player.openContainer.windowId)
                {
                    if ((player.openContainer instanceof ContainerEssentializer))
                    {
                        ((ContainerEssentializer) player.openContainer).updateAmounts(message.contained, message.remaining);
                    }
                }
            }
        });
    }

    @Override
    public void handleEssentializerTileUpdate(EssentializerTileUpdate message)
    {
        Minecraft.getMinecraft().addScheduledTask(() ->
        {
            TileEntity te = Minecraft.getMinecraft().world.getTileEntity(message.pos);
            if (te instanceof TileEssentializer)
            {
                TileEssentializer essentializer = (TileEssentializer) te;
                essentializer.getInventory().setStackInSlot(0, message.activeItem);
                essentializer.remainingToConvert = message.remaining;
            }
        });
    }

    @Override
    public void handleAddVelocity(AddVelocityPlayer message)
    {
        Minecraft.getMinecraft().addScheduledTask(() ->
                Minecraft.getMinecraft().player.addVelocity(message.vx, message.vy, message.vz));
    }

    @Override
    public void beginTracking(EntityPlayer playerIn, EnumHand hand)
    {
        TickEventWandControl.instance.handInUse = hand;
        playerIn.setActiveHand(hand);
    }

    private static void registerGemMeshDefinition(Item item)
    {
        ModelLoader.setCustomMeshDefinition(item, new GemContainerMeshDefinition(item));
    }

    // ----------------------------------------------------------- Entity Renderers
    public void registerEntityRenderers()
    {
        ClientRegistry.bindTileEntitySpecialRenderer(TileEssentializer.class, new RenderEssentializer());

        RenderingRegistry.registerEntityRenderingHandler(EntityBall.class, RenderBall::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityEssence.class, RenderEssence::new);
    }

    private static class GemContainerMeshDefinition implements ItemMeshDefinition
    {
        final Item item;

        private GemContainerMeshDefinition(Item item)
        {
            this.item = item;

            ResourceLocation[] resLocs = new ResourceLocation[Gemstone.values.size() + 1];
            for (int i = 0; i < Gemstone.values.size(); i++)
            {
                Gemstone g = Gemstone.values.get(i);
                resLocs[i] = getModelResourceLocation(g);
            }
            resLocs[Gemstone.values.size()] = getModelResourceLocation(null);
            ModelBakery.registerItemVariants(item, resLocs);
        }

        @Override
        public ModelResourceLocation getModelLocation(ItemStack stack)
        {
            Item item = stack.getItem();
            if (!(item instanceof ItemGemContainer))
                throw new InvalidParameterException("stack is not a gem container");

            ItemGemContainer c = (ItemGemContainer) item;

            Gemstone g = c.getGemstone(stack);

            return getModelResourceLocation(g);
        }

        private ModelResourceLocation getModelResourceLocation(@Nullable Gemstone g)
        {
            String variantName = "gem=" + (g != null ? g.getName() : "unbound");

            return new ModelResourceLocation(item.getRegistryName(), variantName);
        }
    }
}
