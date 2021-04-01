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

import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Number;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Aggregation of health related information.
 *
 * <p>Currently this class aggregates the following information:
 * <ol>
 *   <li><b>value of harmlessness:</b> This piece of information tells how
 *       harmless an individual is to others people. Typically an individual
 *       is harmless (in respect to a certain disease) if it can not spread
 *       that disease (e.g. proofed with a negative test result).
 *   <li><b>strength of shield:</b> This piece of information tells how well
 *       an individual is protected against a certain disease. Typically an
 *       individual is well protected after vaccination.
 * </ol>
 *
 * <p>From the perspective of this class
 * <ol>
 *   <li>Instances are immutable value-types. Thus
 *       {@link Object#equals(Object) equals()},
 *       {@link Object#hashCode() hashCode()} are overwritten, but
 *       {@link Object#clone() clone()} isn't overwritten.
 *   <li>where data is passed in or out, defensive cloning is performed,
 *       if applicable.
 *   <li>methods are thread-safe.
 * </ol>
 *
 * <p>It follows that from the perspective of this class object sharing is
 * possible without side-effects.
 *
 */
// Note 1: Spotbugs claims "BC_UNCONFIRMED_CAST",
//         Short message: Unchecked/unconfirmed cast of return value from method
//         That finding is suppressed because casting is intentional.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
    "BC_UNCONFIRMED_CAST" // see note 1
}) // */
public final class HealthStatus {
  /**
   * Supremum of value for harmlessness.
   *
   * <p>For more explanation, see {@link #getHarmlessness()}.
   */
  public static final int MAX_HARMLESSNESS = 5; // */

  /**
   * Supremum of shield strength.
   *
   * <p>For more explanation, see {@link #getShieldStrength()}.
   */
  public static final int MAX_SHIELD_STRENGTH = 7; // */

  /**
   * Offset used for encoding.
   *
   * <p><i><b>Note:</b> Due to this offset it is possible to encode 48 different
   *               values in just one octet.</i>
   */
  /* package */ static final int OFFSET = 24; // */

  /**
   * Value of harmlessness, see {@link #getHarmlessness()}.
   */
  private final int insHarmlessness; // */

  /**
   * Cash the hash code.
   *
   * <p><i><b>Notes:</b></i>
   * <ol>
   *   <li><i>Because only immutable instance attributes of this class are taken
   *          into account for {@link #hashCode()} lazy initialization is possible.
   *       </i>
   *   <li><i>Intentionally this instance attribute is neither final
   *          (because of lazy initialization) nor synchronized
   *          (in order to avoid synchronization overhead).
   *       </i>
   * </ol>
   */
  private transient int insHashCode; // */

  /**
   * Level of protection, see {@link #getShieldStrength()}.
   */
  private final int insShieldStrength; // */

  /**
   * Comfort constructor.
   *
   * @param shieldStrength an integer indicating the strength of vaccination,
   *                       only the two least significant bit are taken into account
   * @param harmlessness   an integer indicating how harmless the individual is,
   *                       only the two least significant bit are taken into account
   *
   * @throws IllegalArgumentException if
   *                                  <ol>
   *                                    <li>{@code shieldStrength} is not in range
   *                                        {@code [0,} {@link #MAX_SHIELD_STRENGTH}{@code ]}
   *                                    <li>{@code harmlessness} is not in range
   *                                        {@code [0,} {@link #MAX_HARMLESSNESS}{@code ]}
   *                                  </ol>
   */
  public HealthStatus(
      final int shieldStrength,
      final int harmlessness
  ) {
    if ((0 > shieldStrength) || (shieldStrength > MAX_SHIELD_STRENGTH)) {
      throw new IllegalArgumentException(
          "shieldStrength out of range [0, " + MAX_SHIELD_STRENGTH + ']'
      );
    } // end if

    if ((0 > harmlessness) || (harmlessness > MAX_HARMLESSNESS)) {
      throw new IllegalArgumentException(
          "harmlessness out of range [0, " + MAX_HARMLESSNESS + ']'
      );
    } // end if

    // ... shieldStrength  AND  harmlessness in supported range

    insShieldStrength = shieldStrength;
    insHarmlessness = harmlessness;
  } // end constructor */

  /**
   * Decode.
   *
   * <p>Pseudo-constructor, inverse-operation to {@link #encode()}.
   *
   * <p>This method takes a given {@link DataItem} and construct an instance of
   * this class according to the given version indication.
   *
   * <p>Together {@code registrar} and {@code version} specify how the
   * {@code item} is decodes:
   * <ul>
   *   <li><b>{@link Registrar#GERMANY}</b>:
   *       <ul>
   *         <li><b>{@code version = -1}</b>: A {@link Number} in range {@code [-24, 23]}.
   *             A <a href="https://www.rfc-editor.org/rfc/rfc8949.html">CBOR</a>
   *             integer encoding all instance attributes in one octet as follows:<br>
   *             {@code integer = 8 * } {@link #getHarmlessness()}
   *             {@code + } {@link #getShieldStrength()} {@code - 24}.
   *       </ul>
   * </ul>
   *
   * @param registrar of version number
   * @param version   of encoded {@code item}
   * @param item      from which an instance is constructed
   *
   * @return corresponding instance
   *
   * @throws ArithmeticException       if {@link Number#getValue()} exceeds
   *                                   range of {@link Integer}
   * @throws ClassCastException        if {@code item} is not a {@link Number}
   * @throws IllegalArgumentException  if
   *                                   <ol>
   *                                     <li>registrar is not (yet) implemented
   *                                     <li>version is not (yet) implemented
   *                                     <li>an underlying constructor does so
   *                                   </ol>version is not (yet) implemented
   */
  public static HealthStatus decode(
      final Registrar registrar,
      final int version,
      final DataItem item
  ) {
    final int harmlessness;
    final int shieldStrength;
    switch (registrar) {
      case GERMANY: {
        switch (version) { // NOPMD too few branches
          case -1:
            final Number number = (Number) item; // spotbugs: BC_UNCONFIRMED_CAST
            final int value = number.getValue().intValueExact() + OFFSET;
            harmlessness = value >> 3;
            shieldStrength = value & 0x7;
            break;

          default:
            throw new IllegalArgumentException("unknown version: " + version);
        } // end switch (version)
      }
      break; // end Germany

      default:
        throw new IllegalArgumentException("unknown registrar: " + registrar);
    } // end switch (registrar)

    return new HealthStatus(shieldStrength, harmlessness);
  } // end method */

  /**
   * Encode.
   *
   * <p>This method encodes an instance of this class such that it could be
   * stored or transferred in a generalized, program independent way. This
   * is kind of a serialization, which is e.g. necessary for signing artifacts
   * containing instances of this class. This is the inverse-operation to
   * {@link #decode(Registrar, int, DataItem)}.
   *
   * <p>This version of codes encodes according to {@code version = (+49, -1)}.
   *
   * @return {@code cborBuilder} for fluent programming
   */
  public int encode() {
    return (getHarmlessness() << 3) + getShieldStrength() - OFFSET;
  } // end method */

  /**
   * Returns level of harmlessness.
   *
   * <p>This instance attribute states how harmless an individual is.
   * The higher the value, the more harmless. A low value indicates
   * that the individual is (potentially) a high threat to other people.
   * The higher the value, the less harmful.
   *
   * <p>Although, from a medical point of view this is a continuous attribute
   * (in Java-terms a {@link Double})  it seems sufficient to distinguish no more
   * than four different values, because there is (usually) some uncertainty in
   * measurement.
   *
   * <p>A value of zero means (almost) certain thread to other people,
   * the highest value means not harmful at all.
   * See {@link HealthStatus#HealthStatus(int, int)} for currently supported range.
   *
   * @return level of harmlessness
   */
  public int getHarmlessness() {
    return insHarmlessness;
  } // end method */

  /**
   * The implementation of this method fulfills the equals-contract.
   *
   * <p><i><b>Notes:</b></i>
   * <ol>
   *   <li><i>This method is thread-safe.
   *       </i>
   *   <li><i>Object sharing is not a problem here, because
   *          input parameter(s) are immutable
   *          and
   *          return value is primitive.
   *       </i>
   * </ol>
   *
   * @param obj object used for comparison, can be null
   *
   * @return true if objects are equal, false otherwise
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(
      final @CheckForNull Object obj
  ) {
    // --- reflexive
    if (this == obj) {
      return true;
    } // end if
    // ... obj not same as this

    if (null == obj) {
      // ... this differs from null
      return false;
    } // end if
    // ... obj not null

    // Note 1: Because this class is a direct sub-class of Object calling super.equals(...)
    //         would be wrong. Instead special checks are performed.

    if (getClass() != obj.getClass()) {
      // ... different classes
      return false;
    } // end if
    // ... obj is instance of this class

    final HealthStatus other = (HealthStatus) obj;

    // --- compare primitive instance attributes
    // --- compare reference types
    // ... assertion: instance attributes are never null
    return (this.insHarmlessness == other.insHarmlessness)
        && (this.insShieldStrength == other.insShieldStrength);
  } // end method */

  /**
   * The implementation of this method fulfills the hashCode-contract.
   *
   * <p><i><b>Notes:</b></i>
   * <ol>
   *   <li><i>This method is thread-safe.
   *       </i>
   *   <li><i>Object sharing is not a problem here, because
   *          return value is primitive.
   *       </i>
   * </ol>
   *
   * @return hash-code of object
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    // Note 1: Because this class is a direct sub-class of object
    //         calling super.hashCode(...) would be wrong.
    // Note 2: Because equals() takes into account just the encoded value
    //         we can do here the same.
    // Note 3: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    int result = insHashCode; // read insHashCode from main memory into thread local memory
    if (0 == result) {
      // ... obviously attribute hashCode has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      // --- take into account primitive instance attributes
      result = encode();

      // --- take into account reference types (currently none)
      // -/-

      insHashCode = result; // store insHashCode into thread local memory
    } // end if

    return result;
  } // end method */

  /**
   * Returns the level of protection.
   *
   * <p>This is kind of "strength of protection shield".
   * The higher the value, the better the protection. A low value indicates that
   * the individual has no protection at all. A high value indicates that a
   * certain disease is (almost) no thread at all.
   *
   * <p>Although, from a medical point of view this is a continuous attribute
   * (in Java-terms a {@link Double})  it seems sufficient to distinguish only a
   * small number of values, because there is (usually) some uncertainty in
   * measurement.
   *
   * <p>A value of zero means no protection,
   * the highest value means fully protected.
   * See {@link HealthStatus#HealthStatus(int, int)} for currently supported range.
   *
   * @return level of protection
   */
  public int getShieldStrength() {
    return insShieldStrength;
  } // end method */

  @Override
  public String toString() {
    return String.format("(shield=%d, harmlessness=%d)", getShieldStrength(), getHarmlessness());
  } // end method */
} // end class
