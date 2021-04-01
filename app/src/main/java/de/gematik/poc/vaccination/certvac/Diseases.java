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

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * List of known diseases.
 *
 * <p>The enumeration is used to save memory space during serialization
 * of medical certificates.
 */
public enum Diseases {
  /**
   * General item for Covid-19.
   */
  COVID_19(0, "Covid-19"), // */

  /**
   * Mutant of Covid-19, here british mutant.
   */
  COVID_19_B117(1, COVID_19.insFullName + ", B1.1.7"), // */

  /**
   * Hepatitis B.
   */
  HEPATITIS_A(-1, "Hepatitis B"), // */

  /**
   * Hepatitis B.
   */
  HEPATITIS_B(2, "Hepatitis B"), // */

  /**
   * Hepatitis B.
   */
  HEPATITIS_C(-2, "Hepatitis C"), // */
  ;

  /**
   * Value representing a disease in serialized form.
   */
  private final int insEncodedValue; // */

  /**
   * Human readable representation of disease.
   */
  private final String insFullName; // */

  /**
   * Comfort constructor.
   *
   * @param encodedValue used for serialization
   * @param fullName     used for human readable texts
   */
  Diseases(
      final int encodedValue,
      final String fullName
  ) {
    insEncodedValue = encodedValue;
    insFullName = fullName;
  } // end constructor */

  /**
   * Pseudo constructor.
   *
   * @param encodedValue corresponding to a disease
   *
   * @throws NoSuchElementException if there is no corresponding disease to
   *                                {@code encodedValue}
   */
  public static Diseases getInstance(
      final int encodedValue
  ) {
    return Arrays.stream(Diseases.values())
        .filter(disease -> disease.getEncodedValue() == encodedValue)
        .findAny()
        .get();
  } // end method */

  /**
   * Return value used for encoding.
   *
   * @return encoded value
   */
  public int getEncodedValue() {
    return insEncodedValue;
  } // end method */

  /**
   * Returns name of disease.
   *
   * @return name
   */
  public String getFullName() {
    return insFullName;
  } // end method */

  @Override
  public String toString() {
    return getFullName();
  } // end method */
} // end enum
