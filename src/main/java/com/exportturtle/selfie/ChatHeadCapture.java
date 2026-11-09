package com.exportturtle.selfie;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.ColorTextureOverride;
import net.runelite.api.ModelData;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;

/** Assembles dedicated chat-head assets on the client thread, never body geometry. */
final class ChatHeadCapture
{
    private static final int IDENTITY_KIT_ARCHIVE = 3;
    private static final int ITEM_ARCHIVE = 10;

    // Classic player-design hair/skin palettes, shared by body and chat-head assets.
    private static final short[] HAIR = {6798, 107, 10283, 16, 4797, 7744, 5799, 4634, -31839, 22433, 2983,
        -11343, 8, 5281, 10438, 3650, -27322, -21845, 200, 571, 908, 21830, 28946, -15701, -14010};
    private static final short[] HAIR_SECONDARY = {6554, 115, 10304, 28, 5702, 7756, 5681, 4510, -31835,
        22437, 2859, -11339, 16, 5157, 10446, 3658, -27314, -21965, 472, 580, 784, 21966, 28950, -15697,
        -14002};
    private static final short[] SKIN = {
        4550, 4537, 5681, 5673, 5790, 6806, 8076, 4574, 17050, 0, 127, -31821, -17991};

    private static void recolor(ModelData model, int source, short[] palette, int choice)
    {
        if (choice >= 0 && choice < palette.length)
            model.recolor((short)source, palette[choice]);
    }

    static PortraitScene capture(Client client)
    {
        PlayerComposition appearance = client.getLocalPlayer().getPlayerComposition();
        if (appearance == null || appearance.getTransformedNpcId() != -1)
            throw new IllegalStateException("A normal player appearance is needed for the headshot.");
        List<ModelData> parts = new ArrayList<>();
        for (KitType slot : new KitType[] {KitType.HEAD, KitType.HAIR, KitType.JAW})
        {
            int item = appearance.getEquipmentId(slot), kit = appearance.getKitId(slot);
            ChatHeadDefinition definition;
            if (item >= 0)
                definition = ChatHeadDefinition.item(
                    client.getIndexConfig().loadData(ITEM_ARCHIVE, item), appearance.getGender() != 0);
            else if (kit >= 0)
                definition =
                    ChatHeadDefinition.kit(client.getIndexConfig().loadData(IDENTITY_KIT_ARCHIVE, kit));
            else
                continue;
            for (int id : definition.models)
            {
                if (id < 0 || id == 65535)
                    continue;
                ModelData loaded = client.loadModelData(id);
                if (loaded == null)
                    throw new IllegalStateException("Head model is still loading. Refresh again shortly.");
                ModelData model = loaded.shallowCopy();
                if (model.getFaceColors() != null)
                    model = model.cloneColors();
                if (model.getFaceTextures() != null)
                    model = model.cloneTextures();
                ColorTextureOverride override = item < 0 ? null : appearance.getColorTextureOverride(slot);
                short[] colors = override == null ? null : override.getColorToReplaceWith();
                short[] textures = override == null ? null : override.getTextureToReplaceWith();
                for (int i = 0; i < definition.recolors.size(); i++)
                {
                    short[] pair = definition.recolors.get(i);
                    model.recolor(pair[0],
                        colors != null && colors.length == definition.recolors.size() ? colors[i] : pair[1]);
                }
                for (int i = 0; i < definition.retextures.size(); i++)
                {
                    short[] pair = definition.retextures.get(i);
                    model.retexture(pair[0],
                        textures != null && textures.length == definition.retextures.size() ? textures[i]
                                                                                            : pair[1]);
                }
                parts.add(model);
            }
        }
        if (parts.isEmpty())
            throw new IllegalStateException("No dedicated head models are available.");
        ModelData head = client.mergeModels(parts.toArray(new ModelData[0]));
        int[] choices = appearance.getColors();
        if (choices != null && choices.length >= 5)
        {
            recolor(head, 6798, HAIR, choices[0]);
            recolor(head, -10304, HAIR_SECONDARY, choices[0]);
            recolor(head, 4550, SKIN, choices[4]);
        }
        return PortraitScene.chatHead(
            client.getLocalPlayer().getName(), head.light(), client.getTextureProvider());
    }
}
