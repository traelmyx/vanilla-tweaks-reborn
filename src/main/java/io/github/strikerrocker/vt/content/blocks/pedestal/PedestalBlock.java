package io.github.strikerrocker.vt.content.blocks.pedestal;

import io.github.strikerrocker.vt.content.BasePedestalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ItemInteractionResult;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

public class PedestalBlock extends BasePedestalBlock implements SimpleWaterloggedBlock, EntityBlock {

    public PedestalBlock() {
        super();
    }

    private static PedestalBlockEntity getPedestalBE(LevelReader world, BlockPos pos) {
        return (PedestalBlockEntity) world.getBlockEntity(pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide()) {
            PedestalBlockEntity tile = getPedestalBE(worldIn, pos);
            if (!player.isCrouching()) {
                IItemHandler itemHandler = tile.inventory;
                if (heldItem.isEmpty()) {
                    ItemStack stack = itemHandler.extractItem(0, 64, false);
                    player.setItemInHand(handIn, stack);
                } else {
                    player.setItemInHand(handIn, itemHandler.insertItem(0, heldItem, false));
                }
                tile.setChanged();
            } else {
                player.openMenu(new SimpleMenuProvider((id, playerInv, playerIn) -> new PedestalContainer(id, playerInv, pos), Component.translatable("block.vanillatweaks.pedestal")), data -> data.writeBlockPos(pos));
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!worldIn.isClientSide() && !state.is(newState.getBlock())) {
            if (worldIn.getBlockEntity(pos) instanceof PedestalBlockEntity tile) {
                ItemStack stack = tile.inventory.getStackInSlot(0);
                if (!stack.isEmpty()) {
                    ItemEntity item = new ItemEntity(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
                    worldIn.addFreshEntity(item);
                }
            }
        }
        super.onRemove(state, worldIn, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PedestalBlockEntity(pos, state);
    }
}