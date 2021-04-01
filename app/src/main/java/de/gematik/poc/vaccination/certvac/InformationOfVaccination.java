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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import de.gematik.poc.vaccination.userinterface.CmdLine;
import de.gematik.poc.vaccination.utils.Utils;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

/**
 * This class collects information for a certificate of vaccination.
 *
 * <p>This class provides methods to access the information as
 * well as export and import it to and from various formats.
 */
public final class InformationOfVaccination {
  /**
   * Person who has been vaccinated.
   */
  private final PersonalIdentifiableInformation insPii; // */

  /**
   * Vaccine used for immunization.
   */
  private final Vaccination insVaccination; // */

  /**
   * Comfort constructor.
   *
   * @param piiName         of an individual to whom the {@link InformationOfVaccination} belongs
   * @param piiDayOfBirth   of the vaccinated individual, format {@code "YYYY-MM-DD"}
   * @param piiEmailAddress of individual
   * @param vacManufacturer of vaccine
   * @param vacName         of vaccine
   * @param immDate         date of vaccination, format {@code "YYYY-MM-DD"}
   */
  /* package */ InformationOfVaccination(
      final String piiName,
      final String piiDayOfBirth,
      final String piiEmailAddress,
      final String vacManufacturer,
      final String vacName,
      final String immBatch,
      final String immDate
  ) {
    this(
        new PersonalIdentifiableInformation(piiName, piiDayOfBirth, piiEmailAddress),
        new Vaccination(
            new Vaccine(vacManufacturer, vacName),
            immBatch,
            LocalDate.parse(immDate)
        )
    );
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param pii         personal identifiable information
   * @param vaccination used for inoculation
   */
  /* package */ InformationOfVaccination(
      final PersonalIdentifiableInformation pii,
      final Vaccination                     vaccination
  ) {
    insPii         = pii;
    insVaccination = vaccination;
  } // end constructor */

  /**
   * Pseudo-constructor used to collect information for a {@link InformationOfVaccination}.
   *
   * <p>This method corresponds to use case {@code UC_10_CeroVacInfo} in file {@code README.md},
   * see there.
   *
   * <p>Assertions: At least six elements are present in {@code arguments}.
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>element is a prefix for a file name used to store
   *                        the information in files-system
   *                    <li>element contains name of individual to whom the
   *                        {@link InformationOfVaccination} belongs
   *                    <li>element contains day of birth of the vaccinated
   *                         individual, format {@code "YYYY-MM-DD"}
   *                    <li>element contains email-address of individual, arbitrary {@link String}
   *                    <li>element contains the manufacturer of the vaccine
   *                    <li>element contains the name of the vaccine
   *                    <li>batch identifier, arbitrary {@link String}
   *                    <li>element contains the date of vaccination,
   *                        format {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
   *                  </ol>
   *
   * @throws IOException if underlying methods do so
   */
  public static void createCeroVacInfo(
      final ConcurrentLinkedQueue<String> arguments
  ) throws IOException {
    if (arguments.size() < 6) { // NOPMD literal in conditional statement
      // ... too few arguments
      final String newLine = System.lineSeparator();

      CmdLine.LOGGER.atInfo().log(
          List.of(
              CmdLine.ACTION_CEROVAC_CREATE
                  + "prefix name dayOfBirth emailAddress manufacturer vaccine batch date",
              "    prefix      : prefix of file-name used to store information",
              "    name        : name of individual being inoculated",
              "    dayOfBirth  : of individual in format \"YYYY-MM-DD\"",
              "    emailAddress: of individual",
              "    manufacturer: name of manufacturer of vaccine",
              "    vaccine     : name of vaccine",
              "    batch       : identifier of ampulla",
              "    date        : date and time of vaccination in format, e.g. "
              + "\"2013-12-28T18:47:59+01:00[Europe/Paris]\""
          ).stream()
              .collect(Collectors.joining(
                  newLine + "  ", // delimiter
                  newLine + newLine + "Usage:" + newLine
                      + "  " + CmdLine.ACTION_CEROVAC_CREATE + newLine + "  ", // prefix
                  newLine + newLine      // suffix
              ))
      );

      return;
    } // end if
    // ... enough arguments

    // --- calculate path for storing information to file-system
    final String prefix = arguments.remove();

    // --- create instance of CertificateOfVaccination
    final InformationOfVaccination ceroVac = new InformationOfVaccination(
        arguments.remove(), arguments.remove(), arguments.remove(), // personal information
        arguments.remove(), arguments.remove(), // vaccine
        arguments.remove(), arguments.remove()  // immunization
    );

    // --- store instance in file-system
    Utils.exportTlv(Utils.PATH_UC10, prefix, ceroVac.encode());
  } // end method */

  /**
   * Decode QR-code.
   *
   * <p>This method corresponds to use case {@code UC_50_Decode_QR-Code} in file {@code README.md},
   * see there.
   *
   * <p>Assertions: At least one elements is present in {@code arguments}.
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>element is a prefix for a file name used to store
   *                        the information in files-system.
   *                  </ol>
   *
   * @throws IOException     if underlying methods do so
   */
  public static void decodeQrCode(
      final ConcurrentLinkedQueue<String> arguments
  ) throws
      ChecksumException,
      FormatException,
      IOException,
      NotFoundException {
    if (arguments.size() < 1) { // NOPMD literal in conditional statement
      // ... too few arguments
      CmdLine.showUsage();

      return;
    } // end if
    // ... enough arguments

    // --- get file-name prefix from arguments
    final String prefix = arguments.remove();

    // --- decode QR-code
    final String text = new QRCodeReader()
        .decode(
            new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(ImageIO.read(
                    Utils.PATH_UC40.resolve(prefix + ".png").toFile()
                ))
            ))
        )
        .getText();

    // --- write text to output-directory
    // Note: In the end this use case writes to directory UC_50_Decode_QR-Code. In order to keep
    //       things simple at the beginning this version of the program writes to directory
    //       UC_70_DecodeCeroVacInfo TODO.
    Files.write(
        Utils.PATH_UC70.resolve(prefix + ".txt"),
        text.getBytes(StandardCharsets.UTF_8)
    );
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
   *       CertificateOfVaccination :== SEQUENCE {<br>
   *           version                         INTEGER (1),<br>
   *           personalIdentifiableInformation PersonalIdentifiableInformation,<br>
   *           vaccination                     Vaccination<br>
   *       }}
   *       </pre>
   * </ol>
   *
   * <p>For specification of {@code PersonalIdentifiableInformation},
   * see {@link PersonalIdentifiableInformation#decode(DerSequence)}.
   *
   * <p>For specification of {@code Vaccination},
   * see {@link Vaccination#decode(DerSequence)}.
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
  public static InformationOfVaccination decode(
      final DerSequence sequence
  ) {
    int index = 0;
    final List<BerTlv> valueField = sequence.getDecoded();
    final int version = ((DerInteger) valueField.get(index)).getDecoded().intValueExact();

    final PersonalIdentifiableInformation pii;
    final Vaccination vaccination;
    switch (version) { // NOPMD switch with less than 3 branches
      case 1: {
        pii = PersonalIdentifiableInformation.decode((DerSequence) valueField.get(++index));
        vaccination = Vaccination.decode((DerSequence) valueField.get(index));
      } break; // end version = 1

      default: {
        throw new IllegalArgumentException("unknown version: " + version);
      } // end default
    } // end switch (version)

    return new InformationOfVaccination(pii, vaccination);
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
        // FIXME PII
        getVaccination().encode()
    ));
  } // end method */

  /**
   * Converts a message into a QR-code.
   *
   * <p>This method corresponds to use case {@code UC_40_Encode_QR-Code} in file {@code README.md},
   * see there.
   *
   * <p>Assertions: At least one elements is present in {@code arguments}.
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>element is a prefix for a file name used to store
   *                        the information in files-system.
   *                  </ol>
   *
   * @throws IOException     if underlying methods do so
   * @throws WriterException if underlying methods do so
   */
  public static void encodeQrCode(
      final ConcurrentLinkedQueue<String> arguments
  ) throws
      IOException,
      WriterException {
    if (arguments.size() < 1) { // NOPMD literal in conditional statement
      // ... too few arguments
      CmdLine.showUsage();

      return;
    } // end if
    // ... enough arguments

    // --- get file-name prefix from arguments
    final String prefix = arguments.remove();

    // --- get content to be encoded as QR-code
    final String text = new String(
        Files.readAllBytes(Utils.PATH_UC30.resolve(prefix + ".txt")),
        StandardCharsets.UTF_8
    );

    // --- create QR-code
    final int size = 1;

    MatrixToImageWriter.writeToPath(
        new QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size, // width
            size, // height
            Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
        ),
        "PNG",
        Utils.PATH_UC40.resolve(prefix + ".png")
    );
  } // end method */

  /**
   * Returns information about the individual the {@link InformationOfVaccination} belongs to.
   *
   * @return {@link PersonalIdentifiableInformation} of vaccinated individual
   */
  public PersonalIdentifiableInformation getPii() {
    return insPii;
  } // end method */

  /**
   * Returns vaccination.
   *
   * @return instance attribute {@link Vaccination}
   */
  public Vaccination getVaccination() {
    return insVaccination;
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
        "Certificate of Vaccination:%n"
            + "  %s%n"
            + "  - was inoculated according to%n"
            + "    %s",
        getPii(),
        getVaccination()
    );
  } // end method */
} // end class
