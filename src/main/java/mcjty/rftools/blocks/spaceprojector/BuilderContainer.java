package mcjty.rftools.blocks.spaceprojector;

import mcjty.container.ContainerFactory;
import mcjty.container.GenericContainer;
import mcjty.container.SlotDefinition;
import mcjty.container.SlotType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class BuilderContainer extends GenericContainer {
    public static final String CONTAINER_INVENTORY = "container";

    public static final int SLOT_TAB = 0;

    public static final ContainerFactory factory = new ContainerFactory() {
        @Override
        protected void setup() {
            addSlotBox(new SlotDefinition(SlotType.SLOT_SPECIFICITEM, new ItemStack(SpaceProjectorSetup.spaceChamberCardItem)), CONTAINER_INVENTORY, SLOT_TAB, 28, 8, 1, 18, 1, 18);
            layoutPlayerInventorySlots(10, 70);
        }
    };

    public BuilderContainer(EntityPlayer player, BuilderTileEntity containerInventory) {
        super(factory);
        addInventory(CONTAINER_INVENTORY, containerInventory);
        addInventory(ContainerFactory.CONTAINER_PLAYER, player.inventory);
        generateSlots();
    }
}
