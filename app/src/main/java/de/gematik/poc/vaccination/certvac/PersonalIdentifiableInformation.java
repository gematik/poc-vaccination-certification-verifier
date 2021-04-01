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

import com.gmail.alfred65fiedler.tlv.BerTlv;
import com.gmail.alfred65fiedler.tlv.DerDate;
import com.gmail.alfred65fiedler.tlv.DerInteger;
import com.gmail.alfred65fiedler.tlv.DerSequence;
import com.gmail.alfred65fiedler.tlv.DerUtf8String;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * This class collects information about an individual.
 *
 * <p>The intention is to use this information in a
 * {@link InformationOfVaccination} such that the
 * information about a vaccination is bound to that
 * person.
 *
 * <p>This class provides methods to access the information as
 * well as export and import it to and from various formats.
 */
public final class PersonalIdentifiableInformation {
  /**
   * Day of birth.
   */
  private final LocalDate insDayOfBirth; // */

  /**
   * Email-address of person vaccinated.
   *
   * <p><i><b>Note:</b> Possibly this information is not very useful.
   *                   This attribute is added here for the following
   *                   reason:
   *                   Such kind of information might appear in a
   *                   {@link InformationOfVaccination}, but such
   *                   information does not appear in a
   *                   {@link InformationOfProof}. All in all we now
   *                   have at least one attribute which is relevant
   *                   in the former, but not in the latter.</i>
   */
  private final String insEmailAddress; // */

  /**
   * Name of person vaccinated.
   *
   * <p>Intentionally this information is NOT split into given-name, middle-name
   * surname and alike. In Europe and countries influenced by European culture
   * typically the name of an individual consists of such things. But even so
   * things could get quite complex
   * <ol>
   *   <li>more than one given name,
   *   <li>more than one surname,
   *   <li>title,
   *   <li>etc.
   * </ol>
   *
   * <p>In order to simplify things an individual getting vaccinated just declares
   * his or her name in a way he or she is comfortable with.
   *
   * <p>It seems appropriate to allow any {@link StandardCharsets#UTF_8} character to be used.
   */
  private final String insName; // */

  /**
   * Comfort constructor.
   *
   * @param name         of an individual,
   *                     arbitrary sequence of {@link StandardCharsets#UTF_8}
   * @param dayOfBirth   of individual
   * @param emailAddress address of individual,
   *                     arbitrary sequence of {@link StandardCharsets#UTF_8}
   */
  /* package */ PersonalIdentifiableInformation(
      final String    name,
      final LocalDate dayOfBirth,
      final String    emailAddress
  ) {
    insName         = name;
    insDayOfBirth   = dayOfBirth;
    insEmailAddress = emailAddress;
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param name         of an individual,
   *                     arbitrary sequence of {@link StandardCharsets#UTF_8}
   * @param dayOfBirth   formatted according to
   *                     {@link java.time.format.DateTimeFormatter#ISO_LOCAL_DATE},
   *                     i.e. {@code }"YYYY-MM-DD"}
   * @param emailAddress address of individual,
   *                     arbitrary sequence of {@link StandardCharsets#UTF_8}
   */
  public PersonalIdentifiableInformation(
      final String name,
      final String dayOfBirth,
      final String emailAddress
  ) {
    this(name, LocalDate.parse(dayOfBirth), emailAddress);
  } // end constructor */

  /**
   * Decode.
   *
   * <p>Pseudo-constructor, inverse-operation to {@link #encode()}.
   *
   * <p>This method takes a {@link DerSequence} and elements in
   * order to construct an instance of this class.
   *
   * <p>The first element contains a version number. Version zero is used during
   * the development phase (and for experiments). The value of the version number
   * specifies how the input is decoded:
   * <ol>
   *   <li>{@code version == 1}<pre>
   *       {@code
   *       PersonalIdentifiableInformation :== SEQUENCE {<br>
   *           version      INTEGER (1),<br>
   *           name         UTF8String,<br>
   *           dayOfBirth   DATE,<br>
   *           emailAddress UTF8String<br>
   *       }}
   *       </pre>
   * </ol>
   *
   * @param sequence from which an instance is constructed
   *
   * @return corresponding instance
   *
   * @throws ArithmeticException       if version number in the value-field
   *                                   exceeds range of {@link Integer}
   * @throws ClassCastException        if the value-field of {@code sequence}
   *                                   is not in accordance to this specification
   * @throws IllegalArgumentException  if version is not (yet) implemented
   * @throws IndexOutOfBoundsException if the value-field of {@code sequence}
   *                                   contains less than four elements
   */
  public static PersonalIdentifiableInformation decode(
      final DerSequence sequence
  ) {
    int index = 0;
    final List<BerTlv> valueField = sequence.getDecoded();
    final int version = ((DerInteger) valueField.get(index)).getDecoded().intValueExact();

    final String    name;
    final LocalDate dayOfBirth;
    final String    emailAddress;
    switch (version) { // NOPMD switch with less than 3 branches
      case 1: {
        name         = ((DerUtf8String) valueField.get(++index)).getDecoded();
        dayOfBirth   = ((DerDate)       valueField.get(++index)).getDecoded();
        emailAddress = ((DerUtf8String) valueField.get(++index)).getDecoded();
      } break; // end version = 1

      default: {
        throw new IllegalArgumentException("unknown version: " + version);
      } // end default
    } // end switch (version)

    return new PersonalIdentifiableInformation(name, dayOfBirth, emailAddress);
  } // end method */

  /**
   * Encode.
   *
   * <p>This method encodes an instance of this class such that it could be
   * stored or transferred in a generalized, program independent way. This
   * is kind of a serialization, which is e.g. necessary for signing artifacts
   * containing instances of this class. This is the inverse-operation to
   * {@link #decode(DerSequence)}.
   *
   * <p>This version of codes encodes according to {@code version INTEGER (1)}.
   *
   * @return instance encoded in DER (distinguished encoding rules)
   */
  public DerSequence encode() {
    return new DerSequence(List.of(
        new DerInteger(BigInteger.valueOf(1)),
        new DerUtf8String(getName()),
        new DerDate(getDayOfBirth()),
        new DerUtf8String(getEmailAddress())
    ));
  } // end method */

  /**
   * Returns email-address.
   *
   * @return email-address
   */
  public String getEmailAddress() {
    return insEmailAddress;
  } // end method */

  /**
   * Returns the day of birth.
   *
   * @return day of birth
   */
  public LocalDate getDayOfBirth() {
    return insDayOfBirth;
  } // end method */

  /**
   * Returns the name of an individual.
   *
   * @return name of an individual
   */
  public String getName() {
    return insName;
  } // end method */

  /**
   * Returns {@link String} representation of instance attributes.
   *
   * @return {@link String} representation of instance attributes
   *
   * @see Object#toString()
   */
  @Override
  public String toString() {
    return String.format(
        "Person: %s, born on %s, email: %s",
        getName(),
        getDayOfBirth(),
        getEmailAddress()
    );
  } // end method */
} // end class
