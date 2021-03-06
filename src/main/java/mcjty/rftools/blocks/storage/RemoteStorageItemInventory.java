package mcjty.rftools.blocks.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class RemoteStorageItemInventory implements IInventory {
    private ItemStack stacks[] = new ItemStack[RemoteStorageItemContainer.MAXSIZE_STORAGE];
    private final EntityPlayer entityPlayer;

    public RemoteStorageItemInventory(EntityPlayer player) {
        this.entityPlayer = player;
    }

    private RemoteStorageTileEntity getRemoteStorage() {
        return RemoteStorageIdRegistry.getRemoteStorage(entityPlayer.worldObj, getStorageID());
    }

    private int getStorageID() {
        return entityPlayer.getHeldItem().getTagCompound().getInteger("id");
    }

    private boolean isServer() {
        return !entityPlayer.worldObj.isRemote;
    }

    private ItemStack[] getStacks() {
        if (isServer()) {
            RemoteStorageTileEntity storage = getRemoteStorage();
            if (storage == null) {
                return new ItemStack[0];
            }
            int si = storage.findRemoteIndex(getStorageID());
            if (si == -1) {
                return new ItemStack[0];
            }
            return storage.getRemoteStacks(si);
        } else {
            int maxSize = entityPlayer.getHeldItem().getTagCompound().getInteger("maxSize");
            if (maxSize != stacks.length) {
                stacks = new ItemStack[maxSize];
            }
            return stacks;
        }
    }

    @Override
    public int getSizeInventory() {
        if (isServer()) {
            RemoteStorageTileEntity storage = getRemoteStorage();
            if (storage == null) {
                return 0;
            }
            int si = storage.findRemoteIndex(getStorageID());
            if (si == -1) {
                return 0;
            }
            int maxStacks = storage.getMaxStacks(si);
            entityPlayer.getHeldItem().getTagCompound().setInteger("maxSize", maxStacks);
            return maxStacks;
        } else {
            return entityPlayer.getHeldItem().getTagCompound().getInteger("maxSize");
        }
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (isServer()) {
            RemoteStorageTileEntity storage = getRemoteStorage();
            if (storage == null) {
                return null;
            }
            int si = storage.findRemoteIndex(getStorageID());
            if (si == -1) {
                return null;
            }
            return storage.getRemoteSlot(si, index);
        } else {
            return stacks[index];
        }
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        if (isServer()) {
            RemoteStorageTileEntity storage = getRemoteStorage();
            if (storage == null) {
                return null;
            }
            int si = storage.findRemoteIndex(getStorageID());
            if (si == -1) {
                return null;
            }
            return storage.decrStackSizeRemote(si, index, amount);
        } else {
            if (index >= stacks.length) {
                return null;
            }
            if (stacks[index] != null) {
                markDirty();
                if (stacks[index].stackSize <= amount) {
                    ItemStack old = stacks[index];
                    stacks[index] = null;
                    return old;
                }
                ItemStack its = stacks[index].splitStack(amount);
                if (stacks[index].stackSize == 0) {
                    stacks[index] = null;
                }
                return its;
            }
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (isServer()) {
            RemoteStorageTileEntity storage = getRemoteStorage();
            if (storage == null) {
                return;
            }
            int si = storage.findRemoteIndex(getStorageID());
            if (si == -1) {
                return;
            }
            storage.updateRemoteSlot(si, getInventoryStackLimit(), index, stack);
        } else {
            if (index >= stacks.length) {
                return;
            }
            stacks[index] = stack;
            if (stack != null && stack.stackSize > getInventoryStackLimit()) {
                stack.stackSize = getInventoryStackLimit();
            }
            markDirty();
        }
    }

    @Override
    public String getInventoryName() {
        return "remote storage";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        RemoteStorageTileEntity storage = getRemoteStorage();
        if (storage != null) {
            storage.markDirty();
        }
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {

    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        ItemStack[] s = getStacks();
        if (index >= s.length) {
            return false;
        }
        if (isServer()) {
            RemoteStorageTileEntity storage = getRemoteStorage();
            if (storage == null) {
                return false;
            }
            int si = storage.findRemoteIndex(getStorageID());
            if (si == -1) {
                return false;
            }
            if (index >= storage.getMaxStacks(si)) {
                return false;
            }
        }
        return true;
    }
}
