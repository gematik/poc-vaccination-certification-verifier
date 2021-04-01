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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * This class collects information about a vaccination procedure.
 */
public class Vaccination {
  /**
   * Vaccine used for immunization.
   */
  private final Vaccine insVaccine; // */

  /**
   * Batch information.
   */
  private final String insBatch; // */

  /**
   * Date and time of immunization.
   */
  private final LocalDate insDate; // */

  /**
   * Comfort constructor.
   *
   * @param vacManufacturer which produced the vaccine
   * @param vacName         of vaccine
   * @param batch           identification of ampulla used for immunization,
   *                        arbitrary {@link String}
   * @param date            of vaccination, format
   *                        {@link DateTimeFormatter#ISO_LOCAL_DATE}, e.g
   *                        {@code "2013-12-28"}
   */
  public Vaccination(
      final String vacManufacturer,
      final String vacName,
      final String batch,
      final String date
  ) {
    this(
        new Vaccine(vacManufacturer, vacName),
        batch,
        LocalDate.parse(date)
    );
  } // end method */

  /**
   * Comfort constructor.
   *
   * @param vaccine used during vaccination
   * @param batch   identification of ampulla used for immunization,
   *                arbitrary {@link String}
   * @param date    of vaccination
   */
  public Vaccination(
      final Vaccine   vaccine,
      final String    batch,
      final LocalDate date
  ) {
    insVaccine = vaccine;
    insBatch   = batch;
    insDate    = date;
  } // end method */

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
   *       Vaccination :== SEQUENCE {<br>
   *           version INTEGER (1),<br>
   *           vaccine Vaccine,<br>
   *           batch   UTF8String,<br>
   *           date    UTCTime<br>
   *       }}
   *       </pre>
   * </ol>
   *
   * <p>For specification of {@code Vaccine}, see {@link Vaccine#decode(DerSequence)}.
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
  public static Vaccination decode(
      final DerSequence sequence
  ) {
    int index = 0;
    final List<BerTlv> valueField = sequence.getDecoded();
    final int version = ((DerInteger) valueField.get(index)).getDecoded().intValueExact();

    final Vaccine   vaccine;
    final String    batch;
    final LocalDate date;
    switch (version) { // NOPMD switch with less than 3 branches
      case 1: {
        vaccine = Vaccine.decode((DerSequence) valueField.get(++index));
        batch   = ((DerUtf8String)             valueField.get(++index)).getDecoded();
        date    = ((DerDate)                   valueField.get(++index)).getDecoded();
      } break; // end version = 1

      default: {
        throw new IllegalArgumentException("unknown version: " + version);
      } // end default
    } // end switch (version)

    return new Vaccination(vaccine, batch, date);
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
        getVaccine().encode(),
        new DerUtf8String(getBatch()),
        new DerDate(getDate())
    ));
  } // end method */

  /**
   * Returns identification of ampulla used for immunization.
   *
   * @return identification of ampulla used for vaccination
   */
  public String getBatch() {
    return insBatch;
  } // end method */

  /**
   * Returns date of vaccination.
   *
   * @return {@link LocalDate} of vaccination
   */
  public LocalDate getDate() {
    return insDate;
  } // end method */

  /**
   * Returns information about the vaccine being used for immunization.
   *
   * @return {@link Vaccine} being used for immunization
   */
  public Vaccine getVaccine() {
    return insVaccine;
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
        "Vaccination with %s on %s, batch: %s",
        getVaccine(),
        getDate(),
        getBatch()
    );
  } // end method */
} // end class
