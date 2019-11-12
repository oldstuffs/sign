package io.github.portlek.sign;

import io.github.portlek.itemstack.util.Colored;
import io.github.portlek.itemstack.util.XMaterial;
import io.github.portlek.nbt.api.NBTCompound;
import io.github.portlek.nbt.base.EmptyNBTOf;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public final class Menu {

    private static final int SIGN_LINES = 4;

    private static final String NBT_FORMAT = "{\"text\":\"%s\"}";

    private static final String NBT_BLOCK_ID = "minecraft:sign";

    private static final int ACTION_INDEX = 9;

    private static final String OPEN_PACKET = "PacketPlayOutOpenSignEditor";

    private static final String ENTITY_DATA_PACKET = "PacketPlayOutTileEntityData";

    @NotNull
    private final Player player;

    @NotNull
    private final List<String> text;

    private BiPredicate<Player, List<String>> response;

    private boolean reopenIfFail;

    private Consumer<Location> onOpen;

    public Menu(@NotNull Player player, @NotNull List<String> text) {
        this.player = player;
        this.text = text;
    }

    void onOpen(@NotNull Consumer<Location> onOpen) {
        this.onOpen = onOpen;
    }

    @NotNull
    public Menu reopenIfFail() {
        this.reopenIfFail = true;
        return this;
    }

    @NotNull
    public Menu response(@NotNull BiPredicate<Player, List<String>> response) {
        this.response = response;
        return this;
    }

    @NotNull
    public BiPredicate<Player, List<String>> response() {
        return response;
    }

    public boolean isReopenIfFail() {
        return reopenIfFail;
    }

    public void open() {
        final Location location = player.getLocation().clone().subtract(0, 5, 0);

        player.sendBlockChange(location, XMaterial.ACACIA_WALL_SIGN.parseMaterial().createBlockData());

        final Object blockPosition = Sign.blockPosition.create(
            null,
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
        final Object openSignPacket = Sign.createPacket(OPEN_PACKET, blockPosition);

        if (openSignPacket == null) {
            return;
        }

        final NBTCompound signNBT = new EmptyNBTOf().nbt();

        IntStream.range(0, SIGN_LINES).forEach(line ->
            signNBT.setString("Text" + (line + 1), text.size() > line
                ? String.format(NBT_FORMAT, new Colored(text.get(line)).value())
                : "WW"
            )
        );
        signNBT.setInt("x", (Integer) Sign.blockPositionX.of(blockPosition).call(0));
        signNBT.setInt("y", (Integer) Sign.blockPositionY.of(blockPosition).call(0));
        signNBT.setInt("z", (Integer) Sign.blockPositionZ.of(blockPosition).call(0));
        signNBT.setString("id", NBT_BLOCK_ID);

        final Object signDataPacket = Sign.createPacket(
            ENTITY_DATA_PACKET,
            blockPosition,
            ACTION_INDEX,
            signNBT.value()
        );

        if (signDataPacket == null) {
            return;
        }

        Sign.sendPacket(player, signDataPacket);
        Sign.sendPacket(player, openSignPacket);
        onOpen.accept(location);
    }

}