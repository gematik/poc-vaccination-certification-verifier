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

package de.gematik.poc.vaccination.certvac;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gmail.alfred65fiedler.tlv.DerBoolean;
import com.gmail.alfred65fiedler.tlv.DerDate;
import com.gmail.alfred65fiedler.tlv.DerInteger;
import com.gmail.alfred65fiedler.tlv.DerOctetString;
import com.gmail.alfred65fiedler.tlv.DerPrintableString;
import com.gmail.alfred65fiedler.tlv.DerSequence;
import com.gmail.alfred65fiedler.tlv.DerUtf8String;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests on {@link PersonalIdentifiableInformation}.
 */
// Note 1: Spotbugs claims ES_COMPARING_STRINGS_WITH_EQ, i.e.
//         Comparison of String objects using == or !=
//         The finding is correct and caused by using assertSame(String, String),
//         which is intentionally the way it is. Thus this finding is suppressed.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
    "ES_COMPARING_STRINGS_WITH_EQ" // see note 1
}) // */
final class TestVaccination {
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
   * Test method for {@link Vaccination#Vaccination(String, String, String, String)}.
   */
  @Test
  void test__Vacciantion__String4() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String vacManu = "Johnson Constructor";
    final String vacName = "Covid-Const";
    final String immBatch = "Lot-123";
    final String immDate = "1978-04-23";
    final Vaccination dut = new Vaccination(
        vacManu, vacName, immBatch, immDate
    );
    assertSame(vacManu, dut.getVaccine().getManufacturer()); //        ES_COMPARING_STRINGS_WITH_EQ
    assertSame(vacName, dut.getVaccine().getName());      // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
    assertSame(immBatch, dut.getBatch());                 // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
    assertEquals(LocalDate.of(1978, 4, 23), dut.getDate());
  } // end method */

  /**
   * Test method for {@link Vaccination#Vaccination(Vaccine, String, LocalDate)}.
   */
  @Test
  void test__Vacciantion__Vaccine_String_String() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final Vaccine vaccine = new Vaccine("JohnsonComfort", "CovidComfort");
    final String batch = "Lot-124";
    final LocalDate date = LocalDate.of(1986, 7, 23);
    final Vaccination dut = new Vaccination(
        vaccine, batch, date
    );
    assertSame(vaccine, dut.getVaccine());
    assertSame(batch, dut.getBatch()); // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
    assertSame(date, dut.getDate());
  } // end method */

  /**
   * Test method for {@link Vaccination#decode(DerSequence)}.
   */
  @Test
  void test_decode__DerSequence() { // NOPMD '_' character in name of method
    // Assertions:
    // ... a. Methods from class Vaccine.java work as expected.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR, ArithmeticException
    // --- c. ERROR, ClassCastException
    // --- d. ERROR, IllegalArgumentException
    // --- e. ERROR, IndexOutOfBoundsException

    // --- a. smoke test
    {
      final String vacManu = "Johnson Constructor";
      final String vacName = "Covid-Const";
      final String immBatch = "Lot-123";
      final LocalDate immDate = LocalDate.of(1879, 4, 23);
      final Vaccination dut = Vaccination.decode(
          new DerSequence(List.of(
              new DerInteger(BigInteger.valueOf(1)),
              new Vaccine(vacManu, vacName).encode(),
              new DerUtf8String(immBatch),
              new DerDate(immDate)
          ))
      );

      assertSame(vacManu, dut.getVaccine().getManufacturer()); //       ES_COMPARING_STRINGS_WITH_EQ
      assertSame(vacName, dut.getVaccine().getName());     // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
      assertSame(immBatch, dut.getBatch());                // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
      assertEquals(LocalDate.of(1879, 4, 23), dut.getDate());
    }

    // --- b. ERROR, ArithmeticException
    assertThrows(
        ArithmeticException.class,
        () -> Vaccination.decode(
            new DerSequence(List.of(
                new DerInteger(BigInteger.valueOf(Integer.MAX_VALUE + 1L))
            ))
        )
    );

    // --- c. ERROR, ClassCastException
    // c.0 wrong class for version
    // c.1 wrong class for vaccine
    // c.2 wrong class for batch
    // c.3 wrong class for date
    // c.x everything is fine
    List.of(
        // c.0 wrong class for version
        new DerSequence(List.of(
            new DerOctetString(BigInteger.valueOf(1).toByteArray())
        )),

        // c.1 wrong class for vaccine
        new DerSequence(List.of(
            new DerInteger(BigInteger.valueOf(1)),
            DerBoolean.TRUE
        )),

        // c.2 wrong class for batch
        new DerSequence(List.of(
            new DerInteger(BigInteger.valueOf(1)),
            new Vaccine("BionC2", "AntiCov2").encode(),
            new DerPrintableString("Lot-2")
        )),

        // c.3 wrong class for date
        new DerSequence(List.of(
            new DerInteger(BigInteger.valueOf(1)),
            new Vaccine("BionC3", "AntiCov3").encode(),
            new DerUtf8String("Lot-3"),
            new DerUtf8String("1987-02-31")
        ))
    ).forEach(tlv -> assertThrows(
        ClassCastException.class,
        () -> Vaccination.decode(tlv)
    )); // end forEach(tlv -> ...)


    // c.x everything is fine
    {
      assertDoesNotThrow(
          () -> Vaccination.decode(
              new DerSequence(List.of(
                  new DerInteger(BigInteger.valueOf(1)),
                  new Vaccine("BionCx", "AntiCovX").encode(),
                  new DerUtf8String("Lot-X"),
                  new DerDate(LocalDate.now())
              ))
          )
      );
    }

    // --- d. ERROR, IllegalArgumentException
    List.of(
        0, // supremum of not-supported version numbers
        2  // infimum of not-supported version numbers
    ).forEach(version -> assertThrows(
        IllegalArgumentException.class,
        () -> Vaccination.decode(
            new DerSequence(List.of(
                new DerInteger(BigInteger.valueOf(version))
            ))
        )
    )); // end forEach(version -> ...)

    // --- e. ERROR, IndexOutOfBoundsException
    // date missing
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> Vaccination.decode(
            new DerSequence(List.of(
                new DerInteger(BigInteger.valueOf(1)),
                new Vaccine("BionCx", "AntiCovX").encode(),
                new DerUtf8String("Lot-X")
            ))
        )
    );
  } // end method */

  /**
   * Test method for {@link Vaccination#encode()}.
   */
  @Test
  void test_encode() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final Vaccine vaccine = new Vaccine("JohnsonComfort", "CovidComfort");
    final String batch = "Lot-124";
    final LocalDate date = LocalDate.of(1986, 7, 23);
    final Vaccination dut = new Vaccination(
        vaccine, batch, date
    );
    assertEquals(
        new DerSequence(List.of(
            new DerInteger(BigInteger.valueOf(1)),
            vaccine.encode(),
            new DerUtf8String(batch),
            new DerDate(date)
        )),
        dut.encode()
    );
  } // end method */

  /**
   * Test method for {@link Vaccination#getBatch()}.
   */
  @Test
  void test_getBatch() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String vacManu = "Johnson Getter3";
    final String vacName = "Covid-Const3";
    final String immBatch = "Lot-1233";
    final String immDate = "1978-04-23";
    final Vaccination dut = new Vaccination(
        vacManu, vacName, immBatch, immDate
    );
    assertSame(immBatch, dut.getBatch());                 // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
  } // end method */


  /**
   * Test method for {@link Vaccination#getDate()}.
   */
  @Test
  void getDate() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String vacManu = "Johnson Getter4";
    final String vacName = "Covid-Const4";
    final String immBatch = "Lot-1234";
    final String immDate = "1978-04-24";
    final Vaccination dut = new Vaccination(
        vacManu, vacName, immBatch, immDate
    );
    assertEquals(LocalDate.of(1978, 4, 24), dut.getDate());
  } // end method */

  /**
   * Test method for {@link Vaccination#getVaccine()}.
   */
  @Test
  void getVaccine() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String vacManu = "Johnson Getter5";
    final String vacName = "Covid-Const5";
    final String immBatch = "Lot-1253";
    final String immDate = "1978-04-25";
    final Vaccination dut = new Vaccination(
        vacManu, vacName, immBatch, immDate
    );
    assertSame(vacManu, dut.getVaccine().getManufacturer()); //        ES_COMPARING_STRINGS_WITH_EQ
    assertSame(vacName, dut.getVaccine().getName());      // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
  } // end method */

  /**
   * Test method for {@link Vaccination#toString()}.
   */
  @Test
  void test_toString() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String vacManu = "Johnson String";
    final String vacName = "Covid-String";
    final String immBatch = "Lot-123String";
    final String immDate = "1978-04-27";
    final Vaccination dut = new Vaccination(
        vacManu, vacName, immBatch, immDate
    );
    assertEquals(
        "Vaccination with Vaccine from Johnson String:"
            + " Covid-String on 1978-04-27, batch: Lot-123String",
        dut.toString()
    );
  } // end method */
} // end class
