package io.github.nekogenix.integration.badoptimizations;

import io.github.nekogenix.client.FelineVision;
import java.util.function.BooleanSupplier;

public final class FelineLightmapHook implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() { return FelineVision.changing(); }
}
