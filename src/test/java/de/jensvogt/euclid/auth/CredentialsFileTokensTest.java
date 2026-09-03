package de.jensvogt.euclid.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An application euclid deployed authenticates with a token out of a file euclid rewrites roughly
 * every half hour. What these cover is the one thing that has to be true for such an application
 * to keep working past the first hour: that the token going onto the wire is the one in the file
 * now, not the one that was in it when the client was built.
 */
class CredentialsFileTokensTest {

    @TempDir
    Path directory;

    /**
     * Writes a credentials file the way euclid does - to a temporary name, then renamed over the
     * target - so the test exercises the same replacement the reader has to notice.
     */
    private Path writeCredentials(String token, String userId) throws IOException {
        final Path path = directory.resolve("credentials");
        final Path temporary = directory.resolve("credentials.new");
        Files.writeString(temporary, """
                {"token":"%s","expiresAt":"2026-09-02T12:00:00Z","userId":"%s",\
                "accountId":"000000000000","region":"eu-central-1","endpoint":"https://localhost:5566"}"""
                .formatted(token, userId));
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        return path;
    }

    @Test
    void readsTheTokenTheFileHolds() throws IOException {
        final Path path = writeCredentials("token-1", "app-user");

        final CredentialsFileTokens tokens = CredentialsFileTokens.forFile(path);

        assertEquals("token-1", tokens.get());
        assertEquals("app-user", tokens.userId());
    }

    @Test
    void picksUpATokenEuclidRotatedUnderneathIt() throws IOException, InterruptedException {
        final Path path = writeCredentials("token-1", "app-user");
        final CredentialsFileTokens tokens = CredentialsFileTokens.forFile(path);
        assertEquals("token-1", tokens.get());

        // Modification time has a one-second granularity on some filesystems, and a rotation an
        // application would actually see is half an hour apart.
        Thread.sleep(1100);
        writeCredentials("token-2", "app-user");

        assertEquals("token-2", tokens.get(), "the request after a rotation has to carry the new token");
    }

    @Test
    void keepsTheLastTokenWhenTheFileGoesAway() throws IOException {
        final Path path = writeCredentials("token-1", "app-user");
        final CredentialsFileTokens tokens = CredentialsFileTokens.forFile(path);
        assertEquals("token-1", tokens.get());

        Files.delete(path);

        // A token that may still be good beats a request that is certain to fail: whoever removed
        // the file has a bigger problem than this call, and it is not this call's to report.
        assertEquals("token-1", tokens.get());
    }

    @Test
    void aFileThatWasNeverReadableSuppliesNothing() {
        assertEquals("", CredentialsFileTokens.forFile(directory.resolve("absent")).get());
    }

    @Test
    void aFileWithoutATokenSuppliesNothing() throws IOException {
        final Path path = directory.resolve("credentials");
        Files.writeString(path, "{\"userId\":\"app-user\"}");

        assertEquals("", CredentialsFileTokens.forFile(path).get());
    }

    @Test
    void garbageIsNotATokenAndIsNotAnException() throws IOException {
        final Path path = directory.resolve("credentials");
        Files.writeString(path, "this is not json");

        assertEquals("", CredentialsFileTokens.forFile(path).get());
    }

    @Test
    void demandingManagedCredentialsWithoutAnyIsAMistakeWorthHearingAbout() {
        // Only exercisable when the variable really is unset, which it is in a test JVM; the point
        // is that this fails loudly rather than handing back an empty token that 401s later.
        if (System.getenv(CredentialsFileTokens.CREDENTIALS_FILE_VARIABLE) == null) {
            assertThrows(IllegalStateException.class, CredentialsFileTokens::fromEnvironment);
        }
    }

    // ── What a client does with all this ──────────────────────────────────────

    @Test
    void aClientInsideADeployedApplicationFollowsTheFile() throws IOException, InterruptedException {
        final Path path = writeCredentials("token-1", "app-user");

        final Supplier<String> supplier = CredentialsFileTokens.forClient("token-1", "app-user", path.toString());
        assertEquals("token-1", supplier.get());

        Thread.sleep(1100);
        writeCredentials("token-2", "app-user");

        assertEquals("token-2", supplier.get(),
                     "a client built for the user the file names has to follow the file, or it stops working after an hour");
    }

    @Test
    void aClientBuiltFromAStaleTokenStillFollowsTheFile() throws IOException {
        // The application read the file at start-up and only built its client later, by which time
        // euclid had already rotated. Matching on the token rather than the identity would freeze
        // the client on the stale copy - which is exactly the failure this is meant to prevent.
        final Path path = writeCredentials("token-2", "app-user");

        final Supplier<String> supplier = CredentialsFileTokens.forClient("token-1", "app-user", path.toString());

        assertEquals("token-2", supplier.get());
    }

    @Test
    void aClientBuiltForSomebodyElseKeepsItsOwnToken() throws IOException {
        // An application may log in as another user while its own credentials sit in a file it is
        // not using. Swapping in the application's identity would change who the call is made as.
        final Path path = writeCredentials("token-1", "app-user");

        final Supplier<String> supplier = CredentialsFileTokens.forClient("somebody-elses-token", "another-user", path.toString());

        assertEquals("somebody-elses-token", supplier.get());
    }

    @Test
    void aClientOutsideADeployedApplicationKeepsItsToken() {
        assertEquals("token-1", CredentialsFileTokens.forClient("token-1", "app-user", null).get());
        assertEquals("token-1", CredentialsFileTokens.forClient("token-1", "app-user", "  ").get());
    }

    @Test
    void anUnreadableCredentialsFileLeavesTheClientAsItWas() {
        final Supplier<String> supplier =
                CredentialsFileTokens.forClient("token-1", "app-user", directory.resolve("absent").toString());

        assertEquals("token-1", supplier.get());
        assertNotSame(CredentialsFileTokens.class, supplier.getClass());
    }

    @Test
    void anIdentityNeitherSideNamesIsNotAMismatch() throws IOException {
        // A client built without a user ID has not said it means somebody else.
        final Path path = writeCredentials("token-1", "app-user");

        assertEquals("token-1", CredentialsFileTokens.forClient("", "", path.toString()).get());
        assertTrue(CredentialsFileTokens.forClient("", "", path.toString()) instanceof CredentialsFileTokens);
    }

    @Test
    void statingAnUnchangedFileDoesNotProduceANewAnswer() throws IOException {
        final Path path = writeCredentials("token-1", "app-user");
        final CredentialsFileTokens tokens = CredentialsFileTokens.forFile(path);

        // Not an optimisation detail: get() is called once per request, and re-reading and
        // re-parsing the file every time would put that cost on every call the application makes.
        assertSame(tokens.get(), tokens.get());
    }
}
