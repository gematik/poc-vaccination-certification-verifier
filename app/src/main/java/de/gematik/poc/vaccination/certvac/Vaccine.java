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
import com.gmail.alfred65fiedler.tlv.DerInteger;
import com.gmail.alfred65fiedler.tlv.DerSequence;
import com.gmail.alfred65fiedler.tlv.DerUtf8String;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * This class collects information about a (possible multivalent) vaccine.
 *
 * <p>The intention is to use this information in a
 * {@link InformationOfVaccination} such that it
 * known which vaccine was used during immunization.
 *
 * <p>This class provides methods to access the information as
 * well as export and import it to and from various formats.
 */
public final class Vaccine {
  /**
   * Manufacturer.
   */
  private final String insManufacturer; // */

  /**
   * Name of vaccine.
   *
   * <p>It seems appropriate to allow any
   * {@link StandardCharsets#UTF_8}
   * character to be used.
   */
  private final String insName; // */

  /**
   * Comfort constructor.
   *
   * @param manufacturer which produced the vaccine
   * @param name         of vaccine
   */
  public Vaccine(
      final String manufacturer,
      final String name
  ) {
    insManufacturer = manufacturer;
    insName         = name;
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
   *      {@code
   *      Vaccine :== SEQUENCE {<br>
   *          version      INTEGER (1),<br>
   *          manufacturer UTF8String,<br>
   *          name         UTF8String<br>
   *      }}
   *      </pre>
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
   *                                   contains less than three elements
   */
  public static Vaccine decode(
      final DerSequence sequence
  ) {
    int index = 0;
    final List<BerTlv> valueField = sequence.getDecoded();
    final int version = ((DerInteger) valueField.get(index)).getDecoded().intValueExact();

    final String manu;
    final String name;
    switch (version) { // NOPMD switch with less than 3 branches
      case 1: {
        manu = ((DerUtf8String) valueField.get(++index)).getDecoded();
        name = ((DerUtf8String) valueField.get(++index)).getDecoded();
      } break; // end version = 1

      default: {
        throw new IllegalArgumentException("unknown version: " + version);
      } // end default
    } // end switch (version)

    return new Vaccine(manu, name);
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
        new DerUtf8String(getManufacturer()),
        new DerUtf8String(getName())
    ));
  } // end method */

  /**
   * Returns name of manufacturer of vaccine.
   *
   * @return name of manufacturer of vaccine
   */
  public String getManufacturer() {
    return insManufacturer;
  } // end method */

  /**
   * Name of vaccine.
   *
   * @return name of vaccine
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
    return String.format("Vaccine from %s: %s", getManufacturer(), getName());
  } // end method */
} // end class
