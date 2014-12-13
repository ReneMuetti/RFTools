package com.mcjty.rftools.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PacketRegisterDimensions implements IMessage {
    int id;

    @Override
    public void fromBytes(ByteBuf buf) {
        id = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(id);
    }

    public PacketRegisterDimensions() {
    }

    public PacketRegisterDimensions(int id) {
        this.id = id;
    }
}