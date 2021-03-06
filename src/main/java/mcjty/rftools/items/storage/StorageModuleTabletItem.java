package mcjty.rftools.items.storage;

import cofh.api.energy.IEnergyContainerItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcjty.rftools.RFTools;
import mcjty.rftools.blocks.storage.ModularStorageConfiguration;
import mcjty.rftools.blocks.storage.ModularStorageSetup;
import mcjty.varia.Logging;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import java.util.List;

public class StorageModuleTabletItem extends Item implements IEnergyContainerItem {

    private int capacity;
    private int maxReceive;
    private int maxExtract;

    private IIcon iconFull;
    private IIcon iconEmpty;

    public static final int DAMAGE_EMPTY = 0;
    public static final int DAMAGE_FULL = 1;

    public StorageModuleTabletItem() {
        setMaxStackSize(1);

        capacity = ModularStorageConfiguration.TABLET_MAXENERGY;
        maxReceive = ModularStorageConfiguration.TABLET_RECEIVEPERTICK;
        maxExtract = ModularStorageConfiguration.TABLET_CONSUMEPERUSE;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 1;
    }

    @Override
    public void registerIcons(IIconRegister iconRegister) {
        iconFull = iconRegister.registerIcon(RFTools.MODID + ":storage/storageModuleTablet");
        iconEmpty = iconRegister.registerIcon(RFTools.MODID + ":storage/storageModuleTabletEmpty");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        if (damage == DAMAGE_EMPTY) {
            return iconEmpty;
        } else {
            return iconFull;
        }
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            NBTTagCompound tagCompound = stack.getTagCompound();
            if (tagCompound == null || !tagCompound.hasKey("childDamage")) {
                Logging.message(player, EnumChatFormatting.YELLOW + "This tablet contains no storage module!");
                return stack;
            }

            int moduleDamage = tagCompound.getInteger("childDamage");
            int rfNeeded = ModularStorageConfiguration.TABLET_CONSUMEPERUSE;
            if (moduleDamage != StorageModuleItem.STORAGE_REMOTE) {
                rfNeeded += ModularStorageConfiguration.TABLET_EXTRACONSUME * (moduleDamage + 1);
            }

            int energy = tagCompound.getInteger("Energy");
            if (energy < rfNeeded) {
                Logging.message(player, EnumChatFormatting.YELLOW + "Not enough energy to open the contents!");
                return stack;
            }

            energy -= rfNeeded;
            tagCompound.setInteger("Energy", energy);

            if (moduleDamage == StorageModuleItem.STORAGE_REMOTE) {
                if (!tagCompound.hasKey("id")) {
                    Logging.message(player, EnumChatFormatting.YELLOW + "This remote storage module is not linked!");
                    return stack;
                }
                player.openGui(RFTools.instance, RFTools.GUI_REMOTE_STORAGE_ITEM, player.worldObj, (int) player.posX, (int) player.posY, (int) player.posZ);
            } else {
                player.openGui(RFTools.instance, RFTools.GUI_MODULAR_STORAGE_ITEM, player.worldObj, (int) player.posX, (int) player.posY, (int) player.posZ);
            }
            return stack;
        }
        return stack;
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        if (stack.getItemDamage() == DAMAGE_FULL) {
            return true;
        }
        return false;
    }

    @Override
    public Item getContainerItem() {
        return ModularStorageSetup.storageModuleTabletItem;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        if (hasContainerItem(stack) && stack.hasTagCompound()) {
            NBTTagCompound tagCompound = new NBTTagCompound();
            tagCompound.setInteger("Energy", stack.getTagCompound().getInteger("Energy"));
            ItemStack container = new ItemStack(getContainerItem());
            container.setTagCompound(tagCompound);
            return container;
        }
        return null;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer player, List list, boolean whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);
        NBTTagCompound tagCompound = itemStack.getTagCompound();
        if (tagCompound != null) {
            list.add(EnumChatFormatting.BLUE + "Energy: " + tagCompound.getInteger("Energy") + " RF");
            if (itemStack.getItemDamage() == DAMAGE_FULL) {
                int max = StorageModuleItem.MAXSIZE[tagCompound.getInteger("childDamage")];
                StorageModuleItem.addModuleInformation(list, max, tagCompound);
            } else {
                list.add(EnumChatFormatting.YELLOW + "No storage module installed!");
            }
        }
        list.add("This RF/charged module can hold a storage module");
        list.add("and allows the wielder to manipulate the contents of");
        list.add("this module (remote or normal).");
    }

    @Override
    public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate) {
        if (container.stackTagCompound == null) {
            container.stackTagCompound = new NBTTagCompound();
        }
        int energy = container.stackTagCompound.getInteger("Energy");
        int energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));

        if (!simulate) {
            energy += energyReceived;
            container.stackTagCompound.setInteger("Energy", energy);
        }
        return energyReceived;
    }

    @Override
    public int extractEnergy(ItemStack container, int maxExtract, boolean simulate) {
        if (container.stackTagCompound == null || !container.stackTagCompound.hasKey("Energy")) {
            return 0;
        }
        int energy = container.stackTagCompound.getInteger("Energy");
        int energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));

        if (!simulate) {
            energy -= energyExtracted;
            container.stackTagCompound.setInteger("Energy", energy);
        }
        return energyExtracted;
    }

    @Override
    public int getEnergyStored(ItemStack container) {
        if (container.stackTagCompound == null || !container.stackTagCompound.hasKey("Energy")) {
            return 0;
        }
        return container.stackTagCompound.getInteger("Energy");
    }

    @Override
    public int getMaxEnergyStored(ItemStack container) {
        return capacity;
    }
}
