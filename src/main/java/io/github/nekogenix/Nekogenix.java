package io.github.nekogenix;

import com.mojang.logging.LogUtils;
import io.github.nekogenix.network.InjectionInput;
import io.github.nekogenix.config.ClientConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(Nekogenix.ID)
public final class Nekogenix {
    public static final String ID = "nekogenix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Nekogenix(IEventBus bus, ModContainer container) {
        Content.register(bus);
        VoiceSounds.register(bus);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        bus.addListener(Nekogenix::network);
    }

    private static void network(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(InjectionInput.TYPE, InjectionInput.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        io.github.nekogenix.gameplay.InjectionHandler.input(player, payload.held());
                    }
                });
    }
}
