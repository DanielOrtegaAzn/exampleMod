package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class TunnelPickaxeItem extends Item {

    private static final int BEAM_RANGE = 10;
    private static final int BEAM_RADIUS = 1; // Radius of 1 = diameter of 3 (center + 1 each side)

    public TunnelPickaxeItem(ToolMaterial material, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();

        if (level.isClientSide() || player == null) {
            return InteractionResult.PASS;
        }

        // Get the direction the player is looking (including vertical)
        Direction facing = getLookDirection(player);

        // Mine the beam starting from the clicked block, in the direction the player is looking
        mineTunnel((ServerLevel) level, player, clickedPos, facing);

        // Damage the pickaxe
        ItemStack stack = context.getItemInHand();
        stack.hurtAndBreak(1, player, context.getHand());

        return InteractionResult.SUCCESS;
    }

    /**
     * Gets the closest cardinal direction (including UP/DOWN) to where the player is looking.
     */
    private static Direction getLookDirection(Player player) {
        // Get the player's look vector (normalized)
        var lookVec = player.getViewVector(1.0F);
        
        // Find the closest Direction (6-way: N, S, E, W, UP, DOWN)
        Direction closest = Direction.DOWN;
        double maxDot = Double.NEGATIVE_INFINITY;
        
        for (Direction dir : Direction.values()) {
            // Direction.getStepX/Y/Z returns the unit vector components (-1, 0, or 1)
            double dot = lookVec.x * dir.getStepX() + lookVec.y * dir.getStepY() + lookVec.z * dir.getStepZ();
            if (dot > maxDot) {
                maxDot = dot;
                closest = dir;
            }
        }
        
        return closest;
    }

    private void mineTunnel(ServerLevel level, Player player, BlockPos startPos, Direction facing) {
        // Get two perpendicular directions for the beam cross-section
        Direction axis1;
        Direction axis2;

        if (facing == Direction.UP || facing == Direction.DOWN) {
            // For vertical beams, use NORTH and EAST as cross-section axes
            axis1 = Direction.NORTH;
            axis2 = Direction.EAST;
        } else {
            // For horizontal beams, use UP and the clockwise horizontal direction
            axis1 = Direction.UP;
            axis2 = facing.getClockWise();
        }

        // Destroy blocks in a 3x3 beam (radius 1) along the facing direction
        for (int distance = 0; distance < BEAM_RANGE; distance++) {
            BlockPos centerPos = startPos.relative(facing, distance);
            
            // 3x3 cross-section: from -1 to +1 on each perpendicular axis
            for (int a1 = -BEAM_RADIUS; a1 <= BEAM_RADIUS; a1++) {
                for (int a2 = -BEAM_RADIUS; a2 <= BEAM_RADIUS; a2++) {
                    BlockPos targetPos = centerPos
                        .relative(axis1, a1)
                        .relative(axis2, a2);

                    // Mine the block (drop items, trigger events, etc.)
                    level.destroyBlock(targetPos, true, player);
                }
            }
        }
    }
}