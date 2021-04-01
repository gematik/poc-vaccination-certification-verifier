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

package de.gematik.poc.vaccination.pki; // NOPMD high number of imports

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Number;
import com.gmail.alfred65fiedler.crypto.AfiElcParameterSpec;
import com.gmail.alfred65fiedler.crypto.AfiElcUtils;
import com.gmail.alfred65fiedler.tlv.BerTlv;
import com.gmail.alfred65fiedler.tlv.DerBitString;
import com.gmail.alfred65fiedler.tlv.DerInteger;
import com.gmail.alfred65fiedler.tlv.DerSequence;
import com.gmail.alfred65fiedler.utils.AfiBigInteger;
import com.gmail.alfred65fiedler.utils.AfiUtils;
import com.gmail.alfred65fiedler.utils.Hex;
import de.gematik.poc.vaccination.certvac.InformationOfProof;
import de.gematik.poc.vaccination.userinterface.CmdLine;
import de.gematik.poc.vaccination.utils.Utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides end-entity signing facilities.
 */
// Note 1: Spotbugs claims "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
//         Short message: Unchecked/unconfirmed cast of return value from method
//         That finding is suppressed because casting is intentional.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
    "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE" // see note 1
}) // */
public final class CborSigner { // NOPMD too many methods
  /**
   * Logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CborSigner.class); // */

  /**
   * Suffix used for compact certificates.
   */
  /* package */ static final String SUFFIX_COMPACT = "_compactCert"; // */

  /**
   * Private default-constructor.
   */
  private CborSigner() {
    // intentionally empty
  } // end constructor */

  /**
   * Create compact certificate.
   *
   * <p>A compact certificate is a PKI certificate, similar to an X.509 certificate,
   * but with less information. It has just enough information to check it and to
   * extract a public key from it. The goal is to have a PKI certificate with a
   * small memory footprint. In particular: A compact certificate contains:
   * <ol>
   *   <li>A number (an integer) used as an identifier. Together with the registrar
   *       and version number from {@link InformationOfProof} this identifier
   *       <b>SHALL</b> be unique. This identifier references the private keys
   *       used for signing this PKI certificate (i.e. CAR, Certificate
   *       Authentication Reference, i.e. commonName of issuer).
   *   <li>A public key as compressed encoding. The domain parameters of that
   *       public key are implicitly given by registrar and version number.
   * </ol>
   *
   * <p>The coding of a compact certificate is as follows:
   * <ol>
   *   <li>The (unique) identifier as
   *       <a href="https://www.rfc-editor.org/rfc/rfc8949.html">CBOR</a>
   *       major type 0 or 1 (i.e. an unsigned or negative integer)
   *   <li>The (compressed) point from public key as
   *       <a href="https://www.rfc-editor.org/rfc/rfc8949.html">CBOR</a>
   *       major type 2 byte string.
   *   <li>The signature of the PKI certificate as created by
   *       {@link CborSigner#sign(byte[], String)} encoded as
   *       <a href="https://www.rfc-editor.org/rfc/rfc8949.html">CBOR</a>
   *       major type 2 byte string.
   *   <li>A compact certificate has the following (CBOR) structure:
   *       <pre>
   *         identifier         := CBOR-integer(commonNameIssuer)
   *         publicKey          := CBOR-byte-string(compressed point from public key of subject)
   *         messageToBeSigned  := identifier || publicKey
   *         signature          := CBOR-byte-string(R || S)
   *         compactCertificate := array[message, signature]
   *       </pre>
   * </ol>
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>common name of subject, the public key of that
   *                        subject is enclosed in the compact certificate
   *                    <li>common name of issuer of the compact certificate,
   *                        the integer representation of that common name is
   *                        used as identifier (CAR) in the compact certificate
   *                        and the issuer's signature is stored in the compact
   *                        certificate
   *                  </ol>
   *
   * @return {@code TRUE} if compact certificate was successfully created,
   *         {@code FALSE} otherwise
   *
   * @throws CertificateException     if underlying methods do so
   * @throws KeyStoreException        if underlying methods do so
   * @throws IOException              if underlying methods do so
   * @throws NoSuchAlgorithmException if underlying methods do so
   * @throws NoSuchElementException   if there is no agency with given
   *                                  {@code commonName}
   * @throws NumberFormatException    if {@code commonNameIssuer} cannot be
   *                                  converted by {@link Integer#parseInt(String)}
   */
  public static boolean createCompactCertificate(
      final ConcurrentLinkedQueue<String> arguments
  ) throws
      CborException,
      CertificateException,
      KeyStoreException,
      IOException,
      InvalidKeyException,
      NoSuchAlgorithmException,
      SignatureException,
      UnrecoverableKeyException {
    if (arguments.size() < 2) { // NOPMD literal in conditional statement
      // ... too few arguments
      final String newLine = System.lineSeparator();

      CmdLine.LOGGER.atInfo().log(
          List.of(// list with parameter explanation
              "commonNameSubject: common name of entity for which a",
              "                   compact certificate is requested",
              "commonNameIssuer : common name of CA issuing the",
              "                   compact certificate"
          ).stream()
              .collect(Collectors.joining(
                  newLine + "  ",                          // delimiter
                  newLine + newLine + "Usage: "            // start prefix with usage description
                      + CmdLine.ACTION_PKI_ROOT_CA         // action followed by parameter list
                      + " commonNameSabject commonNameIssuer"
                      + newLine + "  ",                    // end prefix
                  newLine + newLine                        // suffix
              ))
      );

      return false;
    } // end if
    // ... enough arguments

    LOGGER.info("start: createCompactCertificate");

    final String commonNameSubject = arguments.remove();
    final String commonNameIssuer = arguments.remove();

    // --- compile message
    final byte[] message = aggregateMessageElements(commonNameSubject, commonNameIssuer);

    // --- sign message
    final byte[] signature = sign(message, commonNameIssuer);

    // --- compile content of compact certificate
    final byte[] content = aggregateCertificateElements(message, signature);

    // --- store compact certificate
    storeCertificate(commonNameSubject, content);

    LOGGER.info("end  : createCompactCertificate");

    return true;
  } // end method */

  /**
   * Verifies given compact certificate.
   *
   * <p>It is assumed, that the given {@code compactCertificate} was created by
   * {@link #createCompactCertificate(ConcurrentLinkedQueue)}.
   *
   * <p>If verifying the compact certificate is successful, then the
   * {@link ECPublicKey} contained in the compact certificate is returned.
   * If the verification fails for any reason an exception is thrown.
   *
   * @param octets compact certificate to be verified
   *
   * @return public key contained in the compact certificate
   */
  public static ECPublicKey verifyCompactCertificate(
      final byte[] octets
  ) throws
      CborException,
      CertificateException,
      IOException,
      InvalidKeyException,
      InvalidKeySpecException,
      KeyStoreException,
      NoSuchAlgorithmException,
      SignatureException {
    final Iterator<byte[]> iterator = extractCertificateElements(octets).iterator();
    final byte[] message   = iterator.next();
    final byte[] signature = iterator.next();

    final Iterator<DataItem> itContent = CborDecoder.decode(message).iterator();
    final int identifier = ((Number) itContent.next()).getValue()
        .intValueExact(); // spotbugs: BC_UNCONFIRMED_CAST_OF_RETURN_VALUE
    if (verify(message, signature, Integer.toString(identifier))) {
      // ... signature is valid
      //     => extract and create public key
      // TODO estimate domain parameter from registrar and version number, rather
      //      than use fixed domain parameter
      final AfiElcParameterSpec dp = AfiElcParameterSpec.brainpoolP256r1;

      // extract compressed point from signed message
      final byte[] compressed = ((ByteString) itContent.next())
          .getBytes(); // spotbugs: BC_UNCONFIRMED_CAST_OF_RETURN_VALUE

      // convert compressed point to point on elliptic curve
      final ECPoint point = AfiElcUtils.os2p(compressed, dp);

      // estimate public key from point and domain parameters
      final ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(point, dp);

      // estimate public key
      final KeyFactory keyFactory = KeyFactory.getInstance("EC");
      return (ECPublicKey) keyFactory.generatePublic(publicKeySpec);
    } // end if
    // ... signature is invalid

    throw new IllegalArgumentException("invalid certificate");
  } // end method */

  /**
   * Extracts elements from a compact certificate.
   *
   * <p>This is kind of the inverse operation to
   * {@link #aggregateCertificateElements(byte[], byte[])}.
   *
   * @param octets compact certificate in encoded format
   *
   * @return list with components of a compact certificate,
   *         the first element contains the {@code message},
   *         the second element contains the signature
   */
  /* package */ static List<byte[]> extractCertificateElements(
      final byte[] octets
  ) throws CborException {
    // --- extract iterator for CBOR data items from octets
    final Iterator<DataItem> items = CborDecoder.decode(octets).iterator();

    // --- there SHALL be just one data item: an array
    final Array array = (Array) items.next(); // spotbugs: BC_UNCONFIRMED_CAST_OF_RETURN_VALUE

    // --- extract elements from array
    final Iterator<DataItem> elements = array.getDataItems().iterator();
    final byte[] message   = ((ByteString) elements.next())
        .getBytes(); // spotbugs: BC_UNCONFIRMED_CAST_OF_RETURN_VALUE
    final byte[] signature = ((ByteString) elements.next())
        .getBytes(); // spotbugs: BC_UNCONFIRMED_CAST_OF_RETURN_VALUE

    return List.of(message, signature);
  } // end method */

  /**
   * Aggregates the content of a compact certificate.
   *
   * <p>The content of each parameter (i.e. {@code message} and {@code signature}
   * is encapsulated into a CBOR major type 2 (i.e. byte string). These byte
   * strings are then encapsulated into a CBOR major type 4 (i.e. array).
   *
   * <p>This is kind of the inverse operation to {@link #extractCertificateElements(byte[])}.
   *
   * @param message   contained in the compact certificate
   * @param signature for {@code message}
   *
   * @return content of compact certificate, i.e. CBOR-array [message, signature]
   *
   * @throws CborException if underlying methods do so
   */
  /* package */ static byte[] aggregateCertificateElements(
      final byte[] message,
      final byte[] signature
  ) throws CborException {
    final ByteArrayOutputStream result = new ByteArrayOutputStream();
    new CborEncoder(result).encode(new CborBuilder()
        .addArray()     // start array
        .add(message)   // +- message
        .add(signature) // +- signature
        .end()          // end array
        .build()
    );

    return result.toByteArray();
  } // end method */

  /**
   * Compiles message to be signed within a compact certificate.
   *
   * @param commonNameSubject common name of subject, the public key of that
   *                          subject is contained in the compact certificate
   * @param commonNameIssuer  common name of issuer of the compact certificate,
   *                          the issuer computes the digital signature of the
   *                          compact certificate
   *
   * @return message to be signed within a compact certificate, i.e.
   *         <pre>
   *           identifier         := CBOR-integer(commonNameIssuer)
   *           publicKey          := CBOR-byte-string(compressed point from public key of subject)
   *           messageToBeSigned  := identifier || publicKey
   *         </pre>
   *
   * @throws CborException            if underlying methods do so
   * @throws CertificateException     if underlying methods do so
   * @throws KeyStoreException        if underlying methods do so
   * @throws IOException              if underlying methods do so
   * @throws NoSuchAlgorithmException if underlying methods do so
   */
  /* package */ static byte[] aggregateMessageElements(
      final String commonNameSubject,
      final String commonNameIssuer
  ) throws
      CertificateException,
      KeyStoreException,
      IOException,
      NoSuchAlgorithmException,
      CborException {
    final int identifier = Integer.parseInt(commonNameIssuer);
    final ECPublicKey puk = PublicKeyInfrastructure.getPublicKey(commonNameSubject);

    final ByteArrayOutputStream result = new ByteArrayOutputStream();
    new CborEncoder(result).encode(new CborBuilder()
        .add(identifier)                // add identifier
        .add(AfiElcUtils.p2osCompressed(// add compressed point from public key
            puk.getW(),
            AfiElcParameterSpec.getInstance(puk.getParams())
        ))
        .build()
    );

    return result.toByteArray();
  } // end method */

  /**
   * Retrieve compact certificate.
   *
   * @param commonName of entity whose compact certificate is requested
   *
   * @return content of compact certificate as is
   */
  public static byte[] getCertificate(
      final String commonName
  ) throws IOException {
    return Files.readAllBytes(
        PublicKeyInfrastructure.getPath(commonName).resolve(
            commonName + SUFFIX_COMPACT + Utils.EXTENSION_BIN
        )
    );
  } // end method */

  /* package */ static void storeCertificate(
      final String commonNameSubject,
      final byte[] content
  ) throws IOException {
    final Path pathSubject = PublicKeyInfrastructure.getPath(commonNameSubject);
    Files.write(// write as binary data
        pathSubject.resolve(
            commonNameSubject + SUFFIX_COMPACT + Utils.EXTENSION_BIN
        ),
        content
    );
    Files.write(// write as hex-digits data
        pathSubject.resolve(
            commonNameSubject + SUFFIX_COMPACT + "-bin.txt"
        ),
        Hex.toHexDigits(content).getBytes(StandardCharsets.UTF_8)
    );
  } // end method */

  /**
   * Signs given message.
   *
   * @param message    to be signed
   * @param commonName of signer
   *
   * @return ECDSA signature {@code R || S}
   */
  public static byte[] sign(
      final byte[] message,
      final String commonName
  ) throws
      CertificateException,
      IOException,
      InvalidKeyException,
      KeyStoreException,
      NoSuchAlgorithmException,
      SignatureException,
      UnrecoverableKeyException {
    // --- get private key
    final ECPrivateKey prk = PublicKeyInfrastructure.getPrivateKey(commonName);
    final int tau = (int) Math.round(Math.ceil(prk.getParams().getOrder().bitLength() / 8.0));

    // --- compute digital signature
    final DerBitString signature = PublicKeyInfrastructure.signEcdsa(message, prk);

    // --- compress signature
    return compressSignature(signature, tau);
  } // end constructor */

  /**
   * Verifies a signature.
   *
   * @param message    for which signature is verified
   * @param signature  as concatenation of {@code R || S}
   * @param commonName of verifier
   *
   * @return {@code TRUE} is signature is valid,
   *         {@code FALSE} otherwise
   */
  public static boolean verify(
      final byte[] message,
      final byte[] signature,
      final String commonName
  ) throws
      CertificateException,
      KeyStoreException,
      IOException,
      InvalidKeyException,
      NoSuchAlgorithmException,
      SignatureException {
    // --- verify signature
    return PublicKeyInfrastructure.verifyEcdsa(
        message,
        expandSignature(signature),                      // expand signature
        PublicKeyInfrastructure.getPublicKey(commonName) // get public key
    );
  } // end method */

  /**
   * Verifies a signature.
   *
   * @param message    for which signature is verified
   * @param signature  as concatenation of {@code R || S}
   * @param puk        public key used to verify signature
   *
   * @return {@code TRUE} is signature is valid,
   *         {@code FALSE} otherwise
   */
  public static boolean verify(
      final byte[] message,
      final byte[] signature,
      final ECPublicKey puk
  ) throws
      CertificateException,
      KeyStoreException,
      IOException,
      InvalidKeyException,
      NoSuchAlgorithmException,
      SignatureException {
    // --- verify signature
    return PublicKeyInfrastructure.verifyEcdsa(
        message,
        expandSignature(signature), // expand signature
        puk                         // get public key
    );
  } // end method */

  /* package */ static byte[] compressSignature(
      final DerBitString signature,
      final int tau
  ) {
    final List<BerTlv> sigC = ((DerSequence) BerTlv.getInstance(signature.getDecoded()))
        .getDecoded();

    // --- extract r and s from signature
    final BigInteger biR = ((DerInteger) sigC.get(0)).getDecoded();
    final BigInteger biS = ((DerInteger) sigC.get(1)).getDecoded();

    return AfiUtils.concatenate(
        AfiBigInteger.i2os(biR, tau),
        AfiBigInteger.i2os(biS, tau)
    );
  } // end method */

  /* package */ static DerBitString expandSignature(
      final byte[] signature
  ) {
    if (1 == (signature.length & 1)) { // NOPMD literal in conditional statment
      // ... length of signature is odd
      //     => do not know how to split that evenly into R || S
      throw new IllegalArgumentException("odd number of octet in signature R || S");
    } // end if
    // ... even number of octet in signature

    final int size = signature.length >> 1;
    final byte[] octetR = Arrays.copyOfRange(signature, 0, size);
    final byte[] octetS = Arrays.copyOfRange(signature, size, signature.length);

    return new DerBitString(
        new DerSequence(List.of(
            new DerInteger(new BigInteger(1, octetR)),
            new DerInteger(new BigInteger(1, octetS))
        )).toByteArray()
    );
  } // end method */
} // end class
