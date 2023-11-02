package net.gudenau.minecraft.cape;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record SkinData(@Nullable Identifier cape, @Nullable Identifier elytra, boolean ears) {
}
