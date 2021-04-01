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

import com.gmail.alfred65fiedler.tlv.DerInteger;
import com.gmail.alfred65fiedler.tlv.DerOctetString;
import com.gmail.alfred65fiedler.tlv.DerPrintableString;
import com.gmail.alfred65fiedler.tlv.DerSequence;
import com.gmail.alfred65fiedler.tlv.DerUtf8String;
import java.math.BigInteger;
import java.util.List;
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
final class TestVaccine {
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
   * Test method for {@link Vaccine#Vaccine(String, String)}.
   */
  @Test
  void test__Vaccine__String_String() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String manu = "BioNar";
    final String name = "AntiCo";
    final Vaccine dut = new Vaccine(manu, name);
    assertSame(manu, dut.getManufacturer()); // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
    assertSame(name, dut.getName());         // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
  } // end method */

  /**
   * Test method for {@link Vaccine#decode(DerSequence)}.
   */
  @Test
  void test_decode__DerSequence() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR, ArithmeticException
    // --- c. ERROR, ClassCastException
    // --- d. ERROR, IllegalArgumentException
    // --- e. ERROR, IndexOutOfBoundsException

    // --- a. smoke test
    {
      final String manu = "BioDecode";
      final String name = "AntiDecode";
      final Vaccine dut = Vaccine.decode(
          new DerSequence(List.of(
              new DerInteger(BigInteger.valueOf(1)),
              new DerUtf8String(manu),
              new DerUtf8String(name)
          ))
      );

      assertEquals(manu, dut.getManufacturer());
      assertEquals(name, dut.getName());
    }

    // --- b. ERROR, ArithmeticException
    assertThrows(
        ArithmeticException.class,
        () -> Vaccine.decode(
            new DerSequence(List.of(
                new DerInteger(BigInteger.valueOf(Integer.MAX_VALUE + 1L))
            ))
        )
    );

    // --- c. ERROR, ClassCastException
    // c.0 wrong class for version
    // c.1 wrong class for manufacturer
    // c.2 wrong class for name
    // c.x everything is fine
    List.of(
        // c.0 wrong class for version
        new DerSequence(List.of(
            new DerOctetString(BigInteger.valueOf(1).toByteArray())
        )),

        // c.1 wrong class for name
        new DerSequence(List.of(
            new DerInteger(BigInteger.valueOf(1)),
            new DerPrintableString("NameC1")
        )),

        // c.2 wrong class for name
        new DerSequence(List.of(
            new DerInteger(BigInteger.valueOf(1)),
            new DerUtf8String("Moderis"),
            new DerPrintableString("Covid-17")
        ))
    ).forEach(tlv -> assertThrows(
        ClassCastException.class,
        () -> Vaccine.decode(tlv)
    )); // end forEach(tlv -> ...)

    // c.x everything is fine
    {
      assertDoesNotThrow(
          () -> Vaccine.decode(
              new DerSequence(List.of(
                  new DerInteger(BigInteger.valueOf(1)),
                  new DerUtf8String("Moderis"),
                  new DerUtf8String("Covid-17")
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
        () -> Vaccine.decode(
            new DerSequence(List.of(
                new DerInteger(BigInteger.valueOf(version))
            ))
        )
    )); // end forEach(version -> ...)

    // --- e. ERROR, IndexOutOfBoundsException
    // name missing
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> PersonalIdentifiableInformation.decode(
            new DerSequence(List.of(
                new DerInteger(BigInteger.valueOf(1)),
                new DerUtf8String("Manu")
            ))
        )
    );
  } // end method */

  /**
   * Test method for {@link Vaccine#encode()}.
   */
  @Test
  void test_encode() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    {
      final String manu = "BioNencode";
      final String name = "AntiEncode";
      final Vaccine dut = new Vaccine(manu, name);
      assertEquals(
          new DerSequence(List.of(
              new DerInteger(BigInteger.valueOf(1)),
              new DerUtf8String(manu),
              new DerUtf8String(name)
          )),
          dut.encode()
      );
    }
  } // end method */

  /**
   * Test method for {@link Vaccine#getManufacturer()}.
   */
  @Test
  void test_getManufacturer() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String manu = "BioManu";
    final String name = "AntiManu";
    final Vaccine dut = new Vaccine(manu, name);
    assertSame(manu, dut.getManufacturer()); // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
  } // end method */

  /**
   * Test method for {@link Vaccine#getName()}.
   */
  @Test
  void test_getName() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String manu = "BioName";
    final String name = "AntiName";
    final Vaccine dut = new Vaccine(manu, name);
    assertSame(name, dut.getName());         // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
  } // end method */

  /**
   * Test method for {@link Vaccine#toString()}.
   */
  @Test
  void test_toString() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String manu = "BioName";
    final String name = "AntiName";
    final Vaccine dut = new Vaccine(manu, name);
    assertEquals(
        "Vaccine from " + manu + ": " + name,
        dut.toString()
    );
  } // end method */
} // end class
