package io.iprf.state;

import java.util.Optional;

/**
 * Read access to pre-computed account profiles.
 *
 * <p><b>Read-only by design.</b> There is no write method on this interface. The
 * in-path layers must not be able to populate what they read — population
 * happens at startup and through asynchronous updaters, outside the payment
 * path. Any implementation that performs a live query when a profile is missing
 * violates the in-path contract regardless of how fast that query is.
 *
 * <p>A missing profile is a normal outcome, returned as an empty
 * {@link Optional}, not an exception. The caller degrades explicitly.
 */
public interface AccountProfileStore {

    /** Returns the pre-computed profile for an account, or empty if none exists. */
    Optional<AccountProfile> findByAccountId(String accountId);

    /** Number of profiles currently loaded. Exposed for readiness reporting. */
    int size();
}
