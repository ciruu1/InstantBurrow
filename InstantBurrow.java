package me.ciruu.abyss.feature.features.combat;

import me.ciruu.abyss.event.events.ClientTickEvent;
import me.ciruu.abyss.feature.ToggleableFeature;
import me.ciruu.abyss.setting.Setting;
import fuck.you.stop.skidding.bitch;
import me.ciruu.abyss.utils.Wrapper;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.block.BlockObsidian;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import static me.ciruu.abyss.utils.Wrapper.mc;

/**
 * @author Ciruu
 */

public class InstantBurrow extends ToggleableFeature {

    private final Setting<Boolean> rotate = new Setting<>("Rotate", "Rotate", this, false);
    private final Setting<Float> offset = new Setting<>("Offset", "Offset", this, 7.0F, -20.0F, 20.0F);

    private BlockPos originalPos;
    private int oldSlot = -1;

    public InstantBurrow() {
        super("InstantBurrow", "", FeatureCategory.COMBAT);
        addSetting(rotate);
        addSetting(offset);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        // Save our original pos
        originalPos = new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ);

        // If we can't place in our actual post then toggle and return
        if (mc.world.getBlockState(new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ)).getBlock().equals(Blocks.OBSIDIAN) ||
                intersectsWithEntity(this.originalPos)) {
            toggle();
            return;
        }

        // Save our item slot
        oldSlot = mc.player.inventory.currentItem;
    }

    @EventHandler
    private final Listener<ClientTickEvent> onTick = new Listener<>(event -> {
        // If we don't have obsidian in hotbar toggle and return
        if (BurrowUtil.findHotbarBlock(BlockObsidian.class) == -1) {
            Wrapper.sendMessage("Can't find obsidian in hotbar!");
            toggle();
            return;
        }

        // Change to obsidian slot
        BurrowUtil.switchToSlot(BurrowUtil.findHotbarBlock(BlockObsidian.class), false);

        // Fake jump
        mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.41999998688698D, mc.player.posZ, true));
        mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.7531999805211997D, mc.player.posZ, true));
        mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.00133597911214D, mc.player.posZ, true));
        mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.16610926093821D, mc.player.posZ, true));

        // Place block
        BurrowUtil.placeBlock(originalPos, EnumHand.MAIN_HAND, rotate.getValue(), true, false);

        // Rubberband
        mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY + offset.getValue(), mc.player.posZ, false));

        // SwitchBack
        BurrowUtil.switchToSlot(oldSlot, false);

        // AutoDisable
        toggle();
    });

    private boolean intersectsWithEntity(final BlockPos pos) {
        for (final Entity entity : mc.world.loadedEntityList) {
            if (entity.equals(mc.player)) continue;
            if (entity instanceof EntityItem) continue;
            if (new AxisAlignedBB(pos).intersects(entity.getEntityBoundingBox())) return true;
        }
        return false;
    }
}
