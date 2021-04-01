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

package de.gematik.poc.vaccination.certvac; // NOPMD many imports

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import com.gmail.alfred65fiedler.utils.AfiRng;
import com.gmail.alfred65fiedler.utils.Hex;
import de.gematik.poc.vaccination.userinterface.CmdLine;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests on {@link Vaccine}.
 */
// Note 1: Spotbugs claims ES_COMPARING_STRINGS_WITH_EQ, i.e.
//         Comparison of String objects using == or !=
//         The finding is correct and caused by using assertSame(String, String),
//         which is intentionally the way it is. Thus this finding is suppressed.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
    "ES_COMPARING_STRINGS_WITH_EQ" // see note 1
}) // */
final class TestInformationOfProof { // NOPMD too many methods
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
   * Test method for {@link InformationOfProof#decode(byte[])}.
   */
  @Test
  void test_decode__DataItem() throws CborException { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of valid inputs
    // --- c. ERROR: ClassCastException caused by dataItem
    // --- d. ERROR: in registrar
    // --- e. ERROR: in version
    // --- f. ERROR: in name
    // --- g. ERROR: in dayOfBirth
    // --- h. ERROR: in expirationDate
    // --- h. ERROR: in map

    // --- a. smoke test
    {
      final String name = "John DoeDeco";
      final LocalDate dayOfBirth = LocalDate.of(1973, 4, 28);
      final ZonedDateTime expirationDate = ZonedDateTime.of(
          2021, 3, 18,
          23, 54, 41, 123_456,
          CmdLine.TIME_ZONE
      );
      final Map<Diseases, HealthStatus> healthStatusMap = Map.ofEntries(// NOPMD use ConcurrentHaMap
          Map.entry(Diseases.COVID_19, new HealthStatus(6, 2)),
          Map.entry(Diseases.HEPATITIS_B, new HealthStatus(1, 4))
      );
      final InformationOfProof dut = new InformationOfProof(
          name,
          dayOfBirth,
          expirationDate,
          healthStatusMap
      );
      final InformationOfProof dutDeco = InformationOfProof.decode(dut.encode());

      assertEquals(name, dutDeco.getName());
      assertEquals(dayOfBirth, dutDeco.getDayOfBirth());
      assertTrue(
          Math.abs(ChronoUnit.MINUTES.between(expirationDate, dutDeco.getExpirationDate())) < 10
      );
      assertEquals(healthStatusMap, dutDeco.getHealthStatusMap());
    }

    // --- b. bunch of valid inputs
    final Set<Long> diffs = new HashSet<>();
    IntStream.rangeClosed(0, 2048)
        .parallel() // for performance boost
        .forEach(i -> {
          try {
            final String name = RNG.nextUtf8(0, 50);
            final int yearOfBirth = RNG.nextIntClosed(1900, 2050);
            final int monthOfBirth = RNG.nextIntClosed(1, 12);
            final int dayOfBirth = RNG.nextIntClosed(1, 28);
            final LocalDate birthDay = LocalDate.of(yearOfBirth, monthOfBirth, dayOfBirth);
            final int yearExpiration = RNG.nextIntClosed(2000, 2050);
            final int monthExpiration = RNG.nextIntClosed(1, 12);
            final int dayExpiration = RNG.nextIntClosed(1, 28);
            final int hourExpiration = RNG.nextIntClosed(3, 23);
            final int minuteExpiration = RNG.nextIntClosed(0, 59);
            final int secondExpiration = RNG.nextIntClosed(0, 59);
            final int nanosExpiration = RNG.nextIntClosed(0, 999_999);
            final ZonedDateTime expiration = ZonedDateTime.of(
                yearExpiration, monthExpiration, dayExpiration,
                hourExpiration, minuteExpiration, secondExpiration, nanosExpiration,
                CmdLine.TIME_ZONE
            );

            // create mapping from disease -> healthStatus
            final List<Diseases> diseases = new ArrayList<>(Arrays.asList(Diseases.values()));
            final int mapSize = RNG.nextIntClosed(1, diseases.size());
            final TreeMap<String, List<Object>> map = new TreeMap<>();
            final Map<Diseases, HealthStatus> healthStatusMap = new ConcurrentHashMap<>();
            while (map.size() < mapSize) {
              // pick a disease
              final Diseases disease = diseases.remove(RNG.nextIntClosed(0, diseases.size() - 1));

              // create health-status
              final int shieldStrength = RNG.nextIntClosed(0, HealthStatus.MAX_SHIELD_STRENGTH);
              final int harmlessness = RNG.nextIntClosed(0, HealthStatus.MAX_HARMLESSNESS);
              final HealthStatus healthStatus = new HealthStatus(// NOPMD new in loop
                  shieldStrength,
                  harmlessness
              );
              healthStatusMap.put(disease, healthStatus);

              final ByteArrayOutputStream baos = new ByteArrayOutputStream(); // NOPMD new in loop
              new CborEncoder(baos).encode(new CborBuilder()// NOPMD new in loop
                  .add(disease.getEncodedValue())
                  .build()
              );
              map.put(
                  Hex.toHexDigits(baos.toByteArray()), // key
                  List.of(disease, healthStatus) // value
              );
            } // end while (map size too small)

            // create device under test, i.e. DUT
            final InformationOfProof dut = new InformationOfProof(
                name,
                birthDay,
                expiration,
                healthStatusMap
            );
            final InformationOfProof dutDeco = InformationOfProof.decode(dut.encode());

            assertEquals(name, dutDeco.getName());
            assertEquals(birthDay, dutDeco.getDayOfBirth());
            diffs.add(Math.abs(
                ChronoUnit.SECONDS.between(expiration, dutDeco.getExpirationDate())
            ));
            assertEquals(healthStatusMap, dutDeco.getHealthStatusMap());
          } catch (CborException e) {
            fail("unexpected exception", e);
          } // end catch (CborException)
        }); // end forEach(i -> ...)

    assertTrue(
        diffs.stream().mapToLong(i -> i).max().getAsLong() < 2,
        diffs::toString
    );

    // --- c. ERROR: ClassCastException caused by dataItem
    // --- d. ERROR: in registrar
    // --- e. ERROR: in version
    // --- f. ERROR: in name
    // --- g. ERROR: in dayOfBirth
    // --- h. ERROR: in expirationDate
    // --- h. ERROR: in map
    // FIXME more tests required
  } // end method */

  /**
   * Test method for {@link InformationOfProof#encode()}.
   */
  @Test
  void test_encode() throws CborException { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of valid inputs

    // --- a. smoke test
    {
      final String name = "John DoeEnco";
      final LocalDate dayOfBirth = LocalDate.of(1673, 4, 28);
      final ZonedDateTime expirationDate = ZonedDateTime.of(
          2021, 3, 18,
          23, 54, 41, 123_456,
          CmdLine.TIME_ZONE
      );
      final Map<Diseases, HealthStatus> healthStatusMap = Map.ofEntries(// NOPMD use ConcurrentHaMap
          Map.entry(Diseases.COVID_19, new HealthStatus(6, 2)),
          Map.entry(Diseases.HEPATITIS_B, new HealthStatus(1, 4))
      );
      final InformationOfProof dut = new InformationOfProof(
          name,
          dayOfBirth,
          expirationDate,
          healthStatusMap
      );
      assertEquals(
          Hex.extractHexDigits(
              "38-18"                             // registrar     : negative integer -25
                  + "20"                          // version       : negative integer -1
                  + "6c-4a6f686e20446f65456e636f" // name          : text with six UTF-8 bytes
                  + "3a-0001a746"                 // dayOfBirth    : negative int32
                  + "1a-6053e841"                 // expirationDate: positive int32
                  + "a2-[(00-21) (02-09)]"        // map with two entries
          ),
          Hex.toHexDigits(dut.encode())
      );
    }

    // --- b. bunch of valid inputs
    IntStream.rangeClosed(0, 2048)
        .parallel() // for performance boost
        .forEach(i -> {
          try {
            final String name = RNG.nextUtf8(0, 50);
            final int yearOfBirth = RNG.nextIntClosed(1900, 2050);
            final int monthOfBirth = RNG.nextIntClosed(1, 12);
            final int dayOfBirth = RNG.nextIntClosed(1, 28);
            final LocalDate birthDay = LocalDate.of(yearOfBirth, monthOfBirth, dayOfBirth);
            final int yearExpiration = RNG.nextIntClosed(2000, 2050);
            final int monthExpiration = RNG.nextIntClosed(1, 12);
            final int dayExpiration = RNG.nextIntClosed(1, 28);
            final int hourExpiration = RNG.nextIntClosed(3, 23);
            final int minuteExpiration = RNG.nextIntClosed(0, 59);
            final int secondExpiration = RNG.nextIntClosed(0, 59);
            final int nanosExpiration = RNG.nextIntClosed(0, 999_999);
            final ZonedDateTime expiration = ZonedDateTime.of(
                yearExpiration, monthExpiration, dayExpiration,
                hourExpiration, minuteExpiration, secondExpiration, nanosExpiration,
                CmdLine.TIME_ZONE
            );

            // create mapping from disease -> healthStatus
            final List<Diseases> diseases = new ArrayList<>(Arrays.asList(Diseases.values()));
            final int mapSize = RNG.nextIntClosed(1, diseases.size());
            final TreeMap<String, List<Object>> map = new TreeMap<>();
            final Map<Diseases, HealthStatus> healthStatusMap = new ConcurrentHashMap<>();
            while (map.size() < mapSize) {
              // pick a disease
              final Diseases disease = diseases.remove(RNG.nextIntClosed(0, diseases.size() - 1));

              // create health-status
              final int shieldStrength = RNG.nextIntClosed(0, HealthStatus.MAX_SHIELD_STRENGTH);
              final int harmlessness = RNG.nextIntClosed(0, HealthStatus.MAX_HARMLESSNESS);
              final HealthStatus healthStatus = new HealthStatus(// NOPMD new in loop
                  shieldStrength,
                  harmlessness
              );
              healthStatusMap.put(disease, healthStatus);

              final ByteArrayOutputStream baos = new ByteArrayOutputStream(); // NOPMD new in loop
              new CborEncoder(baos).encode(new CborBuilder()// NOPMD new in loop
                  .add(disease.getEncodedValue())
                  .build()
              );
              map.put(
                  Hex.toHexDigits(baos.toByteArray()), // key
                  List.of(disease, healthStatus) // value
              );
            } // end while (map size too small)

            // create device under test, i.e. DUT
            final InformationOfProof dut = new InformationOfProof(
                name,
                birthDay,
                expiration,
                healthStatusMap
            );
            final byte[] pre = dut.encode();

            // --- estimate expected value
            // array-header, registrar and version
            final byte[] nameOctets = name.getBytes(StandardCharsets.UTF_8);
            final StringBuilder exp = new StringBuilder()// NOPMD use single append
                .append("38-18  ") // registrar, negative integer = -25
                .append("20  ");   // version,   negative integer = -1

            // name
            if (nameOctets.length < 24) { // NOPMD literal in conditional statement
              // ... text with major type and additional information in one octet
              exp.append(String.format("%02x", 0x60 | nameOctets.length));
            } else if (nameOctets.length < 256) { // NOPMD literal in conditional statement
              // ... text with major type and additional information in two octet
              exp.append(String.format("78-%02x", nameOctets.length));
            } else {
              // ... text with major type and additional information in four octet
              exp.append(String.format("79-%04x", nameOctets.length));
            } // end if
            exp.append(Hex.toHexDigits(nameOctets));

            // dayOfBirth
            final int deltaBirth = (int) ChronoUnit.DAYS.between(LocalDate.EPOCH, birthDay);
            if (deltaBirth >= 0) {
              // ... positive delta
              if (deltaBirth < 24) { // NOPMD literal in conditional statement
                // ... positive integer on one octet
                exp.append(String.format("  %02x", deltaBirth));
              } else if (deltaBirth < 256) { // NOPMD literal in conditional statement
                // ... positive integer on two octet
                exp.append(String.format("  18-%02x", deltaBirth));
              } else {
                // ... positive integer on three octet
                exp.append(String.format("  19-%04x", deltaBirth));
              } // end else
            } else {
              // ... negative delta
              if (deltaBirth > -25) { // NOPMD literal in conditional statement
                // ... negative integer on one octet
                exp.append(String.format("  %02x", 0x20 | (- 1 - deltaBirth)));
              } else if (deltaBirth > -257) { // NOPMD literal in conditional statement
                // ... negative integer on two octet
                exp.append(String.format("  38-%02x", - 1 - deltaBirth));
              } else {
                // ... negative integer on three octet
                exp.append(String.format("  39-%04x", - 1 - deltaBirth));
              } // end else
            } // end else (delta >= 0)

            // expiration date
            final long expirationDate = expiration.toEpochSecond();
            if (expirationDate < 24) { // NOPMD literal in conditional statemement
              // ... positive integer on one octet
              exp.append(String.format("  %02x", expirationDate));
            } else if (expirationDate < 256) { // NOPMD literal in conditional statement
              // ... positive integer on two octet
              exp.append(String.format("  18-%02x", expirationDate));
            } else if (expirationDate < 0x1_0000) { // NOPMD literal in conditional statement
              // ... positive integer on three octet
              exp.append(String.format("  19-%04x", expirationDate));
            } else {
              // ... positive integer on five octet
              exp.append(String.format("  1a-%08x", expirationDate));
            } // end else

            // mapping of disease to healthStatus
            exp.append(String.format("  %02x-[", 0xa0 | mapSize));
            map.forEach((key, value) -> {
              final int disEncoded = ((Diseases) value.get(0)).getEncodedValue();
              final int healthStat = ((HealthStatus) value.get(1)).encode();
              // ... assertion 1: -25 < disEncoded < 24 => encoded in one octet
              // ... assertion 2: -25 < healthStat < 24 => encoded in one octet

              if (disEncoded >= 0) {
                exp.append(String.format("(%02x-", disEncoded));
              } else {
                exp.append(String.format("(%02x-", 0x20 | (- 1 - disEncoded)));
              } // end else

              if (healthStat >= 0) {
                exp.append(String.format("%02x)", healthStat));
              } else {
                exp.append(String.format("(%02x-", 0x20 | (- 1 - healthStat)));
              } // end else
            });
            exp.append(']'); // end of map AND end of array

            assertEquals(
                Hex.extractHexDigits(exp.toString()),
                Hex.toHexDigits(pre),
                exp::toString
            );
          } catch (CborException e) {
            fail("unexpected exception", e);
          } // end catch (CborException)
        }); // end forEach(i -> ...)
  } // end method */

  /**
   * Test method for {@link InformationOfProof#getDayOfBirth()}.
   */
  @Test
  void test_getDayOfBirth() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String name = "John DoeDay";
    final LocalDate dayOfBirth = LocalDate.of(1673, 4, 28);
    final ZonedDateTime expirationDate = ZonedDateTime.of(
        2021, 3, 18,
        23, 54, 41, 123_456,
        CmdLine.TIME_ZONE
    );
    final Map<Diseases, HealthStatus> healthStatusMap = Map.ofEntries(// NOPMD use ConcurrentHashMap
        Map.entry(Diseases.COVID_19, new HealthStatus(6, 2)),
        Map.entry(Diseases.HEPATITIS_B, new HealthStatus(1, 4))
    );
    final InformationOfProof dut = new InformationOfProof(
        name,
        dayOfBirth,
        expirationDate,
        healthStatusMap
    );
    assertSame(dayOfBirth, dut.getDayOfBirth());
  } // end method */

  /**
   * Test method for {@link InformationOfProof#getName()}.
   */
  @Test
  void test_getName() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String name = "John DoeName";
    final LocalDate dayOfBirth = LocalDate.of(1673, 4, 28);
    final ZonedDateTime expirationDate = ZonedDateTime.of(
        2021, 3, 18,
        23, 54, 41, 123_456,
        CmdLine.TIME_ZONE
    );
    final Map<Diseases, HealthStatus> healthStatusMap = Map.ofEntries(// NOPMD use ConcurrentHashMap
        Map.entry(Diseases.COVID_19, new HealthStatus(6, 2)),
        Map.entry(Diseases.HEPATITIS_B, new HealthStatus(1, 4))
    );
    final InformationOfProof dut = new InformationOfProof(
        name,
        dayOfBirth,
        expirationDate,
        healthStatusMap
    );
    assertSame(name, dut.getName()); // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
  } // end method */

  /**
   * Test method for {@link InformationOfProof#toString()}.
   */
  @Test
  void test_toString() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String name = "John DoeString";
    final LocalDate dayOfBirth = LocalDate.of(1673, 4, 28);
    final ZonedDateTime expirationDate = ZonedDateTime.of(
        2021, 3, 18,
        23, 54, 41, 123_456,
        CmdLine.TIME_ZONE
    );
    final Map<Diseases, HealthStatus> healthStatusMap = new TreeMap<>(Map.ofEntries(// NOPMD
        Map.entry(Diseases.COVID_19, new HealthStatus(6, 2)),
        Map.entry(Diseases.HEPATITIS_B, new HealthStatus(1, 4))
    ));
    final InformationOfProof dut = new InformationOfProof(
        name,
        dayOfBirth,
        expirationDate,
        healthStatusMap
    );
    // John DoeString,
    // born on 1673-04-28.
    // Expiration date of this information is 2021-03-18T23:54:41.000123456Z,
    // info: {Covid-19=(shield=6, harmlessness=2), Hepatitis B=(shield=1, harmlessness=4)}
    assertEquals(
        "John DoeString, "
            + "born on 1673-04-28. "
            + "Expiration date of this information is 2021-03-18T23:54:41.000123456Z, "
            + "info: {"
            + "Covid-19=(shield=6, harmlessness=2), "
            + "Hepatitis B=(shield=1, harmlessness=4)"
            + "}",
        dut.toString()
    );
  } // end method */
} // end class
