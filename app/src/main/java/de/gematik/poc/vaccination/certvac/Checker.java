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

package de.gematik.poc.vaccination.certvac; // NOPMD high number of imports

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Number;
import com.gmail.alfred65fiedler.utils.Base45;
import com.gmail.alfred65fiedler.utils.Hex;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import de.gematik.poc.vaccination.pki.CborSigner;
import de.gematik.poc.vaccination.userinterface.CmdLine;
import de.gematik.poc.vaccination.utils.Utils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

/**
 * This class is intended to scan 2D-barcodes and check if an individual is allowed to pass.
 */
// Note 1: Spotbugs claims "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
//         Short message: Unchecked/unconfirmed cast of return value from method
//         That finding is suppressed because casting is intentional.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
    "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE" // see note 1
}) // */
public final class Checker {
  /**
   * File extension for binary CBOR files.
   */
  /* package */ static final String EXTENSION_CBOR = "_cbor.bin"; // */

  /**
   * Private default-constructor.
   */
  private Checker() {
    // intentionally empty
  } // end constructor */

  /**
   * Decode QR-code.
   *
   * <p>This method corresponds to use case {@code UC_50_Decode_QR-Code} in file
   * {@code README.md}, see there.
   *
   * <p>Assertions: At least one elements is present in {@code arguments}.
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>element is a prefix for a file name used to store
   *                        the information in files-system.
   *                  </ol>
   *
   * @throws IOException if underlying methods do so
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
      final String newLine = System.lineSeparator();

      CmdLine.LOGGER.atInfo().log(
          List.of(// list with parameter explanation
              "prefix: prefix of file-name used get 2D-barcode"
          ).stream()
              .collect(Collectors.joining(
                  newLine + "  ",                // delimiter
                  newLine + newLine + "Usage: "  // start prefix with usage description
                      + CmdLine.ACTION_QR_DECODE // action followed by parameter list
                      + " prefix"
                      + newLine + "  ",          // end prefix
                  newLine + newLine              // suffix
              ))
      );

      return;
    } // end if
    // ... enough arguments

    // --- get file-name prefix from arguments
    final String prefix = arguments.remove();

    // --- get content of QR-code
    final String content = getQrCodeContent(prefix);

    // --- convert content of QR-code to octet-string
    final byte[] octets = Base45.decode(content);

    // --- store information
    Files.write(
        Utils.PATH_UC50.resolve(prefix + EXTENSION_CBOR),
        octets
    );
    Files.write(
        Utils.PATH_UC50.resolve(prefix + "_cbor-hexDigits.txt"),
        Hex.toHexDigits(octets).getBytes(StandardCharsets.UTF_8)
    );
  } // end method */

  /**
   * Verify signature of signed message.
   *
   * <p>This method corresponds to use case {@code UC_60_VerifySignature} in file
   * {@code README.md}, see there.
   *
   * <p>Assertions: At least one elements is present in {@code arguments}.
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>element is a prefix for a file name used to store
   *                        the information in files-system.
   *                  </ol>
   *
   * @throws IOException if underlying methods do so
   */
  public static void verifySignature(
      final ConcurrentLinkedQueue<String> arguments
  ) throws
      CborException,
      IOException {
    if (arguments.size() < 1) { // NOPMD literal in conditional statement
      // ... too few arguments
      final String newLine = System.lineSeparator();

      CmdLine.LOGGER.atInfo().log(
          List.of(// list with parameter explanation
              "prefix: prefix of file-name used get signed message"
          ).stream()
              .collect(Collectors.joining(
                  newLine + "  ",                       // delimiter
                  newLine + newLine + "Usage: "         // start prefix with usage description
                      + CmdLine.ACTION_INFOPROOF_VERIFY // action followed by parameter list
                      + " prefix"
                      + newLine + "  ",                 // end prefix
                  newLine + newLine                     // suffix
              ))
      );

      return;
    } // end if
    // ... enough arguments

    // --- get file-name prefix from arguments
    final String prefix = arguments.remove();

    // --- get signed message
    final byte[] octets = Files.readAllBytes(
        Utils.PATH_UC50.resolve(prefix + EXTENSION_CBOR)
    );

    // --- convert to CBOR
    final Iterator<DataItem> dataItemIterator = CborDecoder.decode(octets).iterator();

    // --- verify certificate, signature and extract message
    final byte[] message = verifySignature(dataItemIterator);

    // --- write message
    Files.write(
        Utils.PATH_UC60.resolve(prefix + EXTENSION_CBOR),
        message
    );
    Files.write(
        Utils.PATH_UC60.resolve(prefix + "_cbor-hexdigits.txt"),
        Hex.toHexDigits(message).getBytes(StandardCharsets.UTF_8)
    );
  } // end method */

  /**
   * Verifies the signature and returns the signed message.
   *
   * <p>It is assumed, that the signed message was created by
   * {@link CreatorOfProof#signProof(ConcurrentLinkedQueue)}.
   *
   * <p>If verifying the certificate fails or verifying the signature fails,
   * then an exception is thrown.
   *
   * @param cborItems concatenation of three CBOR byte strings:
   *                  {@code message || signature || compactCertificate}
   *
   * @return message
   *
   * @throws IllegalArgumentException if signature verification fails
   */
  /* package */ static byte[] verifySignature(
      final Iterator<DataItem> cborItems
  ) {
    try {
      // --- retrieve first item, the message
      final byte[] message = ((ByteString) cborItems.next())
          // spotbugs: BC_UNCONFIRMED_CAST_OF_RETURN_VALUE
          .getBytes();

      // --- retrieve registrar and version number from message
      final Iterator<Object> itVersion = extractVersionNumber(
          CborDecoder.decode(message).iterator()
      ).iterator();
      final Registrar registrar = (Registrar) itVersion.next();
      final int version = (Integer) itVersion.next();

      switch (registrar) {
        case GERMANY: {
          switch (version) { // NOPMD too few branches
            case -1:
              // signed message is concatenation of three CBOR byte strings.
              // Note: The first CBOR byte string is already extracted as message.
              // message || signature || compactCertificate
              final byte[] signature = ((ByteString) cborItems.next())
                  // spotbugs: BC_UNCONFIRMED_CAST_OF_RETURN_VALUE
                  .getBytes();
              final byte[] compactCert = ((ByteString) cborItems.next())
                  // spotbugs: BC_UNCONFIRMED_CAST_OF_RETURN_VALUE
                  .getBytes();
              final ECPublicKey puk = CborSigner.verifyCompactCertificate(compactCert);

              if (CborSigner.verify(message, signature, puk)) {
                // ... valid signature
                //     => return message
                return message;
              } // end if
              // ... invalid signature
              throw new IllegalArgumentException("invalid signature");
              // end version == -1

            default:
              throw new IllegalArgumentException("unknown version: " + version);
          } // end switch (version)
        } // end Germany

        default:
          throw new IllegalArgumentException("unknown registrar: " + registrar);
      } // end switch (registrar)
    } catch (CborException
        | CertificateException
        | IOException
        | InvalidKeyException
        | InvalidKeySpecException
        | KeyStoreException
        | NoSuchAlgorithmException
        | SignatureException e) {
      throw new IllegalArgumentException("invalid signature", e);
    } // end catch (...)
  } // end method */

  /**
   * Extracts registrar and version number from gieven {@code message}.
   *
   * @return list with {@link Registrar} as first element and an {@link Integer}
   *         with version number as second element
   */
  public static List<Object> extractVersionNumber(
      final Iterator<DataItem> iterator
  ) {
    // --- first element in array SHALL be a registrar
    final Registrar registrar = Registrar.getInstance(
        ((Number) iterator.next()) // spotbugs: BC_UNCONFIRMED_CAST_OF_RETURN_VALUE
            .getValue()
            .intValueExact()
    );

    // --- second element in array SHALL be a version number encoded as an integer
    final int version = ((Number) iterator.next()) // spotbugs: BC_UNCONFIRMED_CAST_OF_RETURN_VALUE
        .getValue()
        .intValueExact();

    return List.of(registrar, version);
  } // end method */

  /**
   * Verify signature of signed message.
   *
   * <p>This method corresponds to use case {@code UC_60_VerifySignature} in file
   * {@code README.md}, see there.
   *
   * <p>Assertions: At least one elements is present in {@code arguments}.
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>element is a prefix for a file name used to store
   *                        the information in files-system.
   *                  </ol>
   *
   * @throws IOException if underlying methods do so
   */
  public static void decodeMessage(
      final ConcurrentLinkedQueue<String> arguments
  ) throws
      CborException,
      IOException {
    if (arguments.size() < 1) { // NOPMD literal in conditional statement
      // ... too few arguments
      final String newLine = System.lineSeparator();

      CmdLine.LOGGER.atInfo().log(
          List.of(// list with parameter explanation
              "prefix: prefix of file-name used get encoded message"
          ).stream()
              .collect(Collectors.joining(
                  newLine + "  ",                       // delimiter
                  newLine + newLine + "Usage: "         // start prefix with usage description
                      + CmdLine.ACTION_INFOPROOF_DECODE // action followed by parameter list
                      + " prefix"
                      + newLine + "  ",                 // end prefix
                  newLine + newLine                     // suffix
              ))
      );

      return;
    } // end if
    // ... enough arguments

    // --- get file-name prefix from arguments
    final String prefix = arguments.remove();

    // --- get message
    final byte[] octets = Files.readAllBytes(
        Utils.PATH_UC60.resolve(prefix + EXTENSION_CBOR)
    );

    // --- get information of proof
    final InformationOfProof infoOfProof = InformationOfProof.decode(octets);

    // --- write text to output-directory
    final String text = String.format(
        "Name           : %s%n"
            + "Day of birth   : %s%n"
            + "Expiration date: %s%n"
            + "Health status  :%n%s",
        infoOfProof.getName(),
        infoOfProof.getDayOfBirth(),
        infoOfProof.getExpirationDate(),
        infoOfProof.getHealthStatusMap()
    );

    Files.write(
        Utils.PATH_UC70.resolve(prefix + ".txt"),
        text.getBytes(StandardCharsets.UTF_8)
    );
  } // end method */

  /**
   * Decodes an QR-code.
   *
   * @return pure text content of that QR-code
   */
  private static String getQrCodeContent(
      final String prefix
  ) throws
      ChecksumException,
      FormatException,
      IOException,
      NotFoundException {
    return new QRCodeReader()
        .decode(
            new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(ImageIO.read(
                    Utils.PATH_UC40.resolve(prefix + ".png").toFile()
                ))
            ))
        )
        .getText();
  } // end method */
} // end class
