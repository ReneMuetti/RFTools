package com.mcjty.rftools.dimension.world;

import com.mcjty.rftools.blocks.ModBlocks;
import com.mcjty.rftools.blocks.dimlets.DimletConfiguration;
import com.mcjty.rftools.blocks.teleporter.MatterReceiverTileEntity;
import com.mcjty.rftools.blocks.teleporter.TeleportConfiguration;
import com.mcjty.rftools.blocks.teleporter.TeleportDestination;
import com.mcjty.rftools.blocks.teleporter.TeleportDestinations;
import com.mcjty.rftools.dimension.DimensionInformation;
import com.mcjty.rftools.dimension.RfToolsDimensionManager;
import com.mcjty.rftools.dimension.world.types.FeatureType;
import com.mcjty.rftools.items.ModItems;
import com.mcjty.rftools.items.dimlets.KnownDimletConfiguration;
import com.mcjty.varia.Coordinate;
import com.mcjty.varia.WeightedRandomSelector;
import cpw.mods.fml.common.IWorldGenerator;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;

import java.util.Random;

public class GenericWorldGenerator implements IWorldGenerator {

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        RfToolsDimensionManager manager = RfToolsDimensionManager.getDimensionManager(world);
        if (manager.getDimensionDescriptor(world.provider.dimensionId) == null) {
            return; // Not one of RFTools dimensions
        }

        DimensionInformation information = manager.getDimensionInformation(world.provider.dimensionId);
        if (information.hasFeatureType(FeatureType.FEATURE_OREGEN)) {
            for (Block block : information.getExtraOregen()) {
                addOreSpawn(block, information.getBaseBlockForTerrain(), world, random, chunkX * 16, chunkZ * 16, 7, 10, 12, 2, 60);
            }
        }

        addOreSpawn(ModBlocks.dimensionalShardBlock, Blocks.stone, world, random, chunkX * 16, chunkZ * 16, 5, 8, 3, 2, 40);

        if (chunkX == 0 && chunkZ == 0) {
            generateSpawnPlatform(world);
        } else if (Math.abs(chunkX) > 6 && Math.abs(chunkZ) > 6) {
            // Not too close to starting platform we possibly generate dungeons.
            if (random.nextInt(DimletConfiguration.dungeonChance) == 1) {
                int midx = chunkX * 16 + 8;
                int midz = chunkZ * 16 + 8;
                int starty1 = WorldGenerationTools.findSuitableEmptySpot(world, midx-3, midz-3);
                int starty2 = WorldGenerationTools.findSuitableEmptySpot(world, midx+3, midz-3);
                int starty3 = WorldGenerationTools.findSuitableEmptySpot(world, midx-3, midz+3);
                int starty4 = WorldGenerationTools.findSuitableEmptySpot(world, midx+3, midz+3);
                int starty = (starty1+starty2+starty3+starty4) / 4;
                if (starty > 1 && starty < world.getHeight()-20) {
                    generateDungeon(world, random, midx, starty, midz);
                }
            }
        }
    }

    private void generateSpawnPlatform(World world) {
        RfToolsDimensionManager dimensionManager = RfToolsDimensionManager.getDimensionManager(world);
        DimensionInformation information = dimensionManager.getDimensionInformation(world.provider.dimensionId);

        int midx = 8;
        int midz = 8;
        int starty = WorldGenerationTools.findSuitableEmptySpot(world, midx, midz);
        if (starty == -1) {
            // No suitable spot. We will carve something out.
            starty = 63;
        }

        for (int x = -3 ; x <= 3 ; x++) {
            for (int z = -3 ; z <= 3 ; z++) {
                if (x == 0 && z == 0) {
                    world.setBlock(x+midx, starty, z+midz, ModBlocks.matterReceiverBlock, 0, 2);
                    MatterReceiverTileEntity matterReceiverTileEntity = (MatterReceiverTileEntity) world.getTileEntity(x+midx, starty, z+midz);
                    matterReceiverTileEntity.modifyEnergyStored(TeleportConfiguration.RECEIVER_MAXENERGY);
                    matterReceiverTileEntity.setName(information.getName());
                    matterReceiverTileEntity.markDirty();
                } else {
                    world.setBlock(x+midx, starty, z+midz, Blocks.stone, 0, 2);
                }
                for (int y = 1 ; y <= 3 ; y++) {
                    world.setBlockToAir(x+midx, starty+y, z+midz);
                }
                // Check the top layer. If it is something other then air we will replace it with stone.
                if (!world.isAirBlock(x+midx, starty+4, z+midz)) {
                    world.setBlock(x+midx, starty+4, z+midz, Blocks.stone, 0, 2);
                }
            }
        }

        TeleportDestinations destinations = TeleportDestinations.getDestinations(world);
        Coordinate spawnPoint = new Coordinate(midx, starty, midz);
        TeleportDestination destination = destinations.addDestination(spawnPoint, world.provider.dimensionId);
        destination.setName(information.getName());
        destinations.save(world);

        information.setSpawnPoint(spawnPoint);
        dimensionManager.save(world);
    }

    private void generateDungeon(World world, Random random, int midx, int starty, int midz) {
        boolean do_small_antenna = random.nextInt(4) == 0;
        boolean do_extra_feature = random.nextInt(4) == 0;

        int color = random.nextInt(3);
        if (color == 0) {
            color = 3;
        } else if (color == 1) {
            color = 9;
        } else {
            color = 11;
        }

        // Spawn the building
        for (int x = midx - 3 ; x  <= midx + 3 ; x++) {
            for (int z = midz - 3 ; z  <= midz + 3 ;z ++) {
                boolean corner = (x == midx-3 || x == midx+3) && (z == midz-3 || z == midz+3);
                boolean xside = x == midx-3 || x == midx+3;
                boolean zside = z == midz-3 || z == midz+3;
                boolean antenna = (x == midx-2 && z == midz-2);
                boolean small_antenna = do_small_antenna && (x == midx+2 && z == midz+2);
                world.setBlock(x, starty, z, Blocks.double_stone_slab, 0, 2);
                if (corner) {
                    world.setBlock(x, starty + 1, z, Blocks.quartz_block, 0, 2);
                    world.setBlock(x, starty + 2, z, Blocks.quartz_block, 0, 2);
                    world.setBlock(x, starty + 3, z, Blocks.quartz_block, 0, 2);
                } else if (xside) {
                    world.setBlock(x, starty+1, z, Blocks.stained_hardened_clay, color, 2);
                    if (z >= midz-1 && z <= midz+1) {
                        world.setBlock(x, starty+2, z, Blocks.glass_pane, 0, 2);
                    } else {
                        world.setBlock(x, starty+2, z, Blocks.stained_hardened_clay, color, 2);
                    }
                    world.setBlock(x, starty+3, z, Blocks.stained_hardened_clay, color, 2);
                } else if (zside) {
                    world.setBlock(x, starty+1, z, Blocks.stained_hardened_clay, color, 2);
                    world.setBlock(x, starty+2, z, Blocks.stained_hardened_clay, color, 2);
                    world.setBlock(x, starty+3, z, Blocks.stained_hardened_clay, color, 2);
                } else {
                    world.setBlockToAir(x, starty+1, z);
                    world.setBlockToAir(x, starty+2, z);
                    world.setBlockToAir(x, starty+3, z);
                }
                if (antenna) {
                    world.setBlock(x, starty+4, z, Blocks.double_stone_slab, 0, 2);
                    world.setBlock(x, starty+5, z, Blocks.iron_bars, 0, 2);
                    world.setBlock(x, starty+6, z, Blocks.iron_bars, 0, 2);
                    world.setBlock(x, starty+7, z, Blocks.iron_bars, 0, 2);
                    world.setBlock(x, starty+8, z, Blocks.glowstone, 0, 2);
                } else if (small_antenna) {
                    world.setBlock(x, starty+4, z, Blocks.double_stone_slab, 0, 2);
                    world.setBlock(x, starty+5, z, Blocks.iron_bars, 0, 2);
                    world.setBlockToAir(x, starty+6, z);
                    world.setBlockToAir(x, starty+7, z);
                    world.setBlockToAir(x, starty+8, z);
                } else {
                    world.setBlock(x, starty+4, z, Blocks.stone_slab, 0, 2);
                    world.setBlockToAir(x, starty+5, z);
                    world.setBlockToAir(x, starty+6, z);
                    world.setBlockToAir(x, starty+7, z);
                    world.setBlockToAir(x, starty+8, z);
                }

                // Spawn stone under the building for as long as it is air.
                WorldGenerationTools.fillEmptyWithStone(world, x, starty-1, z);
            }
        }

        if (do_extra_feature) {
            if (!WorldGenerationTools.isSolid(world, midx+4, starty, midz-3)) {
                world.setBlock(midx+4, starty, midz-3, Blocks.iron_bars, 0, 2);
            }
            world.setBlock(midx+4, starty+1, midz-3, Blocks.iron_bars, 0, 2);
            world.setBlock(midx+4, starty+2, midz-3, Blocks.iron_bars, 0, 2);
            if (!WorldGenerationTools.isSolid(world, midx+5, starty, midz-3)) {
                world.setBlock(midx+5, starty, midz-3, Blocks.stained_hardened_clay, color, 2);
            }
            world.setBlock(midx+5, starty+1, midz-3, Blocks.stained_hardened_clay, color, 2);
            world.setBlock(midx+5, starty+2, midz-3, Blocks.stained_hardened_clay, color, 2);
            WorldGenerationTools.fillEmptyWithStone(world, midx + 4, starty - 1, midz - 3);
            WorldGenerationTools.fillEmptyWithStone(world, midx+5, starty-1, midz-3);
        }

        // Clear the space before the door.
        for (int x = midx-3 ; x <= midx+3 ; x++) {
            for (int y = starty+1 ; y <= starty + 3 ; y++) {
                world.setBlockToAir(x, y, midz-4);
            }
        }

        // Small platform before the door
        world.setBlock(midx-1, starty, midz-4, Blocks.double_stone_slab, 0, 2);
        world.setBlock(midx, starty, midz-4, Blocks.double_stone_slab, 0, 2);
        world.setBlock(midx+1, starty, midz-4, Blocks.double_stone_slab, 0, 2);

        world.setBlock(midx, starty+1, midz-3, Blocks.iron_door, 1, 2);
        world.setBlock(midx, starty+2, midz-3, Blocks.iron_door, 8, 2);
        world.setBlock(midx-1, starty+2, midz-4, Blocks.stone_button, 4, 2);
        world.setBlock(midx+1, starty+2, midz-2, Blocks.stone_button, 3, 2);

        world.setBlock(midx, starty+3, midz+3, Blocks.redstone_lamp, 0, 2);
        world.setBlock(midx, starty+3, midz+2, Blocks.lever, 4, 2);

        world.setBlock(midx+2, starty+1, midz-2, Blocks.chest, 0, 2);
        TileEntityChest chest = (TileEntityChest) world.getTileEntity(midx+2, starty+1, midz-2);
        for (int i = 0 ; i < random.nextInt(2)+2 ; i++) {
            chest.setInventorySlotContents(random.nextInt(chest.getSizeInventory()), new ItemStack(ModItems.unknownDimlet, random.nextInt(6) + 3));
        }
        WeightedRandomSelector.Distribution<Integer> goodDistribution = KnownDimletConfiguration.randomDimlets.createDistribution(0.01f);
        for (int i = 0 ; i < random.nextInt(2)+1 ; i++) {
            chest.setInventorySlotContents(random.nextInt(chest.getSizeInventory()), new ItemStack(ModItems.knownDimlet, 1, KnownDimletConfiguration.getRandomDimlet(goodDistribution, random)));
        }

        WeightedRandomSelector.Distribution<Integer> bestDistribution = KnownDimletConfiguration.randomDimlets.createDistribution(0.15f);
        EntityItemFrame frame1 = spawnItemFrame(world, midx - 1, starty + 2, midz + 2);
        frame1.setDisplayedItem(new ItemStack(ModItems.knownDimlet, 1, KnownDimletConfiguration.getRandomDimlet(bestDistribution, random)));
        EntityItemFrame frame2 = spawnItemFrame(world, midx, starty + 2, midz + 2);
        frame2.setDisplayedItem(new ItemStack(ModItems.knownDimlet, 1, KnownDimletConfiguration.getRandomDimlet(bestDistribution, random)));
        EntityItemFrame frame3 = spawnItemFrame(world, midx + 1, starty + 2, midz + 2);
        frame3.setDisplayedItem(new ItemStack(ModItems.knownDimlet, 1, KnownDimletConfiguration.getRandomDimlet(bestDistribution, random)));
    }

    private EntityItemFrame spawnItemFrame(World world, int x, int y, int z) {
        EntityItemFrame frame = new EntityItemFrame(world, x, y, z+1, 2);
        world.spawnEntityInWorld(frame);
        frame.setPosition(x, y, z);

        frame.field_146063_b = x;
        frame.field_146064_c = y;
        frame.field_146062_d = z + 1;
        frame.setDirection(frame.hangingDirection);
        return frame;
    }

    public void addOreSpawn(Block block, Block targetBlock, World world, Random random, int blockXPos, int blockZPos, int minVeinSize, int maxVeinSize, int chancesToSpawn, int minY, int maxY) {
        WorldGenMinable minable = new WorldGenMinable(block, (minVeinSize - random.nextInt(maxVeinSize - minVeinSize)), targetBlock);
        for (int i = 0 ; i < chancesToSpawn ; i++) {
            int posX = blockXPos + random.nextInt(16);
            int posY = minY + random.nextInt(maxY - minY);
            int posZ = blockZPos + random.nextInt(16);
            minable.generate(world, random, posX, posY, posZ);
        }
    }
}
