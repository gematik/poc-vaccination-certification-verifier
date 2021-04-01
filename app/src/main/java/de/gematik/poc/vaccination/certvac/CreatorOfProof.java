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

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import com.gmail.alfred65fiedler.utils.Base45;
import com.gmail.alfred65fiedler.utils.Hex;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import de.gematik.poc.vaccination.pki.CborSigner;
import de.gematik.poc.vaccination.userinterface.CmdLine;
import de.gematik.poc.vaccination.utils.Utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * This class creates a 2D-barcode used for proofing the
 * health status of an individual.
 */
public final class CreatorOfProof {
  /**
   * Private default-constructor.
   */
  private CreatorOfProof() {
    // intentionally empty
  } // end constructor */

  /**
   * Creates 2D-barcodes.
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>element is a prefix for a file name used to store
   *                        the information in files-system
   *                  </ol>
   */
  public static void createBarcode(
      final ConcurrentLinkedQueue<String> arguments
  ) throws
      IOException,
      WriterException {
    if (arguments.size() < 1) { // NOPMD literal in conditional statement
      // ... too few arguments
      final String newLine = System.lineSeparator();

      CmdLine.LOGGER.atInfo().log(
          List.of(// list with parameter explanation
              "prefix: prefix of file-name used to store 2D-barcode"
          ).stream()
              .collect(Collectors.joining(
                  newLine + "  ",                // delimiter
                  newLine + newLine + "Usage: "  // start prefix with usage description
                      + CmdLine.ACTION_QR_ENCODE // action followed by parameter list
                      + " prefix"
                      + newLine + "  ",          // end prefix
                  newLine + newLine              // suffix
              ))
      );

      return;
    } // end if
    // ... enough arguments

    final String prefix = arguments.remove();

    // --- get content
    final byte[] content = Files.readAllBytes(
        Utils.PATH_UC30.resolve(prefix + "_cbor.bin")
    );

    // --- start investigation on various encodings  . . . . . . . . . . . . . . . . . . . . . . . .
    final String base10 = new BigInteger(1, content).toString();
    final String base45 = Base45.encode(content);
    final String base64 = Base64.getEncoder().encodeToString(content);
    final int lengthOs = content.length;
    final int length10 = base10.length();
    final int length45 = base45.length();
    final int length64 = base64.length();
    CmdLine.LOGGER.atInfo().log("createBarcode: {}, OS = {}", lengthOs, Hex.toHexDigits(content));
    CmdLine.LOGGER.atInfo().log("createBarcode: {}, 10 = {}", length10, base10);
    CmdLine.LOGGER.atInfo().log("createBarcode: {}, 45 = {}", length45, base45);
    CmdLine.LOGGER.atInfo().log("createBarcode: {}, 64 = {}", length64, base64);

    final int bitOs = lengthOs * 8;
    final int bit10 = (int) Math.round(Math.ceil(length10 * 10 / 3.0));
    final int bit45 = (int) Math.round(Math.ceil(length45 * 11 / 2.0));
    final int bit64 = length64 * 8;
    CmdLine.LOGGER.atInfo().log(String.format(
        "OS: %5.1f%%: %d bit", 100.0 * bitOs / ((double) bitOs), bitOs
    ));
    CmdLine.LOGGER.atInfo().log(String.format(
        "10: %5.1f%%: %d bit", 100.0 * bit10 / ((double) bitOs), bit10
    ));
    CmdLine.LOGGER.atInfo().log(String.format(
        "10: %5.1f%%: %d bit", 100.0 * bit45 / ((double) bitOs), bit45
    ));
    CmdLine.LOGGER.atInfo().log(String.format(
        "64: %5.1f%%: %d bit", 100.0 * bit64 / ((double) bitOs), bit64
    ));
    // end investigation on various encodings ___________________________________________________ */

    // --- create QR-code
    // QR-code tutorial: https://www.thonky.com/qr-code-tutorial/introduction
    // Aztec-code: http://barcodeguide.seagullscientific.com/Content/Symbologies/Aztec_Code.htm
    // Aztec-code: https://en.wikipedia.org/wiki/Aztec_Code
    final String text = Base45.encode(content);
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
   * Sign proof.
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>element is a prefix for a file name used to store
   *                        the information in files-system
   *                    <li>common name of end-entity signing the proof
   *                  </ol>
   */
  public static void signProof(
      final ConcurrentLinkedQueue<String> arguments
  ) throws
      CborException,
      CertificateException,
      IOException,
      KeyStoreException,
      InvalidKeyException,
      NoSuchAlgorithmException,
      SignatureException,
      UnrecoverableKeyException {
    if (arguments.size() < 2) { // NOPMD literal in conditional statement
      // ... too few arguments
      final String newLine = System.lineSeparator();

      CmdLine.LOGGER.atInfo().log(
          List.of(// list with parameter explanation
              "prefix    : prefix of file-name used to store information",
              "commonName: common name of EndEntity used for signing"
          ).stream()
              .collect(Collectors.joining(
                  newLine + "  ",                     // delimiter
                  newLine + newLine + "Usage: "       // start prefix with usage description
                      + CmdLine.ACTION_INFOPROOF_SIGN // action followed by parameter list
                      + " prefix commonName"
                      + newLine + "  ",               // end prefix
                  newLine + newLine                   // suffix
              ))
      );

      return;
    } // end if
    // ... enough arguments

    final String prefix = arguments.remove();
    final String commonName = arguments.remove();

    // --- get information for proof
    final byte[] information = getInformation(prefix);

    // --- sign information
    final byte[] signature = CborSigner.sign(information, commonName);

    // --- get certificate of signer
    final byte[] certificate = CborSigner.getCertificate(commonName);

    // --- compile content of barcode
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new CborEncoder(baos).encode(new CborBuilder()
        .add(information)
        .add(signature)
        .add(certificate)
        .build()
    );
    final byte[] content = baos.toByteArray();

    // --- store signed InformationOfProof
    Files.write(
        Utils.PATH_UC30.resolve(prefix + "_cbor.bin"),
        content
    );
    Files.write(
        Utils.PATH_UC30.resolve(prefix + "_cbor-hexdigits.txt"),
        Hex.toHexDigits(content).getBytes(StandardCharsets.UTF_8)
    );
  } // end method */

  private static byte[] getInformation(
      final String prefix
  ) throws IOException {
    return Files.readAllBytes(Utils.PATH_UC20.resolve(prefix + "_cbor.bin"));
  } // end method */
} // end class
