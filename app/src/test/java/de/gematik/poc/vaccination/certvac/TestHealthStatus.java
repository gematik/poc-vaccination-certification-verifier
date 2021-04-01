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

package de.gematik.poc.vaccination.certvac; // NOPMD too many static imports

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;
import com.gmail.alfred65fiedler.utils.AfiRng;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests on {@link HealthStatus}.
 */
final class TestHealthStatus {
  /**
   * Fail message in case of unexpected exception.
   */
  private static final String UNEXPECTED_EXCEPTION = "unexpected exception"; // */

  /**
   * Random Number Generator.
   */
  private static final AfiRng RNG = new AfiRng(); // */

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
   * Test method for {@link HealthStatus#HealthStatus(int, int)}.
   */
  @Test
  void test__HealthStatus__int_int() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test with valid values
    // --- b. ERROR: invalid harmlessness
    // --- c. ERROR: invalid shieldStrength

    // --- a. smoke test with valid values
    IntStream.rangeClosed(0, HealthStatus.MAX_HARMLESSNESS)
        .parallel() // for performance boost
        .forEach(harmlessness -> {
          IntStream.rangeClosed(0, HealthStatus.MAX_SHIELD_STRENGTH).forEach(shieldStrength -> {
            final HealthStatus dut = new HealthStatus(shieldStrength, harmlessness);

            assertEquals(harmlessness, dut.getHarmlessness());
            assertEquals(shieldStrength, dut.getShieldStrength());
          }); // end forEach(shieldStrength -> ...)
        }); // end forEach(harmlessness -> ...)

    // --- b. ERROR: invalid harmlessness
    List.of(
        -1,                              // supremum of invalid values too small
        HealthStatus.MAX_HARMLESSNESS + 1 // infimum  of invalid values too large
    ).forEach(harmlessness -> {
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> new HealthStatus(
              RNG.nextIntClosed(0, HealthStatus.MAX_SHIELD_STRENGTH),
              harmlessness
          )
      );

      assertEquals(
          "harmlessness out of range [0, " + HealthStatus.MAX_HARMLESSNESS + "]",
          throwable.getMessage()
      );
      assertNull(throwable.getCause());
    }); // end forEach(harmlessness -> ...)

    // --- c. ERROR: invalid shieldStrength
    List.of(
        -1,                                 // supremum of invalid values too small
        HealthStatus.MAX_SHIELD_STRENGTH + 1 // infimum  of invalid values too large
    ).forEach(shieldStrength -> {
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> new HealthStatus(
              shieldStrength,
              RNG.nextIntClosed(0, HealthStatus.MAX_HARMLESSNESS)
          )
      );

      assertEquals(
          "shieldStrength out of range [0, " + HealthStatus.MAX_SHIELD_STRENGTH + "]",
          throwable.getMessage()
      );
      assertNull(throwable.getCause());
    }); // end forEach(shieldStrength -> ...)
  } // end method */

  /**
   * Test method for {@link HealthStatus#decode(Registrar, int, DataItem)}.
   *
   * <p>Here {@code registrar=49} and {@code version=1} is tested
   */
  @Test
  void test_decode__int_int_DataItem() throws // NOPMD '_' character in name of method
      CborException {
    // Test strategy:
    // --- a. (+49, -1) loop over all possible values
    // --- b. (+49, -1) ERROR: ClassCastException due unexpected DataItem
    // --- c. (+49, -1) ERROR: ArithmeticException due to value out of range
    // --- d. (+49, -1) ERROR: invalid harmlessness
    // --- e. (+49, -1) ERROR: invalid shieldStrength
    // --- z. ERROR: invalid version

    // --- a. (+49, -1) loop over all possible values
    final Registrar registrarA = Registrar.GERMANY;
    final int versionA = -1;
    IntStream.rangeClosed(0, HealthStatus.MAX_HARMLESSNESS)
        .parallel() // for performance boost
        .forEach(harmlessness -> {
          IntStream.rangeClosed(0, HealthStatus.MAX_SHIELD_STRENGTH).forEach(shieldStrength -> {
            try {
              final ByteArrayOutputStream baos = new ByteArrayOutputStream();
              new CborEncoder(baos).encode(new CborBuilder()
                  .add((harmlessness << 3) + shieldStrength - HealthStatus.OFFSET)
                  .build()
              );
              final List<DataItem> items = CborDecoder.decode(baos.toByteArray());

              assertEquals(1, items.size());

              final DataItem item = items.get(0);
              final HealthStatus dut = HealthStatus.decode(registrarA, versionA, item);

              assertEquals(harmlessness, dut.getHarmlessness());
              assertEquals(shieldStrength, dut.getShieldStrength());
            } catch (CborException e) {
              fail(UNEXPECTED_EXCEPTION, e);
            } // end catch (CborException)
          }); // end forEach(shieldStrength -> ...)
        }); // end forEach(harmlessness -> ...)

    // --- b. (+49, -1) ERROR: ClassCastException due unexpected DataItem
    {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      new CborEncoder(baos).encode(new CborBuilder()
          .add(true)
          .build()
      );
      final List<DataItem> items = CborDecoder.decode(baos.toByteArray());

      assertEquals(1, items.size());

      final DataItem item = items.get(0);

      assertThrows(
          ClassCastException.class,
          () -> HealthStatus.decode(registrarA, versionA, item)
      );
    }

    // --- c. (+49, -1) ERROR: ArithmeticException due to value out of range
    List.of(
        Integer.MIN_VALUE - 1L, // supremum of invalid negative values
        Integer.MAX_VALUE + 1L  // infimum  of invalid positive values
    ).forEach(value -> {
      try {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
            .add(value)
            .build()
        );
        final List<DataItem> items = CborDecoder.decode(baos.toByteArray());

        assertEquals(1, items.size());

        final DataItem item = items.get(0);

        assertThrows(
            ArithmeticException.class,
            () -> HealthStatus.decode(registrarA, versionA, item)
        );
      } catch (CborException e) {
        fail(UNEXPECTED_EXCEPTION, e);
      } // end catch (CborException)
    }); // end forEach(value -> ...)

    // --- d. (+49, -1) ERROR: invalid harmlessness
    // intentionally not tested here, because constructor is responsible for such tests

    // --- e. (+49, -1) ERROR: invalid shieldStrength
    // intentionally not tested here, because constructor is responsible for such tests

    // --- z. ERROR: invalid version
    List.of(
        versionA - 1, // supremum of invalid values too small
        versionA + 1  // infimum  of invalid values too large
    ).forEach(version -> {
      try {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
            .add(7)
            .build()
        );
        final List<DataItem> items = CborDecoder.decode(baos.toByteArray());

        assertEquals(1, items.size());

        final DataItem item = items.get(0);

        final Throwable throwable = assertThrows(
            IllegalArgumentException.class,
            () -> HealthStatus.decode(registrarA, version, item)
        );
        assertEquals("unknown version: " + version, throwable.getMessage());
        assertNull(throwable.getCause());
      } catch (CborException e) {
        fail(UNEXPECTED_EXCEPTION, e);
      } // end catch (CborException)
    }); // end forEach(version -> ...)
  } // end method */

  /**
   * Test method for {@link HealthStatus#encode()}.
   */
  @Test
  void test_encode() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test with valid values
    IntStream.rangeClosed(0, HealthStatus.MAX_HARMLESSNESS)
        .parallel() // for performance boost
        .forEach(harmlessness -> {
          IntStream.rangeClosed(0, HealthStatus.MAX_SHIELD_STRENGTH).forEach(shieldStrength -> {
            final HealthStatus dut = new HealthStatus(shieldStrength, harmlessness);
            assertEquals(
                (harmlessness << 3) + shieldStrength - HealthStatus.OFFSET,
                dut.encode()
            );
          }); // end forEach(shieldStrength -> ...)
        }); // end forEach(harmlessness -> ...)
  } // end method */

  /**
   * Test method for {@link HealthStatus#getHarmlessness()}.
   */
  @Test
  void test_getHarmlessness() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test with valid values

    // --- a. smoke test with valid values
    IntStream.rangeClosed(0, HealthStatus.MAX_HARMLESSNESS)
        .parallel() // for performance boost
        .forEach(harmlessness -> {
          IntStream.rangeClosed(0, HealthStatus.MAX_SHIELD_STRENGTH).forEach(shieldStrength -> {
            final HealthStatus dut = new HealthStatus(shieldStrength, harmlessness);

            assertEquals(harmlessness, dut.getHarmlessness());
          }); // end forEach(shieldStrength -> ...)
        }); // end forEach(harmlessness -> ...)
  } // end method */

  /**
   * Test method for {@link HealthStatus#getShieldStrength()}.
   */
  @Test
  void test_getShieldStrength() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test with valid values

    // --- a. smoke test with valid values
    IntStream.rangeClosed(0, HealthStatus.MAX_HARMLESSNESS)
        .parallel() // for performance boost
        .forEach(harmlessness -> {
          IntStream.rangeClosed(0, HealthStatus.MAX_SHIELD_STRENGTH).forEach(shieldStrength -> {
            final HealthStatus dut = new HealthStatus(shieldStrength, harmlessness);

            assertEquals(shieldStrength, dut.getShieldStrength());
          }); // end forEach(shieldStrength -> ...)
        }); // end forEach(harmlessness -> ...)
  } // end method */
} // end class
