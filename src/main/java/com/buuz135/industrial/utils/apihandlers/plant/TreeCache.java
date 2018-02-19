package com.buuz135.industrial.utils.apihandlers.plant;

import com.buuz135.industrial.proxy.BlockRegistry;
import com.buuz135.industrial.utils.BlockUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;

import java.util.*;

public class TreeCache {

    private Queue<BlockPos> woodCache;
    private Queue<BlockPos> leavesCache;
    private World world;

    public TreeCache(World world, BlockPos current) {
        this.woodCache = new PriorityQueue<>(Comparator.comparingDouble(value -> ((BlockPos) value).distanceSq(((BlockPos) value).getX(), current.getY(), ((BlockPos) value).getZ())).reversed());
        this.leavesCache = new PriorityQueue<>(Comparator.comparingDouble(value -> ((BlockPos) value).distanceSq(current.getX(), ((BlockPos) value).getY(), current.getZ())).reversed());
        this.world = world;
        Stack<BlockPos> tree = new Stack<>();
        tree.push(current);
        while (!tree.isEmpty()) {
            BlockPos checking = tree.pop();
            if (BlockUtils.isLog(world, checking) || BlockUtils.isLeaves(world, checking)) {
                Iterable<BlockPos> area = BlockPos.getAllInBox(checking.offset(EnumFacing.DOWN).offset(EnumFacing.SOUTH).offset(EnumFacing.WEST), checking.offset(EnumFacing.UP).offset(EnumFacing.NORTH).offset(EnumFacing.EAST));
                for (BlockPos blockPos : area) {
                    if (world.isAirBlock(blockPos) || woodCache.contains(blockPos) || leavesCache.contains(blockPos) || blockPos.getDistance(current.getX(), current.getY(), current.getZ()) > BlockRegistry.cropRecolectorBlock.getMaxDistranceTreeBlocksScan())
                        continue;
                    if (BlockUtils.isLog(world, blockPos)) {
                        tree.push(blockPos);
                        woodCache.add(blockPos);
                    } else if (BlockUtils.isLeaves(world, blockPos)) {
                        tree.push(blockPos);
                        leavesCache.add(blockPos);
                    }
                }
            }
        }
    }


    public List<ItemStack> chop(Queue<BlockPos> cache, boolean shear) {
        BlockPos p = cache.peek();
        NonNullList<ItemStack> stacks = NonNullList.create();
        if (BlockUtils.isLeaves(world, p) || BlockUtils.isLog(world, p)) {
            IBlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof IShearable && shear) {
                stacks.addAll(((IShearable) s.getBlock()).onSheared(new ItemStack(Items.SHEARS), world, p, 0));
            } else {
                s.getBlock().getDrops(stacks, world, p, s, 0);
            }
            world.setBlockToAir(p);
        }
        cache.poll();
        return stacks;
    }

    public Queue<BlockPos> getWoodCache() {
        return woodCache;
    }

    public Queue<BlockPos> getLeavesCache() {
        return leavesCache;
    }
}
