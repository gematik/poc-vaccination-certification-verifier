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

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.MapBuilder;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Number;
import co.nstant.in.cbor.model.UnicodeString;
import com.gmail.alfred65fiedler.utils.Hex;
import de.gematik.poc.vaccination.userinterface.CmdLine;
import de.gematik.poc.vaccination.utils.Utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * This class collects information for a certificate of vaccination.
 *
 * <p>This class provides methods to access the information as
 * well as export and import it to and from various formats.
 */
// Note 1: Spotbugs claims "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
//         Short message: Unchecked/unconfirmed cast of return value from method
//         That finding is suppressed because casting is intentional.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
    "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE" // see note 1
}) // */
public final class InformationOfProof {
  /**
   * Day of birth.
   */
  private final LocalDate insDayOfBirth; // */

  /**
   * Date and time this information expires.
   */
  private final ZonedDateTime insExpirationDate; // */

  /**
   * Mapping of {@link Diseases} to a corresponding {@link HealthStatus}.
   */
  private final Map<Diseases, HealthStatus> insHealthStatusMap; // */

  /**
   * Name of person vaccinated.
   *
   * <p>This is derived from {@link PersonalIdentifiableInformation#getName()}.
   */
  private final String insName; // */

  /**
   * Comfort constructor.
   *
   * @param name            of an individual to whom the {@link InformationOfProof} belongs
   * @param dayOfBirth      of individual
   * @param expirationDate  of information provided in {@code healthStatusMap}
   * @param healthStatusMap with information about a number of {@link Diseases}
   *                        and the {@link HealthStatus} of the individual
   *                        considering the corresponding disease.
   */
  public InformationOfProof(
      final String name,
      final LocalDate dayOfBirth,
      final ZonedDateTime expirationDate,
      final Map<Diseases, HealthStatus> healthStatusMap
  ) {
    insName = name;
    insDayOfBirth = dayOfBirth;
    insExpirationDate = expirationDate;
    insHealthStatusMap = healthStatusMap;
  } // end constructor */

  /**
   * Creates information of proof from arguments.
   *
   * <p>Currently this method is used by a shell-script and used to create
   * an {@link InformationOfProof}. That instance is then written in
   * {@link #encode()} format to the appropriate output-directory for later
   * use (typically in a signing process).
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>element is a prefix for a file name used to store
   *                        the information in files-system
   *                    <li>element contains name of individual to whom the
   *                        {@link InformationOfProof} belongs
   *                    <li>element contains day of birth of the vaccinated
   *                         individual, format {@code "YYYY-MM-DD"}
   *                    <li>element contains the expiration date of this proof,
   *                        format {@code "YYYY-MM-DDThh:mm:ss+HH:MM[TimeZone"]},
   *                        e.g. "2021-03-28T18:47:59+00:00[Z]" where
   *                        {@code "YYYY-MM-DD"} encodes a date,
   *                        {@code "Thh:mm:ss"} encodes a time,
   *                        {@code "+HH:MM"} or {@code "-HH:MM"} encodes a time
   *                        zone offset and
   *                        {@code "[TimeZone]"} encodes a time zone,
   *                        e.g. {@code "[Z]"} for {@link ZoneId} UTC.
   *                    <li>one or more triple of integer, the first encodes a
   *                        {@link Diseases}, the second and third a
   *                        {@link HealthStatus}
   *                  </ol>
   */
  public static void create(
      final ConcurrentLinkedQueue<String> arguments
  ) throws
      CborException,
      IOException {
    if (arguments.size() < 7) { // NOPMD literal in conditional statement
      // ... too few arguments
      CmdLine.LOGGER.atInfo().log("too few arguments");
      showUsage();

      return;
    } // end if
    // ... enough arguments

    if (0 != ((arguments.size() - 7) % 3)) {
      // ... too few arguments
      CmdLine.LOGGER.atInfo().log("incomplete triples");
      showUsage();

      return;
    } // end if
    // ... enough arguments

    final String prefix = arguments.remove();
    CmdLine.LOGGER.atInfo().log("start: createInfoOfProof for prefix=\"{}\"", prefix);

    // --- get name, dayOfBirth and expirationDate
    final String name = arguments.remove();
    CmdLine.LOGGER.atInfo().log("name          : \"{}\"", name);
    final String dayOfBirth = arguments.remove();
    CmdLine.LOGGER.atInfo().log("dayOfBirth    : \"{}\"", dayOfBirth);
    final String expirationDate = arguments.remove();
    CmdLine.LOGGER.atInfo().log("expirationDate: \"{}\"", expirationDate);

    // --- get information for insHealtStatusMap
    final Map<Diseases, HealthStatus> healthStatusMap = new ConcurrentHashMap<>();
    while (!arguments.isEmpty()) {
      final Diseases disease = Diseases.getInstance(Integer.parseInt(arguments.remove()));
      final int shieldStrength = Integer.parseInt(arguments.remove());
      final int harmlessness = Integer.parseInt(arguments.remove());
      final HealthStatus healthStatus = new HealthStatus(// NOPMD new in loop
          shieldStrength,
          harmlessness
      );
      healthStatusMap.put(disease, healthStatus);
    } // end while (still elements are available)

    final InformationOfProof informationOfProof = new InformationOfProof(
        name,                                // name
        LocalDate.parse(dayOfBirth),         // day of birth
        ZonedDateTime.parse(expirationDate), // expirationDate
        healthStatusMap
    );
    CmdLine.LOGGER.atDebug().log("InfOProof: {}", informationOfProof);

    // --- store information in filesystem
    final byte[] content = informationOfProof.encode();
    Files.write(
        Utils.PATH_UC20.resolve(prefix + "_cbor.bin"),
        content
    );
    Files.write(
        Utils.PATH_UC20.resolve(prefix + "_cbor-hexdigits.txt"),
        Hex.toHexDigits(content).getBytes(StandardCharsets.UTF_8)
    );
    Files.write(
        Utils.PATH_UC20.resolve(prefix + ".txt"),
        informationOfProof.toString().getBytes(StandardCharsets.UTF_8)
    );

    CmdLine.LOGGER.atInfo().log("end  : createInfoOfProof for prefix=\"{}\"", prefix);
  } // end method */

  private static void showUsage() {
    final String newLine = System.lineSeparator();

    CmdLine.LOGGER.atInfo().log(
        List.of(// list with parameter explanation
            "prefix        : prefix of file-name used to store information",
            "name          : name of individual being inoculated",
            "dayOfBirth    : of individual in format \"YYYY-MM-DD\"",
            "expirationDate: of proof, e.g. \"2021-03-28T18:47:59+00:00[Z]\"",
            "disease1      : encoded version of 1st disease",
            "shield1       : encoded version of 1st shield value",
            "harmlessness1 : encoded version of 1st harmlessness value",
            "...           : optionally more triplets of disease.n -> shield.n || harmlessness.n"
        ).stream()
            .collect(Collectors.joining(
                newLine + "  ",                            // delimiter
                newLine + newLine + "Usage: "              // start prefix with usage description
                    + CmdLine.ACTION_INFOPROOF_CREATE      // action followed by parameter list
                    + " prefix name dayOfBirth expirationDate"
                    + " disease1 shield1  harmlessness1 ..."
                    + newLine + "  ",                     // end prefix
                newLine + newLine                         // suffix
            ))
    );
  } // end method */

  /**
   * Decode.
   *
   * <p>Pseudo-constructor, inverse-operation to {@link #encode()}.
   *
   * <p>This method takes a given {@link List} and construct an instance of
   * this class according to the given version indication.
   *
   * <p>Together {@code registrar} and {@code version} specify how the
   * {@code items} is decodes:
   * <ul>
   *   <li><b>{@link Registrar#GERMANY}</b>:
   *       <ul>
   *         <li><b>{@code version = -1}</b>: The following
   *             <a href="https://www.rfc-editor.org/rfc/rfc8949.html">CBOR</a>
   *             artifacts are expected in {@code items}:
   *             <ol>
   *               <li>CBOR major type 0 or 1 (i.e. an unsigned or negative
   *                   integer, encoding {@link Registrar#GERMANY}
   *               <li>CBOR major type 0 or 1 (i.e. an unsigned or negative
   *                   integer, encoding {@code version = -1}
   *               <li>CBOR major type 3 (i.e. UTF-8 text) encoding a name
   *               <li>CBOR major type 0 or 1 (i.e. an unsigned or negative
   *                   integer, encoding the number of days the day of birth
   *                   differs from {@link LocalDate#EPOCH}
   *               <li>CBOR major type 0 or 1 (i.e. an unsigned or negative
   *                   integer, encoding the expiration date as the number of
   *                   seconds since the EPOCH
   *               <li>CBOR major type 5 (i.e. map) mapping keys to values.
   *                   Each key is {@link Diseases#getEncodedValue()} encoded
   *                   as a CBOR integer (unsigned or negative).
   *                   Each value is {@link HealthStatus#encode()} encoded as
   *                   CBOR integer (unsigned or negative).
   *             </ol>
   *       </ul>
   * </ul>
   *
   * @param content from which an instance is constructed
   *
   * @return corresponding instance
   *
   * @throws ArithmeticException      if {@code registrar} or {@code version}
   *                                  exceeds range of {@link Integer}
   * @throws NoSuchElementException   if less than expected elements are
   *                                  available in {@code dataItem}
   * @throws ClassCastException       if any {@link DataItem} in {@code dataItem}
   *                                  has an unexpected {@link Class}
   * @throws IllegalArgumentException if
   *                                  <ol>
   *                                    <li>registrar is not (yet) implemented
   *                                    <li>version is not (yet) implemented
   *                                    <li>an underlying constructor does so
   *                                  </ol>
   */
  public static InformationOfProof decode(
      final byte[] content
  ) throws CborException {
    // --- create an iterator for CBOR data items in content
    final Iterator<DataItem> iterator = CborDecoder.decode(content).iterator();

    // --- retrieve registrar and version number from message
    final Iterator<Object> itVersion = Checker.extractVersionNumber(iterator).iterator();
    final Registrar registrar = (Registrar) itVersion.next();
    final int version = (Integer) itVersion.next();

    switch (registrar) {
      case GERMANY: {
        switch (version) { // NOPMD too few branches
          case -1:
            final String name = ((UnicodeString) iterator.next()).getString();
            final LocalDate dayOfBirth = decodeDayOfBirth(
                (Number) iterator.next() // spotbugs: BC_UNCONFIRMED_CAST_OF_RETURN_VALUE
            );
            final ZonedDateTime expirationDate = decodeExpirationDate(((Number) iterator.next()));
            final co.nstant.in.cbor.model.Map map = (co.nstant.in.cbor.model.Map) iterator
                // spotbugs: BC_UNCONFIRMED_CAST_OF_RETURN_VALUE
                .next();
            final Map<Diseases, HealthStatus> healthStatusMap = new ConcurrentHashMap<>();
            map.getKeys().forEach(key -> healthStatusMap.put(
                Diseases.getInstance(((Number) key).getValue().intValueExact()),
                HealthStatus.decode(registrar, version, map.get(key))
            ));

            return new InformationOfProof(
              name, dayOfBirth, expirationDate, healthStatusMap
            );
            // end version == -1

          default:
            throw new IllegalArgumentException("unknown version: " + version);
        } // end switch (version)
      } // end Germany

      default:
        throw new IllegalArgumentException("unknown registrar: " + registrar);
    } // end switch (registrar)
  } // end method */

  /**
   * Encode.
   *
   * <p>This method encodes an instance of this class such that it could be
   * stored or transferred in a generalized, program independent way. This
   * is kind of a serialization, which is e.g. necessary for signing artifacts
   * containing instances of this class. This is the inverse-operation to
   * {@link #decode(byte[])}.
   *
   * <p>This version of codes encodes according to {@code version = (GERMANY, -1)}.
   *
   * @return encoded instance
   */
  public byte[] encode() throws CborException {
    final int version = -1;

    final MapBuilder<CborBuilder> mapBuilder = new CborBuilder()
        .add(Registrar.GERMANY.getIdentifier()) // add registrar
        .add(version)                           // version
        .add(getName())                         // name
        .add(encodeDayOfBirth())                // dayOfBirth
        .add(encodeExpirationDate())            // expirationDate
        .addMap();                              // add map

    getHealthStatusMap().forEach((diseases, healthStatus) -> mapBuilder.put(
        diseases.getEncodedValue(),
        healthStatus.encode())
    ); // end forEach((diseases, healthStatus) -> ...)

    final ByteArrayOutputStream result = new ByteArrayOutputStream();
    new CborEncoder(result).encode(
        mapBuilder
        .end()   // end map
        .build() // get all items
    );

    return result.toByteArray();
  } // end method */

  /**
   * Decodes day of birth.
   *
   * <p>This is kind of the inverse operation to {@link #encodeDayOfBirth()}.
   *
   * @param number difference between {@link LocalDate#EPOCH} and day of birth
   *
   * @return decoded value, i.e. {@link LocalDate#EPOCH} plus {@link LocalDate#plusDays(long)},
   *         where the summand is taken from {@code number}, i.e. {@link Number#getValue()}
   *
   * @throws ArithmeticException if the value encoded in {@code number}
   *                             exceeds range of {@link Long}
   */
  private static LocalDate decodeDayOfBirth(
      final Number number
  ) {
    return  LocalDate.EPOCH.plusDays(number.getValue().longValueExact());
  } // end method */

  /**
   * Decodes expiration date.
   *
   * <p>This is kind of the inverse operation to {@link #encodeExpirationDate()}.
   *
   * @param number with amount of seconds since the EPOCH for expiration date
   *
   * @return decoded value
   *
   * @throws ArithmeticException if the value encoded in {@code number}
   *                             exceeds range of {@link Long}
   */
  private static ZonedDateTime decodeExpirationDate(
      final Number number
  ) {
    return ZonedDateTime.ofInstant(
        Instant.ofEpochSecond(number.getValue().longValueExact()),
        CmdLine.TIME_ZONE
    );
  } // end method */

  /**
   * Returns encoded value for day of birth.
   *
   * <p>This is kind of the inverse operation to {@link #decodeDayOfBirth(Number)}.
   *
   * @return difference between {@link LocalDate#EPOCH} and day of birth
   */
  private long encodeDayOfBirth() {
    return ChronoUnit.DAYS.between(LocalDate.EPOCH, getDayOfBirth());
  } // end method */

  /**
   * Returns encoded value for expiration date.
   *
   * <p>This is kind of the inverse operation to {@link #decodeExpirationDate(Number)}.
   *
   * @return number of seconds since the EPOCH
   */
  private long encodeExpirationDate() {
    return getExpirationDate().toEpochSecond();
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
   * Returns expiration date of this information.
   *
   * @return expiration date
   */
  public ZonedDateTime getExpirationDate() {
    return insExpirationDate;
  } // end method */

  /**
   * Return mapping of {@link Diseases} to the corresponding {@link HealthStatus}.
   *
   * @return mapping of {@link Diseases} to {@link HealthStatus}
   */
  public Map<Diseases, HealthStatus> getHealthStatusMap() {
    return insHealthStatusMap;
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
        "%s, born on %s. Expiration date of this information is %s, info: %s",
        getName(), getDayOfBirth(), getExpirationDate(), getHealthStatusMap()
    );
  } // end method */
} // end class
