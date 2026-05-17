package com.cargohub.mobile.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a mutation that was performed offline and needs to be synced
 * when connectivity is restored.
 */
@Entity(tableName = "pending_operations")
public class PendingOperationEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Operation type: CHANGE_STATE, CREATE_INCIDENCIA */
    public String operationType;

    /** JSON-encoded payload with all parameters needed to replay the operation */
    public String payload;

    /** Timestamp when the operation was queued */
    public long createdAt;

    /** Number of retry attempts so far */
    public int retryCount;

    /** Last error message if sync failed */
    public String lastError;
}
