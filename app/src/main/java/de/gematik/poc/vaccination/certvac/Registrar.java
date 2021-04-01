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
 * List of known registrars.
 *
 * <p>Registrars are responsible to define an encoding for artifacts for
 * serialization. Each version of an encoding is identified by the identifier
 * of its registrar plus a version number assigned by the registrar.
 *
 * <p>Here the telephone country codes are used as identifiers of registrars.
 * Reason:
 * <ol>
 *   <li>Well defined,
 *   <li>unique,
 *   <li>small numbers which could be easily encoded in
 *       <a href="https://www.rfc-editor.org/rfc/rfc8949.html">CBOR</a>.
 * </ol>
 */
public enum Registrar {
  /**
   * Germany.
   */
  GERMANY(49), // */
  ;

  /**
   * Identifier of a registrar.
   */
  private final int insIdentifier;

  /**
   * Comfort constructor.
   *
   * @param identifier of registrar
   */
  Registrar(
      final int identifier
  ) {
    if (identifier < 0) {
      throw new IllegalArgumentException("negative values prohibited");
    } // end if
    // ... identifier is non-negative

    if (0 == (identifier & 0x1)) {
      // ... even identifier
      insIdentifier = identifier >> 1;
    } else {
      // ... odd identifier
      insIdentifier = (-1 - identifier) >> 1;
    } // end else
  } // end constructor */

  /**
   * Return identifier of registrar.
   *
   * @return identifier of registrar
   */
  public int getIdentifier() {
    return insIdentifier;
  } // end method */

  /**
   * Pseudo constructor.
   *
   * @param identifier corresponding to a disease
   *
   * @throws NoSuchElementException if there is no corresponding disease to
   *                                {@code identifier}
   */
  public static Registrar getInstance(
      final int identifier
  ) {
    return Arrays.stream(Registrar.values())
        .filter(registrar -> registrar.insIdentifier == identifier)
        .findAny()
        .get();
  } // end method */
} // end enum
