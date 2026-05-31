package com.glisco.deathlog.storage;

import com.glisco.deathlog.client.DeathInfo;
import com.google.common.collect.ImmutableList;
import io.wispforest.endec.SerializationContext;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.owo.serialization.format.nbt.NbtDeserializer;
import io.wispforest.owo.serialization.format.nbt.NbtSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

// TODO format conversion?
public abstract class BaseDeathLogStorage implements DeathLogStorage {

    private static final int FORMAT_REVISION = 3;
    public static final Logger LOGGER = LogManager.getLogger();

    private boolean errored = false;

    private final RegistryAccess registries;

    protected BaseDeathLogStorage(RegistryAccess registries) {
        this.registries = registries;
    }

    protected CompletableFuture<List<DeathInfo>> load(RegistryAccess registries, File file) {
        final var future = new CompletableFuture<List<DeathInfo>>();
        Util.ioPool().service().submit(() -> {
            if (errored) {
                LOGGER.warn("Attempted to load DeathLog database even though disk operations are disabled");
                future.complete(null);
                return;
            }

            CompoundTag deathNbt;

            if (file.exists()) {
                try {
                    deathNbt = NbtIo.read(file.toPath());
                    if (deathNbt.contains("FormatRevision")) {
                        int formatRev = deathNbt.getInt("FormatRevision").orElse(-1);
                        if (formatRev != FORMAT_REVISION) {
                            raiseError("Incompatible format");

                            LOGGER.error("Incompatible DeathLog database format detected. Database not loaded and further disk operations disabled");

                            future.complete(null);
                            return;
                        }
                    }
                } catch (IOException e) {
                    raiseError("Disk access failed");

                    e.printStackTrace();
                    LOGGER.error("Failed to load DeathLog database, further disk operations have been disabled");

                    future.completeExceptionally(e);
                    return;
                }
            } else {
                deathNbt = new CompoundTag();
            }

            final var list = new ArrayList<DeathInfo>();
            final Optional<ListTag> infoList = deathNbt.getList("Deaths");
            try {
                for (int i = 0; i < infoList.get().size(); i++) {
                    Optional<CompoundTag> compoundOpt = infoList.get().getCompound(i);
                    if (compoundOpt.isPresent()) {
                        list.add(DeathInfo.ENDEC.decodeFully(
                                SerializationContext.attributes(RegistriesAttribute.of(registries)),
                                NbtDeserializer::of,
                                compoundOpt.get()
                        ));
                    } else {
                        LOGGER.warn("Missing compound at index " + i);
                    }
                }
            /*try {
                for (int i = 0; i < infoList.size(); i++) {
                    list.add(DeathInfo.ENDEC.decodeFully(
                            SerializationContext.attributes(RegistriesAttribute.of(registries)),
                            NbtDeserializer::of,
                            infoList.getCompound(i)
                    ));
                }*/
            } catch (Exception e) {
                LOGGER.error("Failed to decode death info", e);
            }

            future.complete(list);
        });

        return future;
    }

    protected void save(RegistryAccess registries, File file, List<DeathInfo> listIn) {
        final var list = ImmutableList.copyOf(listIn);
        Util.ioPool().service().submit(() -> {
            if (errored) {
                LOGGER.warn("Attempted to save DeathLog database even though disk operations are disabled");
                return;
            }

            final CompoundTag deathNbt = new CompoundTag();
            final ListTag infoList = new ListTag();

            list.forEach(deathInfo -> infoList.add(DeathInfo.ENDEC.encodeFully(
                    SerializationContext.attributes(RegistriesAttribute.of(registries)),
                    NbtSerializer::of,
                    deathInfo)
            ));

            deathNbt.put("Deaths", infoList);
            deathNbt.putInt("FormatRevision", FORMAT_REVISION);

            try {
                NbtIo.write(deathNbt, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error("Failed to save DeathLog database");
            }
        });
    }

    @Override
    public boolean isErrored() {
        return errored;
    }

    protected void raiseError(String error) {
        this.errored = true;
    }

    @Override
    public RegistryAccess registries() {
        return this.registries;
    }
}
