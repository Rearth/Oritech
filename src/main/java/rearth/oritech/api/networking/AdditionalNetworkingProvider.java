package rearth.oritech.api.networking;

import java.lang.reflect.Field;
import java.util.List;

public interface AdditionalNetworkingProvider {
    List<Field> additionalSyncedFields(SyncType type);
}
