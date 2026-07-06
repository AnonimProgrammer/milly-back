package com.milly.common.idempotency;

/**
 * A cached outcome for a given idempotency scope key.
 *
 * @param requestHash hash of the request body that produced this response, used to detect
 *                     the same key being reused with a different payload
 * @param status       HTTP status of the original response
 * @param body         body of the original response, replayed verbatim on retry
 */
public record IdempotencyRecord(String requestHash, int status, Object body) {
}
