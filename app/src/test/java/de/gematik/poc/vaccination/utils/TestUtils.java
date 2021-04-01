/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.poc.vaccination.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.gmail.alfred65fiedler.tlv.BerTlv;
import com.gmail.alfred65fiedler.tlv.DerBoolean;
import com.gmail.alfred65fiedler.tlv.DerDate;
import com.gmail.alfred65fiedler.tlv.DerInteger;
import com.gmail.alfred65fiedler.tlv.DerNull;
import com.gmail.alfred65fiedler.tlv.DerSequence;
import com.gmail.alfred65fiedler.tlv.DerUtf8String;
import com.gmail.alfred65fiedler.utils.Hex;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class performing white-box tests on {@link Utils}.
 */
final class TestUtils {
  /**
   * Temporary Directory.
   */
  @TempDir
  /* package */ static Path claTempDir; // NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR */

  /**
   * Method executed before other tests.
   */
  @BeforeAll
  static void setUpBeforeClass() {
    // intentionally empty
  } // end method */

  /**
   * Method executed after other tests.
   */
  @AfterAll
  static void tearDownAfterClass() {
    // intentionally empty
  } // end method */

  /**
   * Method executed before each test.
   */
  @BeforeEach
  void setUp() {
    // intentionally empty
  } // end method */

  /**
   * Method executed after each test.
   */
  @AfterEach
  void tearDown() {
    // intentionally empty
  } // end method */

  /**
   * Test method for {@link Utils#exportTlv(Path, String, BerTlv)}.
   */
  @Test
  void test_exportTlv__Path_String_BerTlv() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test with a bunch of input-value-combinations
    List.of(
        claTempDir.resolve("a"),
        claTempDir.resolve("b")
    ).forEach(dir -> {
      try {
        Files.createDirectories(dir);

        for (final String prefix : List.of(
            "prefixA",
            "prefix-B.4711"
        )) {
          for (final BerTlv tlv : List.of(
              DerNull.NULL,
              DerBoolean.FALSE,
              DerBoolean.TRUE,
              new DerInteger(BigInteger.TEN),   // NOPMD new in loop
              new DerSequence(List.of(//           NOPMD new in loop
                  new DerUtf8String("foo bar"), // NOPMD new in loop
                  new DerDate(LocalDate.now())  // NOPMD new in loop
              ))
          )) {
            Utils.exportTlv(dir, prefix, tlv);

            final Path pathBin = dir.resolve(prefix + ".bin");
            assertTrue(Files.isRegularFile(pathBin));
            final byte[] content = Files.readAllBytes(pathBin);
            assertArrayEquals(
                content,
                tlv.toByteArray(),
                () -> Hex.toHexDigits(content)
            );

            final Path pathTxt = dir.resolve(prefix + "-bin.txt");
            assertTrue(Files.isRegularFile(pathTxt));
            assertEquals(
                new String(Files.readAllBytes(pathTxt), StandardCharsets.UTF_8), // NOPMD new
                tlv.toString(" ", "|  ")
            );

            final Path pathTree = dir.resolve(prefix + "-bin_explanation.txt");
            assertTrue(Files.isRegularFile(pathTree));
            assertEquals(
                new String(Files.readAllBytes(pathTree), StandardCharsets.UTF_8), // NOPMD new
                tlv.toStringTree()
            );
          } // end for (tlv...)
        } // end for (prefix...)
      } catch (IOException e) {
        fail("unexpected exception", e);
      } // end catch (IOException)
    }); // end forEach(dir -> ...)
  } // end method */
} // end class
