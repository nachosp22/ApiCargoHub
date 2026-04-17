package com.cargohub.mobile.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cargohub.mobile.data.RepositoryCallback;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.network.ConnectivityObserver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility for repositories that want cache-first behavior.
 * Checks connectivity, returns cached data when offline,
 * and caches network responses for future offline use.
 */
public final class OfflineSupport {

    private static final ExecutorService IO = Executors.newFixedThreadPool(2);

    private OfflineSupport() {
    }

    /**
     * If offline and a cached value is available, return it immediately via callback.
     * Returns true if cache was served (caller should skip network call).
     */
    public static <T> boolean serveCacheIfOffline(@NonNull Context context,
                                                   @NonNull CacheLoader<T> cacheLoader,
                                                   @NonNull RepositoryCallback<T> callback) {
        if (ConnectivityObserver.getInstance(context).isOnline()) {
            return false;
        }

        IO.execute(() -> {
            T cached = cacheLoader.load();
            if (cached != null) {
                callback.onResult(RepositoryResult.cached(cached));
            } else {
                callback.onResult(RepositoryResult.error(
                        "Sin conexión y sin datos en caché.", -1, false));
            }
        });
        return true;
    }

    /**
     * Save data to cache on a background thread.
     */
    public static void cacheInBackground(@NonNull Runnable cacheAction) {
        IO.execute(cacheAction);
    }

    /**
     * Functional interface for loading cached data from Room.
     */
    public interface CacheLoader<T> {
        @Nullable
        T load();
    }
}
