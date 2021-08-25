/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2021 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.retrooper.packetevents.injector.modern;

import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.event.impl.PacketSendEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.bukkit.entity.Player;

import java.util.List;
@ChannelHandler.Sharable
public class PacketEncoderModern extends MessageToMessageEncoder<ByteBuf> {
    public volatile Player player;
    private boolean handledCompression;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {
        ByteBuf transformedBuf = ctx.alloc().buffer().writeBytes(byteBuf);
        try {
            boolean needsCompress = handleCompressionOrder(ctx, transformedBuf);

            int firstReaderIndex = transformedBuf.readerIndex();
            PacketSendEvent packetSendEvent = new PacketSendEvent(ctx.channel(), player, transformedBuf);
            int readerIndex = transformedBuf.readerIndex();
            PacketEvents.get().getEventManager().callEvent(packetSendEvent, () -> {
                transformedBuf.readerIndex(readerIndex);
            });
            transformedBuf.readerIndex(firstReaderIndex);

            if (needsCompress) {
                recompress(ctx, transformedBuf);
            }
            out.add(transformedBuf.retain());
        } finally {
            transformedBuf.release();
        }
    }

    private boolean handleCompressionOrder(ChannelHandlerContext ctx, ByteBuf buf) {
        if (handledCompression) return false;

        int encoderIndex = ctx.pipeline().names().indexOf("compress");
        if (encoderIndex == -1) return false;
        handledCompression = true;
        if (encoderIndex > ctx.pipeline().names().indexOf(PacketEvents.get().encoderName)) {
            // Need to decompress this packet due to bad order
            ByteBuf decompressed = PacketDecoderModern.callDecode((ByteToMessageDecoder) ctx.pipeline().get("decompress"), ctx, buf);
            return PacketDecoderModern.refactorHandlers(ctx, buf, decompressed);
        }
        return false;
    }

    private void recompress(ChannelHandlerContext ctx, ByteBuf buf) {
        ByteBuf compressed = ctx.alloc().buffer();
        try {
            PacketDecoderModern.callEncode((MessageToByteEncoder<?>) ctx.pipeline().get("compress"), ctx, buf, compressed);
            buf.clear().writeBytes(compressed);
        } finally {
            compressed.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }
}
