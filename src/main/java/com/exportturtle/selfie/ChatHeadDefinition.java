/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.exportturtle.selfie;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads only chat-head fields from the local item/identity-kit cache formats.
 * Opcode layout follows RuneLite cache ItemLoader and KitLoader (BSD-2-Clause).
 */
final class ChatHeadDefinition
{
    final List<Integer> models = new ArrayList<>();
    final List<short[]> recolors = new ArrayList<>(), retextures = new ArrayList<>();

    static ChatHeadDefinition kit(byte[] bytes)
    {
        ByteBuffer input = buffer(bytes);
        ChatHeadDefinition result = new ChatHeadDefinition();
        for (int opcode; (opcode = u8(input)) != 0;)
        {
            if (opcode == 1)
                skip(input, 1);
            else if (opcode == 2 || opcode == 5)
                skip(input, u8(input) * (opcode == 2 ? 2 : 4));
            else if (opcode == 3)
            {
            }
            else if (opcode == 40 || opcode == 41)
                pairs(input, opcode == 40 ? result.recolors : result.retextures);
            else if (opcode >= 60 && opcode < 70)
                result.models.add(u16(input));
            else if (opcode >= 70 && opcode < 80)
                result.models.add(input.getInt());
            else
                throw new IllegalArgumentException("Unsupported identity-kit field " + opcode);
        }
        return result;
    }

    static ChatHeadDefinition item(byte[] bytes, boolean female)
    {
        ByteBuffer input = buffer(bytes);
        ChatHeadDefinition result = new ChatHeadDefinition();
        for (int opcode; (opcode = u8(input)) != 0;)
        {
            if (opcode >= 90 && opcode <= 93)
            {
                int model = u16(input);
                if ((opcode == 91 || opcode == 93) == female)
                    result.models.add(model);
            }
            else if (opcode >= 51 && opcode <= 54)
            {
                int model = input.getInt();
                if ((opcode >= 53) == female)
                    result.models.add(model);
            }
            else if (opcode == 40 || opcode == 41)
                pairs(input, opcode == 40 ? result.recolors : result.retextures);
            else if (opcode == 2 || opcode == 3 || opcode == 9 || opcode >= 30 && opcode <= 39)
                string(input);
            else if (opcode == 11 || opcode == 15 || opcode == 16 || opcode == 65 || opcode == 160 ||
                     opcode == 251)
            {
            }
            else if (opcode == 13 || opcode == 14 || opcode == 27 || opcode == 42 ||
                     opcode >= 113 && opcode <= 115)
                skip(input, 1);
            else if (opcode == 23 || opcode == 25)
                skip(input, 3);
            else if (opcode == 12 || opcode == 44 || opcode == 46 || opcode == 47 || opcode == 49 ||
                     opcode == 50)
                skip(input, 4);
            else if (opcode == 45 || opcode == 48)
                skip(input, 5);
            else if (opcode >= 100 && opcode < 110)
                skip(input, 4);
            else if (opcode == 1 || opcode >= 4 && opcode <= 8 || opcode == 24 || opcode == 26 ||
                     opcode == 75 || opcode == 78 || opcode == 79 || opcode >= 94 && opcode <= 99 ||
                     opcode >= 110 && opcode <= 112 || opcode == 139 || opcode == 140 || opcode == 148 ||
                     opcode == 149)
                skip(input, 2);
            else if (opcode == 43)
            {
                skip(input, 1);
                while (u8(input) != 0)
                    string(input);
            }
            else if (opcode == 161)
                skip(input, u16(input) * 2);
            else if (opcode == 249)
            {
                int count = u8(input);
                for (int i = 0; i < count; i++)
                {
                    boolean text = u8(input) == 1;
                    skip(input, 3);
                    if (text)
                        string(input);
                    else
                        skip(input, 4);
                }
            }
            else
                throw new IllegalArgumentException("Unsupported item field " + opcode);
        }
        return result;
    }

    private static ByteBuffer buffer(byte[] bytes)
    {
        if (bytes == null)
            throw new IllegalStateException("Appearance data is still loading. Refresh again shortly.");
        return ByteBuffer.wrap(bytes);
    }
    private static int u8(ByteBuffer input)
    {
        return input.get() & 255;
    }
    private static int u16(ByteBuffer input)
    {
        return input.getShort() & 65535;
    }
    private static void skip(ByteBuffer input, int count)
    {
        if (count < 0 || count > input.remaining())
            throw new IllegalArgumentException("Truncated appearance data");
        input.position(input.position() + count);
    }
    private static void string(ByteBuffer input)
    {
        while (u8(input) != 0)
        {
        }
    }
    private static void pairs(ByteBuffer input, List<short[]> target)
    {
        int count = u8(input);
        for (int i = 0; i < count; i++)
            target.add(new short[] {input.getShort(), input.getShort()});
    }
}
