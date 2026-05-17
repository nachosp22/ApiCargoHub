package com.cargohub.mobile.data;

import androidx.annotation.NonNull;

public interface RepositoryCallback<T> {
    void onResult(@NonNull RepositoryResult<T> result);
}
