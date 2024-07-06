package org.embeddedt.embeddium.impl.mixin.features.model;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.data.MultipartModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(MultipartModelData.class)
public interface MultipartModelDataAccessor {
    @Accessor("PROPERTY")
    static ModelProperty<Map<BakedModel, ModelData>> getProperty() {
        throw new AssertionError();
    }
}
