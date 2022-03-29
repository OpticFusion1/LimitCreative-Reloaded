package com.zenya.limitcreative;

import org.bukkit.block.Block;

public record Loc(int x, int y, int z) {

    public Loc(Block block) {
        this(block.getX(), block.getY(), block.getZ());
    }

}
