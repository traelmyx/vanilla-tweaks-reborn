package io.github.strikerrocker.vt.content.blocks.pedestal;

import io.github.strikerrocker.vt.content.blocks.ForgeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class PedestalBlockEntity extends BlockEntity {
    long lastChangeTime;
    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide()) {
                lastChangeTime = level.getGameTime();
                BlockState state = level.getBlockState(worldPosition);
                level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }
    };

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ForgeBlocks.PEDESTAL_TYPE.get(), pos, state);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.saveAdditional(compoundTag, registries);
        compoundTag.put("inventory", inventory.serializeNBT(registries));
        compoundTag.putLong("lastChangeTime", lastChangeTime);
    }

    @Override
    protected void loadAdditional(CompoundTag nbtIn, HolderLookup.Provider registries) {
        super.loadAdditional(nbtIn, registries);
        inventory.deserializeNBT(registries, nbtIn.getCompound("inventory"));
        lastChangeTime = nbtIn.getLong("lastChangeTime");
    }
}
