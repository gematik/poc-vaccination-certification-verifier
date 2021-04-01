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

import com.gmail.alfred65fiedler.tlv.DerDate;
import com.gmail.alfred65fiedler.tlv.DerInteger;
import com.gmail.alfred65fiedler.tlv.DerOctetString;
import com.gmail.alfred65fiedler.tlv.DerPrintableString;
import com.gmail.alfred65fiedler.tlv.DerSequence;
import com.gmail.alfred65fiedler.tlv.DerUtf8String;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
final class TestPersonalIdentifiableInformation {
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
   * Test method for {@code PersonalIdentifiableInformation#
   * PersonalIdentifiableInformation(String, LocalDate, String)}.
   */
  @Test
  void test__PersonalIdentifiableInformation__String_LocalDate_String() { // NOPMD '_' character
    // Test strategy:
    // --- a. smoke test
    final String    name  = "Foo Bar";
    final LocalDate date  = LocalDate.now();
    final String    email = "Foo.Bar@example.com";
    final PersonalIdentifiableInformation dut = new PersonalIdentifiableInformation(
        name, date, email
    );
    assertSame(name, dut.getName());          // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
    assertSame(date, dut.getDayOfBirth());
    assertSame(email, dut.getEmailAddress()); // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
  } // end method */

  /**
   * Test method for {@code PersonalIdentifiableInformation#
   * PersonalIdentifiableInformation(String, String, String)}.
   */
  @Test
  void test__PersonalIdentifiableInformation__String_String_String() { // NOPMD '_' character
    // Test strategy:
    // --- a. smoke test
    // --- b.
    final String    name  = "Foo Bar";
    final LocalDate date  = LocalDate.now();
    final String    email = "Foo.Bar@example.com";
    final PersonalIdentifiableInformation dut = new PersonalIdentifiableInformation(
        name, date.format(DateTimeFormatter.ISO_LOCAL_DATE), email
    );
    assertSame(name, dut.getName());          // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
    assertEquals(date, dut.getDayOfBirth());
    assertSame(email, dut.getEmailAddress()); // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
  } // end method */

  /**
   * Test method for {@link PersonalIdentifiableInformation#decode(DerSequence)}.
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
      final String name = "Foo Barty";
      final LocalDate date = LocalDate.now();
      final String email = "Foo.Barty@example.com";
      final PersonalIdentifiableInformation dut = PersonalIdentifiableInformation.decode(
          new DerSequence(List.of(
              new DerInteger(BigInteger.valueOf(1)),
              new DerUtf8String(name),
              new DerDate(date),
              new DerUtf8String(email)
          ))
      );

      assertEquals(name, dut.getName());
      assertEquals(date, dut.getDayOfBirth());
      assertEquals(email, dut.getEmailAddress());
    }

    // --- b. ERROR, ArithmeticException
    assertThrows(
        ArithmeticException.class,
        () -> PersonalIdentifiableInformation.decode(
            new DerSequence(List.of(
                new DerInteger(BigInteger.valueOf(Integer.MAX_VALUE + 1L))
            ))
        )
    );

    // --- c. ERROR, ClassCastException
    // c.0 wrong class for version
    // c.1 wrong class for name
    // c.2 wrong class for day of birth
    // c.3 wrong class for email address
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

        // c.2 wrong class for day of birth
        new DerSequence(List.of(
            new DerInteger(BigInteger.valueOf(1)),
            new DerUtf8String("NameC2"),
            new DerUtf8String("19650319")
        )),

        // c.3 wrong class for email address
        new DerSequence(List.of(
            new DerInteger(BigInteger.valueOf(1)),
            new DerUtf8String("NameC3"),
            new DerDate(LocalDate.now()),
            new DerPrintableString("email")
        ))
    ).forEach(tlv -> assertThrows(
        ClassCastException.class,
        () -> PersonalIdentifiableInformation.decode(tlv)
    )); // end forEach(tlv -> ...)

    // c.x everything is fine
    {
      assertDoesNotThrow(
          () -> PersonalIdentifiableInformation.decode(
              new DerSequence(List.of(
                  new DerInteger(BigInteger.valueOf(1)),
                  new DerUtf8String("NameCx"),
                  new DerDate(LocalDate.now()),
                  new DerUtf8String("emailx")
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
        () -> PersonalIdentifiableInformation.decode(
            new DerSequence(List.of(
                new DerInteger(BigInteger.valueOf(version))
            ))
        )
    )); // end forEach(version -> ...)

    // --- e. ERROR, IndexOutOfBoundsException
    // email-address is missing
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> PersonalIdentifiableInformation.decode(
            new DerSequence(List.of(
                new DerInteger(BigInteger.valueOf(1)),
                new DerUtf8String("NameE"),
                new DerDate(LocalDate.now())
            ))
        )
    );
  } // end method */

  /**
   * Test method for {@link PersonalIdentifiableInformation#encode()}.
   */
  @Test
  void test_encode() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    {
      final String    name  = "Foo Bartilus";
      final LocalDate date  = LocalDate.now();
      final String    email = "Foo.Bartilus@example.com";
      final PersonalIdentifiableInformation dut = new PersonalIdentifiableInformation(
          name, date, email
      );
      assertEquals(
          new DerSequence(List.of(
              new DerInteger(BigInteger.valueOf(1)),
              new DerUtf8String(name),
              new DerDate(date),
              new DerUtf8String(email)
          )),
          dut.encode()
      );
    }
  } // end method */

  /**
   * Test method for {@link PersonalIdentifiableInformation#getDayOfBirth()}.
   */
  @Test
  void test_getDayOfBirth() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String    name  = "Foo DayOfBirth";
    final LocalDate date  = LocalDate.now();
    final String    email = "Foo.Day@example.com";
    final PersonalIdentifiableInformation dut = new PersonalIdentifiableInformation(
        name, date, email
    );
    assertSame(date, dut.getDayOfBirth());
  } // end method */

  /**
   * Test method for {@link PersonalIdentifiableInformation#getEmailAddress()}.
   */
  @Test
  void test_getEmailAddress() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String    name  = "Foo Email";
    final LocalDate date  = LocalDate.now();
    final String    email = "Foo.Eamil@example.com";
    final PersonalIdentifiableInformation dut = new PersonalIdentifiableInformation(
        name, date, email
    );
    assertSame(email, dut.getEmailAddress()); // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
  } // end method */

  @Test
  void test_getName() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String    name  = "Foo Name";
    final LocalDate date  = LocalDate.now();
    final String    email = "Foo.Name@example.com";
    final PersonalIdentifiableInformation dut = new PersonalIdentifiableInformation(
        name, date, email
    );
    assertSame(name, dut.getName()); // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
  } // end method */

  @Test
  void test_toString() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. smoke test
    final String    name  = "Foo String";
    final LocalDate date  = LocalDate.of(1987, 6, 25);
    final String    email = "Foo.String@example.com";
    final PersonalIdentifiableInformation dut = new PersonalIdentifiableInformation(
        name, date, email
    );
    assertEquals(
        "Person: Foo String, born on 1987-06-25, email: Foo.String@example.com",
        dut.toString()
    );
  } // end method */
} // end class
