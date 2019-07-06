package net.nerds.oysters.blocks;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.Direction;
import net.nerds.oysters.Utils.OysterBreedUtility;
import net.nerds.oysters.oysters.OysterBlockItem;
import net.nerds.oysters.oysters.OysterBreed;

import java.util.Arrays;
import java.util.Iterator;

public class OysterBasketEntity extends BlockEntity implements Tickable, SidedInventory {

    private int maxStorage = 47;
    public DefaultedList<ItemStack> inventory;
    private long tickCounter = 0;
    private long tickCheck = 1200;
    private long tickBreedCheck = 1200;
    private long tickBreedCounter = 0;
    private float canBreedChance = 4;

    public OysterBasketEntity(BlockEntityType blockEntityType) {
        super(blockEntityType);
        inventory = DefaultedList.create(maxStorage, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        if (tickCounter >= tickCheck) {
            farmOysters();
            tickCounter = 0;
        }
        if (tickBreedCounter >= tickBreedCheck) {
            attemptToBreedOysters();
            tickBreedCounter = 0;
        }
        tickCounter++;
        tickBreedCounter++;
    }

    private void farmOysters() {
        if (!world.isClient) {
            if (!inventory.get(0).isEmpty()) {
                OysterBreed oysterBreed = OysterBreedUtility.getBreedByBlockItem(inventory.get(0).getItem());
                ItemStack itemStack = new ItemStack(oysterBreed.getOysterPearl());
                addItemToInventory(itemStack);
            }
        }
    }

    private void addItemToInventory(ItemStack itemStack) {
        //loop through inventory looking for space. start at 2 to avoid breeding slots
        for (int i = 2; i < inventory.size(); i++) {
            if (inventory.get(i).isEmpty()) {
                inventory.set(i, itemStack);
                markDirty();
                break;
            } else if (inventory.get(i).isItemEqual(itemStack) &&
                    (inventory.get(i).getCount() + itemStack.getCount() <= itemStack.getMaxCount()) &&
                    itemStack.isStackable()) {
                inventory.set(i, new ItemStack(itemStack.getItem(), itemStack.getCount() + inventory.get(i).getCount()));
                markDirty();
                break;
            }
        }
    }

    private void attemptToBreedOysters() {
        if (!world.isClient) {
            //there are oysters and there is more than one
            if (!inventory.get(0).isEmpty() && inventory.get(0).getCount() > 1) {
                //pray to rng gods for blessings
                if (rngForBreeding(inventory.get(0).getCount())) {
                    //get the Oyster Breed from the spawning inventory
                    OysterBreed oysterBreed = OysterBreedUtility.getBreedByBlockItem(inventory.get(0).getItem());
                    //is this attempting to mutate the oyster
                    if (!inventory.get(1).isEmpty()) {
                        //there is a shell and a resource... get the new shell of that resource
                        OysterBreed newBreed = Arrays.stream(OysterBreed.values())
                                .filter(oyster -> oyster.getResourceItem() == inventory.get(1).getItem())
                                .findFirst().get();
                        //add new oyster to basket
                        addItemToInventory(new ItemStack(newBreed.getOysterBlockItem()));
                        //remove a resource item
                        if (!inventory.get(1).isEmpty()) {
                            inventory.get(1).decrement(1);
                        }
                    } else if (inventory.get(0).getCount() > 1) {
                        addItemToInventory(new ItemStack(oysterBreed.getOysterBlockItem()));
                    }
                } else {
                    //rng gods failed you, remove a resource item
                    if (!inventory.get(1).isEmpty()) {
                        inventory.get(1).decrement(1);
                    }
                }
            }
        }
    }

    private boolean rngForBreeding(int oysterCount) {
        float range = world.random.nextInt(100);
        //breed chance + additional oysters = ~ 4 - 10 % chance of breeding
        return (this.canBreedChance + (oysterCount - 2)) >= range;
    }

    @Override
    public void fromTag(CompoundTag nbt) {
        super.fromTag(nbt);
        inventory = DefaultedList.create(maxStorage, ItemStack.EMPTY);
        Inventories.fromTag(nbt, this.inventory);
    }

    @Override
    public CompoundTag toTag(CompoundTag nbt) {
        super.toTag(nbt);
        Inventories.toTag(nbt, this.inventory);
        return nbt;
    }

    @Override
    public int getInvSize() {
        return inventory.size();
    }

    @Override
    public boolean isInvEmpty() {
        Iterator var1 = this.inventory.iterator();
        ItemStack itemStack_1;
        do {
            if (!var1.hasNext()) {
                return true;
            }
            itemStack_1 = (ItemStack) var1.next();
        } while (itemStack_1.isEmpty());
        return false;
    }

    @Override
    public ItemStack getInvStack(int i) {
        return inventory.get(i);
    }

    @Override
    public ItemStack takeInvStack(int i, int i1) {
        return Inventories.splitStack(this.inventory, i, i1);
    }

    @Override
    public ItemStack removeInvStack(int i) {
        return Inventories.removeStack(inventory, i);
    }

    @Override
    public void setInvStack(int i, ItemStack itemStack) {
        inventory.set(i, itemStack);
        this.markDirty();
    }

    @Override
    public boolean canPlayerUseInv(PlayerEntity playerEntity) {
        if (this.world.getBlockEntity(this.pos) != this) {
            return false;
        } else {
            return playerEntity.squaredDistanceTo((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public int[] getInvAvailableSlots(Direction direction) {
        int[] arr = new int[maxStorage];
        for (int i = 0; i < maxStorage; i++) {
            arr[i] = i;
        }
        return arr;
    }

    @Override
    public boolean canInsertInvStack(int i, ItemStack itemStack, Direction direction) {
        return (i == 0 && itemStack.getItem() instanceof OysterBlockItem) ||
                (i == 1 && OysterBreedUtility.isAResource(itemStack.getItem()));
    }

    @Override
    public boolean canExtractInvStack(int i, ItemStack itemStack, Direction direction) {
        if (direction == Direction.DOWN && i > 0) {
            return true;
        }
        return false;
    }
}