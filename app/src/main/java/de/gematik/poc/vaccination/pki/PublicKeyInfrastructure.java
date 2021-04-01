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

import com.gmail.alfred65fiedler.crypto.AfiElcParameterSpec;
import com.gmail.alfred65fiedler.tlv.BerTlv;
import com.gmail.alfred65fiedler.tlv.ConstructedBerTlv;
import com.gmail.alfred65fiedler.tlv.DerBitString;
import com.gmail.alfred65fiedler.tlv.DerInteger;
import com.gmail.alfred65fiedler.tlv.DerNull;
import com.gmail.alfred65fiedler.tlv.DerOctetString;
import com.gmail.alfred65fiedler.tlv.DerOid;
import com.gmail.alfred65fiedler.tlv.DerPrintableString;
import com.gmail.alfred65fiedler.tlv.DerSequence;
import com.gmail.alfred65fiedler.tlv.DerSet;
import com.gmail.alfred65fiedler.tlv.DerUtcTime;
import com.gmail.alfred65fiedler.utils.AfiOid;
import com.gmail.alfred65fiedler.utils.EafiHashAlgorithm;
import de.gematik.poc.vaccination.userinterface.CmdLine;
import de.gematik.poc.vaccination.utils.Utils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import javax.security.auth.x500.X500Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a (rather simple) PKI-structure.
 *
 * <p>In particular:
 * <ol>
 *   <li>Creation of asymmetric key pairs
 *   <li>Creation of PKI-certificates
 * </ol>
 *
 * <p>Basically a PKI-structure could be seen as a tree-like structure with a RootCA as its root,
 * optionally one or more Certification Authorities (CA) as nodes of the tree and end-entities
 * as leaves. This version of this class reflects the tree-like structure of a PKI also for
 * storing keys and PKI-certificates persistently in a file-system.
 * <ol>
 *   <li>The RootCA is stored in a certain directory of the file-system.
 *   <li>An entity (CA or end-entity) which has its PKI-certificate signed by another entity
 *       (CA or RootCA) is stored in a subdirectory of the signing entity.
 *   <li>The file-name of a directory is identical to its
 *       <a href="https://tools.ietf.org/html/rfc5280#section-4.1.2.6">subjectName</a>.
 *   <li>Each entity (i.e. RootCA, CA, end-entity) posses an asymmetric key pair.
 *   <li>For security reasons the key length differ. The largest key length is used for RootCA,
 *       the shortest key length is used for end-entities.
 *   <li>For each entity the following artifacts are stored in the file-system:
 *       <ol>
 *         <li>{@link ECPublicKey#getEncoded()} as TLV-structure in three formats,
 *         <li>{@link ECPrivateKey#getEncoded()} as TLV-structure in three formats,<br>
 *             <i><b>Note:</b> Exporting private keys as plain text is (typically) a security hole.
 *                             For a proof-of-concept (PoC) this is intentionally tolerated.</i>
 *         <li>Self-signed {@link Certificate#getEncoded()} as TLV-structure in three formats,
 *         <li>{@link Certificate#getEncoded()} as TLV-structure in three formats,
 *             with {@link ECPublicKey} of this entity signed by a CA.<br>
 *             <i><b>Note:</b> RootCA is the only entity where this {@link Certificate} and the
 *                             self-signed {@link Certificate} are equal.</i>
 *         <li>{@link KeyStore} for {@link ECPrivateKey},
 *         <li>{@link KeyStore} with the complete {@link Certificate}-chain for
 *             this entity plus all {@link Certificate}s signed by this entity.
 *       </ol>
 * </ol>
 */
// Note 1: Spotbugs claims: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE, i.e.
//         Possible null pointer dereference due to return value of called method
//         That finding is correct. It belongs to places where the parent of a path
//         is requested (which might be null). This finding is ignored hereafter,
//         because paths in this project are so long that the parent of a path is
//         assumed to never be null.
// Note 2: Spotbugs claims: RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE, i.e.
//         Redundant nullcheck of value known to be non-null
//         This is a false positive in try-with-resources structures.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
    "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",  // see note 1
    "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE" // see note 2
}) // */
public final class PublicKeyInfrastructure { // NOPMD too many methods
  /**
   * Type of {@link Certificate}.
   */
  /* package */ static final String CERTIFICATE_TYPE = "X.509"; // */

  /**
   * Password used for all {@link KeyStore} operations.
   *
   * <p><i><b>Note:</b> For real world applications such a password would not be stored in source
   * code. Furthermore, at least each {@link KeyStore} would have its own
   * password-value. Even better, on top of a {@link KeyStore}-specific
   * password-value, each {@link java.security.Key} in a {@link KeyStore}
   * would have a unique password-value.</i>
   */
  /* package */ static final char[] KEYSTORE_PASSWORD = "1234".toCharArray(); // */

  /**
   * Type of {@link KeyStore}.
   */
  /* package */ static final String KEYSTORE_TYPE = "PKCS12"; // */

  /**
   * Path to public-key-infrastructure (PKI) information.
   */
  /* package */ static Path claPkiBasePath = CmdLine.BASE_PATH.resolve(("pki")); // */

  /**
   * Suffix used for {@link KeyStore}s storing {@link Certificate}s only.
   */
  /* package */ static final String SUFFIX_KEYSTORE_X509 = "_keyStore.X509"; // */

  /**
   * Suffix used for {@link KeyStore}s storing {@link PrivateKey} of an entity.
   */
  /* package */ static final String SUFFIX_KEYSTORE_PRIVATE = "_keyStore.private"; // */

  /**
   * Suffix for {@link Certificate}s.
   *
   * <p><i><b>Note:</b> This suffix is used for any {@link X509Certificate}, self-signed or not.</i>
   */
  /* package */ static final String SUFFIX_X509 = "_" + CERTIFICATE_TYPE; // */

  /**
   * Suffix for self-signed {@link Certificate}.
   */
  /* package */ static final String SUFFIX_SELF_SIGNED = "_selfSigned" + SUFFIX_X509; // */

  /**
   * Suffix for storing {@link ECPrivateKey}.
   */
  /* package */ static final String SUFFIX_PRIVATE_KEY = "_privateKey-PKCS-8"; // */

  /**
   * Suffix for storing {@link ECPublicKey}.
   */
  /* package */ static final String SUFFIX_PUBLIC_KEY = "_publicKey-PKCS-8"; // */

  /**
   * Logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(PublicKeyInfrastructure.class); // */

  /**
   * Private default-constructor.
   *
   * <p><i><b>Note:</b> This is a utility class.</i>
   */
  private PublicKeyInfrastructure() {
    // intentionally empty
  } // end constructor */

  /**
   * Creates an entity (i.e. non Root-CA).
   *
   * <p>Assertions:
   * <ol>
   *   <li>At least one element is present in {@code arguments}.
   *   <li>The first element in {@code arguments} has a length greater than zero.
   * </ol>
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>commonName of CA
   *                    <li>CA signing the X.509 certificate
   *                  </ol>
   *
   * @return {@code TRUE} if entity is successfully created,
   *         {@code FALSE} otherwise
   *
   * @throws CertificateException               if underlying methods do so
   * @throws IOException                        if underlying methods do so
   * @throws InvalidAlgorithmParameterException if underlying methods do so
   * @throws InvalidKeyException                if underlying methods do so
   * @throws KeyStoreException                  if underlying methods do so
   * @throws NoSuchAlgorithmException           if underlying methods do so
   * @throws SignatureException                 if underlying methods do so
   * @throws UnrecoverableKeyException          if underlying methods do so
   */
  public static boolean createCa(
      final ConcurrentLinkedQueue<String> arguments
  ) throws
      CertificateException,
      IOException,
      InvalidAlgorithmParameterException,
      InvalidKeyException,
      KeyStoreException,
      NoSuchAlgorithmException,
      SignatureException,
      UnrecoverableKeyException {
    if (arguments.size() < 2) { // NOPMD literal in conditional statement
      // ... too few arguments
      final String newLine = System.lineSeparator();

      CmdLine.LOGGER.atInfo().log(
          List.of(// list with parameter explanation
              "commonName: common name of CA (arbitrary printable string)",
              "rootCA    : common name of Root-CA used for creating a X.509 for this CA"
          ).stream()
              .collect(Collectors.joining(
                  newLine + "  ",               // delimiter
                  newLine + newLine + "Usage: " // start prefix with usage description
                      + CmdLine.ACTION_PKI_CA   // action followed by parameter list
                      + " commonName rootCA"
                      + newLine + "  ",         // end prefix
                  newLine + newLine             // suffix
              ))
      );

      return false;
    } // end if
    // ... enough arguments

    LOGGER.atInfo().log("start: createCA");

    final String commonName = arguments.remove();
    final String certificationAuthority = arguments.remove();

    createEntity(commonName, certificationAuthority, AfiElcParameterSpec.brainpoolP384r1);

    LOGGER.atInfo().log("end  : createCA");

    return true;
  } // end method */

  /**
   * Create directory in file-system for an entity.
   *
   * @param path to be created.
   *
   * @throws IOException              if underlying methods do so
   * @throws IllegalArgumentException if {@code directory} already exists
   */
  /* package */ static void createDirectory(
      final Path path
  ) throws IOException {
    if (Files.exists(path)) {
      // ... directory already present
      //     => inform user and exit
      LOGGER.atError().log("ERROR: Entity at {} already exists.", path);
      LOGGER.atError().log(
          "       Program terminates without adding, modifying or deleting anything"
      );

      throw new IllegalArgumentException("entity already exists");
    } else if (!Files.isDirectory(path.getParent())) { // NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
      // ... parent absent
      //     => inform user and exit
      LOGGER.atError().log("ERROR: CA at {} absent.", path.getParent());
      LOGGER.atError().log(
          "       Program terminates without adding, modifying or deleting anything"
      );

      throw new IllegalArgumentException("CA absent");
    } // end if
    // ... entity does not (yet) exist
    //     => create appropriate directory
    Files.createDirectory(path);
  } // end method */

  /**
   * Creates an entity (i.e. non Root-CA).
   *
   * <p>Assertions:
   * <ol>
   *   <li>At least one element is present in {@code arguments}.
   *   <li>The first element in {@code arguments} has a length greater than zero.
   * </ol>
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>commonName of End-Entity
   *                    <li>CA signing the X.509 certificate
   *                  </ol>
   *
   * @return {@code TRUE} if entity is successfully created,
   *         {@code FALSE} otherwise
   *
   * @throws CertificateException               if underlying methods do so
   * @throws IOException                        if underlying methods do so
   * @throws InvalidAlgorithmParameterException if underlying methods do so
   * @throws InvalidKeyException                if underlying methods do so
   * @throws KeyStoreException                  if underlying methods do so
   * @throws NoSuchAlgorithmException           if underlying methods do so
   * @throws SignatureException                 if underlying methods do so
   * @throws UnrecoverableKeyException          if underlying methods do so
   */
  public static boolean createEndEntity(
      final ConcurrentLinkedQueue<String> arguments
  ) throws
      CertificateException,
      IOException,
      InvalidAlgorithmParameterException,
      InvalidKeyException,
      KeyStoreException,
      NoSuchAlgorithmException,
      SignatureException,
      UnrecoverableKeyException {
    if (arguments.size() < 2) { // NOPMD literal in conditional statement
      // ... too few arguments
      final String newLine = System.lineSeparator();

      CmdLine.LOGGER.atInfo().log(
          List.of(// list with parameter explanation
              "commonName  : common name of end-entity (arbitrary printable string)",
              "commonNameCa: common name of CA (arbitrary printable string)"
          ).stream()
              .collect(Collectors.joining(
                  newLine + "  ",                          // delimiter
                  newLine + newLine + "Usage: "            // start prefix with usage description
                      + CmdLine.ACTION_PKI_ROOT_CA         // action followed by parameter list
                      + " commonName commonNameCa"
                      + newLine + "  ",                    // end prefix
                  newLine + newLine                        // suffix
              ))
      );

      return false;
    } // end if
    // ... enough arguments

    LOGGER.atInfo().log("start: createEndEntity");

    final String commonName = arguments.remove();
    final String certificationAuthority = arguments.remove();

    createEntity(commonName, certificationAuthority, AfiElcParameterSpec.brainpoolP256r1);

    LOGGER.atInfo().log("end  : createEndEntity");

    return true;
  } // end method */

  /**
   * Creates an entity (i.e. non Root-CA).
   *
   * <p>Assertions:
   * <ol>
   *   <li>At least one element is present in {@code arguments}.
   *   <li>The first element in {@code arguments} has a length greater than zero.
   * </ol>
   *
   * @param commonName             of this entity, a printable string
   * @param certificationAuthority signing the {@link X509Certificate} of this entity
   * @param domainParameter        used for key generation
   *
   * @throws CertificateException               if underlying methods do so
   * @throws IOException                        if underlying methods do so
   * @throws InvalidAlgorithmParameterException if underlying methods do so
   * @throws InvalidKeyException                if underlying methods do so
   * @throws KeyStoreException                  if underlying methods do so
   * @throws NoSuchAlgorithmException           if underlying methods do so
   * @throws SignatureException                 if underlying methods do so
   * @throws UnrecoverableKeyException          if underlying methods do so
   */
  /* package */ static void createEntity(
      final String commonName,
      final String certificationAuthority,
      final ECParameterSpec domainParameter
  ) throws
      CertificateException,
      IOException,
      InvalidAlgorithmParameterException,
      InvalidKeyException,
      KeyStoreException,
      NoSuchAlgorithmException,
      SignatureException,
      UnrecoverableKeyException {
    LOGGER.atInfo().log(
        "start: createEntity for commonName CN=\"{}\" and CA: {}",
        commonName,
        certificationAuthority
    );

    final Path directory = getPath(certificationAuthority).resolve(commonName);

    // --- create key pair
    final KeyPair keyPair = generateAsymmetricKeyPair(
        directory,
        domainParameter
    );

    // --- create X.509 certificate from CA
    createX509Certificate(
        directory,
        keyPair.getPublic()
    );

    // --- export certificate to a key store
    createKeyStore(directory, commonName, keyPair.getPrivate());

    LOGGER.atInfo().log("end  : createEntity for commonName CN=\"{}\"", commonName);
  } // end method */

  /**
   * Creates a certificate for given public key signed by CA from parent directory.
   *
   * @param directory with information of entity for which a {@link X509Certificate} is created
   * @param publicKeySubject of entity
   *
   * @throws CertificateException      if underlying methods do so
   * @throws IOException               if underlying methods do so
   * @throws InvalidKeyException       if underlying methods do so
   * @throws KeyStoreException         if underlying methods do so
   * @throws NoSuchAlgorithmException  if underlying methods do so
   * @throws SignatureException        if underlying methods do so
   * @throws UnrecoverableKeyException if underlying methods do so
   */
  private static void createX509Certificate(
      final Path directory,
      final PublicKey publicKeySubject
  ) throws
      CertificateException,
      IOException,
      InvalidKeyException,
      KeyStoreException,
      NoSuchAlgorithmException,
      SignatureException,
      UnrecoverableKeyException {
    final String commonNameSubject = directory
        .getFileName()// Spotbugs: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
        .toString();
    final String commonNameIssuer = directory
        .getParent()// Spotbugs: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
        .getFileName()// Spotbugs: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
        .toString();

    // --- get private key used for signing
    final ECPrivateKey privateKeyIssuer = getPrivateKey(commonNameIssuer);

    // --- create certificate
    // Note: According to https://tools.ietf.org/html/rfc5280#section-4.1
    //       a X.509 certificate has the following elements:
    //           Certificate  ::=  SEQUENCE  {
    //               tbsCertificate       TBSCertificate,
    //               signatureAlgorithm   AlgorithmIdentifier,
    //               signatureValue       BIT STRING  }

    // create tbsCertificate
    final BerTlv tbsCertificate = tbsCertificate(
        commonNameIssuer,  // issuer
        commonNameSubject, // subject
        (ECPublicKey) publicKeySubject,
        privateKeyIssuer
    );
    LOGGER.atDebug().log("tbsCertificate = {}", tbsCertificate.toStringTree());

    // create signatureAlgorithm
    final BerTlv signatureAlgorithm = signatureAlgorithm(privateKeyIssuer.getParams());
    LOGGER.atDebug().log("signatureAlgorithm = {}", signatureAlgorithm.toStringTree());

    // create signatureValue
    final BerTlv signatureValue = signEcdsa(tbsCertificate.toByteArray(), privateKeyIssuer);
    LOGGER.atDebug().log("signatureValue = {}", signatureValue.toStringTree());

    // compose X.509 certificate
    final DerSequence x509 = new DerSequence(List.of(
        tbsCertificate,
        signatureAlgorithm,
        signatureValue
    ));
    LOGGER.atInfo().log("X.509 certificate = {}", x509.toStringTree());

    // --- export X.509 certificate
    Utils.exportTlv(directory, commonNameSubject + SUFFIX_X509, x509);

    // --- add X.509 certificate to key store of CA which signed it
    // create X.509 certificate
    final CertificateFactory certificateFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE);
    final X509Certificate tmp = (X509Certificate) certificateFactory
        .generateCertificate(new ByteArrayInputStream(x509.toByteArray()));

    // open key store
    final Path pathKeyStore = directory
        .getParent()// Spotbugs: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
        .resolve(commonNameIssuer + SUFFIX_KEYSTORE_X509);
    final KeyStore keyStoreCertificate = KeyStore.getInstance(
        pathKeyStore.toFile(),
        KEYSTORE_PASSWORD
    );

    // add X.509 to opened key store
    keyStoreCertificate.setCertificateEntry(getCommonName(tmp), tmp);

    //  save key store to file-system
    try (OutputStream fos = Files.newOutputStream(pathKeyStore)) {
      keyStoreCertificate.store(fos, KEYSTORE_PASSWORD);
    } // end try-with-resources, Spotbugs RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE
  } // end method */

  /**
   * Creates {@code signatureAlgorithm} from given {@code domainParameter}.
   *
   * @param domainParameter used to create {@code signatureAlgorithm}
   *
   * @return {@code signatureAlgorithm according to}
   *     <a href="https://tools.ietf.org/html/rfc5280#section-4.1.1.2">RFC 5280 clause 4.1.1.2</a>
   */
  private static BerTlv signatureAlgorithm(
      final ECParameterSpec domainParameter
  ) {
    // --- estimate signature algorithm
    final int size = ((ECFieldFp) domainParameter.getCurve().getField()).getP().bitLength();
    final AfiOid oid;
    if (size <= 256) { // NOPMD literal in conditional statement
      oid = AfiOid.ECDSA_with_SHA256;
    } else if (size <= 384) { // NOPMD literal in conditional statement
      oid = AfiOid.ECDSA_with_SHA384;
    } else {
      oid = AfiOid.ECDSA_with_SHA512;
    } // end if

    // --- create signatureAlgorithm
    // see https://tools.ietf.org/html/rfc5280#section-4.1.1.2
    return new DerSequence(List.of(
        new DerOid(oid),
        DerNull.NULL
    ));
  } // end method */

  /**
   * Creates a {@code signatureValue} for given data.
   *
   * <p>This is the inverse function to
   * {@link #verifyEcdsa(byte[], DerBitString, ECPublicKey)}.
   *
   * @param message to be signed
   * @param key     used for signing
   *
   * @return {@code signatureValue} according to
   *          <a href="https://tools.ietf.org/html/rfc5280">RFC 5280</a>
   *
   * @throws InvalidKeyException      if underlying methods do so
   * @throws NoSuchAlgorithmException if underlying methods do so
   * @throws SignatureException       if underlying methods do so
   */
  /* package */ static DerBitString signEcdsa(
      final byte[] message,
      final ECPrivateKey key
  ) throws
      InvalidKeyException,
      NoSuchAlgorithmException,
      SignatureException {
    // --- estimate algorithm
    final int size = ((ECFieldFp) key.getParams().getCurve().getField()).getP().bitLength();
    final String algorithm;
    if (size <= 256) { // NOPMD literal in conditional statement
      algorithm = "SHA256withECDSA";
    } else if (size <= 384) { // NOPMD literal in conditional statement
      algorithm = "SHA384withECDSA";
    } else {
      algorithm = "SHA512withECDSA";
    } // end if

    // --- compute signature
    final Signature signer = Signature.getInstance(algorithm);
    signer.initSign(key);
    signer.update(message);

    return new DerBitString(signer.sign());
  } // end method */

  /**
   * Verifies a {@code signatureValue} for given {@code message}.
   *
   * <p>This is the inverse function to {@link #signEcdsa(byte[], ECPrivateKey)}.
   *
   * @param message   corresponding to {@code signature}
   * @param signature {@code signatureValue} according to
   *                  <a href="https://tools.ietf.org/html/rfc5280">RFC 5280</a>
   * @param key       used for signature verification
   *
   * @return {@code TRUE} if signature verification was successful,
   *         {@code FALSE} if signature verification fails
   *
   * @throws InvalidKeyException      if underlying methods do so
   * @throws NoSuchAlgorithmException if underlying methods do so
   * @throws SignatureException       if underlying methods do so
   */
  /* package */ static boolean verifyEcdsa(
      final byte[] message,
      final DerBitString signature,
      final ECPublicKey key
  ) throws
      InvalidKeyException,
      NoSuchAlgorithmException,
      SignatureException {
    final int size = ((ECFieldFp) key.getParams().getCurve().getField()).getP().bitLength();
    final String algorithm;
    if (size <= 256) { // NOPMD literal in conditional statement
      algorithm = "SHA256withECDSA";
    } else if (size <= 384) { // NOPMD literal in conditional statement
      algorithm = "SHA384withECDSA";
    } else {
      algorithm = "SHA512withECDSA";
    } // end if

    // --- verify signature
    final Signature verifier = Signature.getInstance(algorithm);
    verifier.initVerify(key);
    verifier.update(message);

    return verifier.verify(signature.getDecoded());
  } // end method */

  /**
   * Create key stores.
   *
   * <p>In particular:
   * <ol>
   *   <li>collect certificates for certificate-chain,
   *   <li>create {@link KeyStore} for {@link PrivateKey},
   *   <li>create {@link KeyStore} with the complete {@link Certificate}-chain for
   *       this entity plus all {@link Certificate}s signed by this entity.
   * </ol>
   *
   * @param directory   directory where artefacts are stored
   * @param subjectName of file name
   * @param privateKey  to be stored in {@link KeyStore}
   *
   * @throws CertificateException     if underlying methods do so
   * @throws IOException              if underlying methods do so
   * @throws KeyStoreException        if underlying methods do so
   * @throws NoSuchAlgorithmException if underlying methods do so
   */
  /* package */ static void createKeyStore(
      final Path directory,
      final String subjectName,
      final PrivateKey privateKey
  ) throws
      CertificateException,
      IOException,
      KeyStoreException,
      NoSuchAlgorithmException {
    // --- create a certificate factory
    final CertificateFactory certificateFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE);

    // --- load certificate-chain
    //     i.e.: loop until self-signed root-certificate is reached
    final List<X509Certificate> chain = new ArrayList<>();

    // Note: First iteration of "loop" reads certificate NOT from a KeyStore
    //       (which is not yet available), but from file-system.
    Path currentDirectory = directory;
    X509Certificate currentX509 = (X509Certificate) certificateFactory
        .generateCertificate(new ByteArrayInputStream(Files.readAllBytes(
            directory.resolve(subjectName + SUFFIX_X509 + Utils.EXTENSION_BIN)
        )));
    chain.add(currentX509);
    while (!currentX509.getSubjectX500Principal().equals(currentX509.getIssuerX500Principal())) {
      // ... subject != issuer
      //     => currentX509 is not self-signed
      currentDirectory = currentDirectory
          .getParent(); // Spotbugs: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
      final String currentSubjectName = currentDirectory
          .getFileName()// Spotbugs: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
          .toString();

      // --- open existing key store with certificates in currentDirectory
      final KeyStore keyStoreCert = KeyStore.getInstance(
          currentDirectory.resolve(currentSubjectName + SUFFIX_KEYSTORE_X509).toFile(),
          KEYSTORE_PASSWORD
      );
      currentX509 = (X509Certificate) keyStoreCert.getCertificate(currentSubjectName);
      chain.add(currentX509);
    } // end while (subject != issuer)
    // ... certificate-chain estimated
    LOGGER.atInfo().log("chain.size = {}, chain.content = {}", chain.size(), chain);
    final Certificate[] certificates = chain.toArray(new Certificate[0]);

    // --- create key store for private key
    // create an empty key store
    final KeyStore keyStorePrivate = KeyStore.getInstance(KEYSTORE_TYPE);
    keyStorePrivate.load(
        null, // InputStream, here null => load an empty key store
        KEYSTORE_PASSWORD
    );

    // fill key store with private key
    keyStorePrivate.setKeyEntry(
        subjectName,       // alias
        privateKey,        // key
        KEYSTORE_PASSWORD, // password
        certificates       // certificate-chain
    );

    //  save key store to file-system
    try (OutputStream fos = Files.newOutputStream(
        directory.resolve(subjectName + SUFFIX_KEYSTORE_PRIVATE)
    )) {
      keyStorePrivate.store(fos, KEYSTORE_PASSWORD);
    } // end try-with-resources

    // --- create key store with certificate chain of public key
    // create an empty key store
    final KeyStore keyStoreCertificate = KeyStore.getInstance(KEYSTORE_TYPE);
    keyStoreCertificate.load(
        null, // InputStream, here null => load an empty key store
        KEYSTORE_PASSWORD
    );

    // fill key store with certificates
    for (final X509Certificate x509 : chain) {
      keyStoreCertificate.setCertificateEntry(getCommonName(x509), x509);
    } // end for (x509...)

    //  save key store to file-system
    try (OutputStream fos = Files.newOutputStream(
        directory.resolve(subjectName + SUFFIX_KEYSTORE_X509)
    )) {
      keyStoreCertificate.store(fos, KEYSTORE_PASSWORD);
    } // end try-with-resources, Spotbugs RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE
  } // end method */

  /**
   * Extracts {@code commonName} from a {@link X509Certificate}.
   *
   * @param x509 certificate
   *
   * @return {@code commonName}
   */
  /* package */ static String getCommonName(
      final X509Certificate x509
  ) {
    final X500Principal principal = x509.getSubjectX500Principal();
    final ConstructedBerTlv tlv = (ConstructedBerTlv) BerTlv.getInstance(principal.getEncoded());
    final ConstructedBerTlv set = tlv.getConstructed(DerSet.TAG).get();
    final ConstructedBerTlv seq = set.getConstructed(DerSequence.TAG).get();

    return ((DerPrintableString) seq.get(DerPrintableString.TAG).get()).getDecoded();
  } // end method */

  /**
   * Returns path to directory where a signature creating agency is stored.
   *
   * @param commonName of signature creating agency
   *
   * @return path to directory where {@link KeyStore} and other stuff is stored
   *
   * @throws IOException            if underlying methods do so
   * @throws NoSuchElementException if there is no agency with given
   *                                {@code commonName}
   */
  /* package */ static Path getPath(
      final String commonName
  ) throws IOException {
    final Path directory = Files.walk(claPkiBasePath)
        .filter(file -> file.getFileName().toString().equals(commonName))
        .findAny()
        .orElseThrow();
    LOGGER.atDebug().log("CborSigner in \"{}\"", directory);

    return directory;
  } // end method */

  /**
   * Gets {@link ECPrivateKey} associated with given {@code commonName}.
   *
   * @param commonName of signature creating agency for which its private key
   *                   is requested
   *
   * @return requested {@link ECPrivateKey}
   *
   * @throws CertificateException      if underlying methods do so
   * @throws KeyStoreException         if underlying methods do so
   * @throws IOException               if underlying methods do so
   * @throws NoSuchAlgorithmException  if underlying methods do so
   * @throws NoSuchElementException    if there is no agency with given
   *                                   {@code commonName}
   * @throws UnrecoverableKeyException if underlying methods do so
   */
  /* package */ static ECPrivateKey getPrivateKey(
      final String commonName
  ) throws
      CertificateException,
      KeyStoreException,
      IOException,
      NoSuchAlgorithmException,
      UnrecoverableKeyException {
    // --- open appropriate KeyStore
    final KeyStore keyStore = KeyStore.getInstance(
        getPath(commonName)
            .resolve(commonName + SUFFIX_KEYSTORE_PRIVATE)
            .toFile(),
        KEYSTORE_PASSWORD
    );

    // --- retrieve appropriate private key from KeyStore
    return (ECPrivateKey) keyStore.getKey(
        commonName, KEYSTORE_PASSWORD
    );
  } // end method */

  /**
   * Gets {@link ECPublicKey} associated with given {@code commonName}.
   *
   * @param commonName of signature verifiying agency for which its public key
   *                   is requested
   *
   * @return requested {@link ECPublicKey}
   *
   * @throws CertificateException      if underlying methods do so
   * @throws KeyStoreException         if underlying methods do so
   * @throws IOException               if underlying methods do so
   * @throws NoSuchAlgorithmException  if underlying methods do so
   * @throws NoSuchElementException    if there is no agency with given
   *                                   {@code commonName}
   */
  /* package */ static ECPublicKey getPublicKey(
      final String commonName
  ) throws
      CertificateException,
      KeyStoreException,
      IOException,
      NoSuchAlgorithmException {
    // --- open appropriate KeyStore
    final KeyStore keyStore = KeyStore.getInstance(
        getPath(commonName)
            .resolve(commonName + SUFFIX_KEYSTORE_X509)
            .toFile(),
        KEYSTORE_PASSWORD
    );

    // --- retrieve appropriate certificate
    final Certificate certificate = keyStore.getCertificate(commonName);

    // --- retrieve appropriate private key from certificate
    return (ECPublicKey) certificate.getPublicKey();
  } // end method */

  /**
   * Create Root-CA.
   *
   * <p>Method creates a root certification authority (Root-CA) and
   * a corresponding self-signed X.509-certificate.
   *
   * <p>Assertions:
   * <ol>
   *   <li>At least one element is present in {@code arguments}.
   *   <li>The first element in {@code arguments} has a length greater than zero.
   * </ol>
   *
   * @param arguments arguments used within this method,
   *                  <ol>
   *                    <li>commonName of Root-CA
   *                  </ol>
   *
   * @return {@code TRUE} if entity is successfully created,
   *         {@code FALSE} otherwise
   *
   * @throws CertificateException               if underlying methods do so
   * @throws IOException                        if underlying methods do so
   * @throws InvalidAlgorithmParameterException if underlying methods do so
   * @throws InvalidKeyException                if underlying methods do so
   * @throws KeyStoreException                  if underlying methods do so
   * @throws NoSuchAlgorithmException           if underlying methods do so
   * @throws SignatureException                 if underlying methods do so
   */
  public static boolean createRootCa(
      final ConcurrentLinkedQueue<String> arguments
  ) throws
      CertificateException,
      IOException,
      InvalidAlgorithmParameterException,
      InvalidKeyException,
      KeyStoreException,
      NoSuchAlgorithmException,
      SignatureException {
    if (arguments.size() < 1) { // NOPMD literal in conditional statement
      // ... too few arguments
      final String newLine = System.lineSeparator();

      CmdLine.LOGGER.atInfo().log(
          List.of(// list with parameter explanation
              "commonName: common name of Root-CA (arbitrary printable string)"
          ).stream()
              .collect(Collectors.joining(
                  newLine + "  ",                  // delimiter
                  newLine + newLine + "Usage: "    // start prefix with usage description
                      + CmdLine.ACTION_PKI_ROOT_CA // action followed by parameter list
                      + " commonName"
                      + newLine + "  ",            // end prefix
                  newLine + newLine                // suffix
              ))
      );

      return false;
    } // end if
    // ... enough arguments

    final String commonName = arguments.remove();
    LOGGER.atInfo().log("start: createRootCa for commonName CN=\"{}\"", commonName);

    // --- check if RootCA already exists
    final Path directory = claPkiBasePath.resolve(commonName);

    // --- create key pair, intentionally the strongest domain parameters are used for Root-CA
    final KeyPair keyPair = generateAsymmetricKeyPair(
        directory,
        AfiElcParameterSpec.brainpoolP512r1
    );

    // --- get X.509 certificate from CA
    //     Note: For a Root-CA this is equal to the self-signed certificate.
    final BerTlv x509 = BerTlv.getInstance(Files.readAllBytes(
        directory.resolve(commonName + SUFFIX_SELF_SIGNED + Utils.EXTENSION_BIN)
    ));

    // --- export X.509 certificate from CA
    Utils.exportTlv(directory, commonName + SUFFIX_X509, x509);

    // --- export self-signed certificate to a key store
    createKeyStore(directory, commonName, keyPair.getPrivate());

    LOGGER.atInfo().log("end  : createRootCa for dn=\"{}\"", commonName);

    return true;
  } // end method */

  /**
   * Exports key pair to file-system.
   *
   * <p>The following things are exported:
   * <ol>
   *   <li>{@link KeyStore} with public key
   *   <li>{@link KeyStore} with private key
   *   <li>public key as plain text,
   *   <li>self-signed certificate,
   *   <li>private key as plain text, see note 1 below.
   * </ol>
   *
   * <p><i><b>Note 1:</b> Exporting private keys as plain text is (typically) a security hole.
   *                      For a proof-of-concept (PoC) this is intentionally tolerated.</i>
   *
   * @param directory directory where artefacts are stored
   * @param keyPair   to be exported
   *
   * @throws IOException                        if underlying methods do so
   * @throws InvalidKeyException                if underlying methods do so
   * @throws NoSuchAlgorithmException           if underlying methods do so
   * @throws SignatureException                 if underlying methods do so
   */
  private static void exportKeyPair(// NOPMD long method
                                    final Path directory,
                                    final KeyPair keyPair
  ) throws
      IOException,
      InvalidKeyException,
      NoSuchAlgorithmException,
      SignatureException {
    LOGGER.atDebug().log("  exportKeyPair to: {}", directory);

    final String commonName = directory
        .getFileName()// Spotbugs: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
        .toString();

    // --- export private key
    final ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
    final BerTlv prkTlv = BerTlv.getInstance(privateKey.getEncoded());
    Utils.exportTlv(directory, commonName + SUFFIX_PRIVATE_KEY, prkTlv);

    // --- export public key to plain text file
    final ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
    final BerTlv pukTlv = BerTlv.getInstance(publicKey.getEncoded());
    Utils.exportTlv(directory, commonName + SUFFIX_PUBLIC_KEY, pukTlv);

    // --- create self-signed certificate
    // Note: According to https://tools.ietf.org/html/rfc5280#section-4.1
    //       a X.509 certificate has the following elements:
    //           Certificate  ::=  SEQUENCE  {
    //               tbsCertificate       TBSCertificate,
    //               signatureAlgorithm   AlgorithmIdentifier,
    //               signatureValue       BIT STRING  }

    // create tbsCertificate
    final BerTlv tbsCertificate = tbsCertificate(
        commonName, // issuer (here same as subject, because self-signed X.509 is generated
        commonName, // subject
        publicKey,
        privateKey
    );
    LOGGER.atDebug().log("tbsCertificate = {}", tbsCertificate.toStringTree());

    // create signatureAlgorithm
    final BerTlv signatureAlgorithm = signatureAlgorithm(privateKey.getParams());
    LOGGER.atDebug().log("signatureAlgorithm = {}", signatureAlgorithm.toStringTree());

    // create signatureValue
    final BerTlv signatureValue = signEcdsa(tbsCertificate.toByteArray(), privateKey);
    LOGGER.atDebug().log("signatureValue = {}", signatureValue.toStringTree());

    // compose X.509 certificate
    final DerSequence x509certificate = new DerSequence(List.of(
        tbsCertificate,
        signatureAlgorithm,
        signatureValue
    ));
    LOGGER.atDebug().log("X.509 certificate = {}", x509certificate.toStringTree());

    // --- export X.509 certificate
    Utils.exportTlv(directory, commonName + SUFFIX_SELF_SIGNED, x509certificate);
  } // end method */

  /**
   * Calculates a RDNSequence according to <a href="">RFC 5280 clause 4.1.2.6</a>.
   *
   * @param commonName to be used for calculation
   */
  private static DerSequence rdnSequence(
      final String commonName
  ) {
    return new DerSequence(List.of(// RDNSequence
        new DerSet(List.of(// RelativeDistinguishedName
            new DerSequence(List.of(// AttributeTypeAndValue
                new DerOid(AfiOid.commonName),
                new DerPrintableString(commonName)
            ))
        ))
    ));
  } // end method */

  /**
   * Generates an asymmetric key pair for given domain parameter.
   *
   * <p>In particular, this method performs all steps which are identical to all entities:
   * <ol>
   *   <li>create directory in file-system (might throw {@link IllegalArgumentException},
   *   <li>generate asymmetric key pair,
   *   <li>export {@link ECPrivateKey#getEncoded()},
   *   <li>export {@link ECPublicKey#getEncoded()},
   *   <li>export self-signed {@link Certificate#getEncoded()}.
   * </ol>
   *
   * @param domainParameter of generated key pair
   * @param directory       in file-system where information about the {@link KeyPair} is stored
   *
   * @throws IOException                        if underlying methods do so
   * @throws InvalidAlgorithmParameterException if underlying methods do so
   * @throws InvalidKeyException                if underlying methods do so
   * @throws NoSuchAlgorithmException           if underlying methods do so
   * @throws SignatureException                 if underlying methods do so
   */
  private static KeyPair generateAsymmetricKeyPair(
      final Path directory,
      final ECParameterSpec domainParameter
  ) throws
      IOException,
      InvalidAlgorithmParameterException,
      InvalidKeyException,
      NoSuchAlgorithmException,
      SignatureException {
    // --- create directory in file-system for entity
    createDirectory(directory);

    // --- generate asymmetric key pair
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
    keyPairGenerator.initialize(domainParameter);
    final KeyPair result = keyPairGenerator.generateKeyPair();

    // --- export key pair and self-signed certificate
    exportKeyPair(directory, result);

    return result;
  } // end method */

  /**
   * Creates {@code tbsCertificate} according to
   * <a href="https://tools.ietf.org/html/rfc5280#section-4.1.1.1">X.509 RFC 5280 clause 4.1.1.1</a>.
   *
   * @param commonNameIssuer  common name of issuer
   * @param commonNameSubject common name of subject
   * @param publicKey         for which a certificate is requested
   * @param privateKey        used for signing the certificate content
   */
  private static DerSequence tbsCertificate(// NOPMD avoid long methods
                                            final String commonNameIssuer,
                                            final String commonNameSubject,
                                            final ECPublicKey publicKey,
                                            final ECPrivateKey privateKey
  ) {
    // --- define some artifacts
    final ZonedDateTime validityNotBefore = ZonedDateTime.now(CmdLine.TIME_ZONE);
    final DerSequence subjectPublicKeyInfo = (DerSequence) BerTlv.getInstance(
        publicKey.getEncoded()
    );

    final byte[] pubKey = ((DerBitString) subjectPublicKeyInfo.get(DerBitString.TAG).get())
        .getDecoded();

    // --- TBSCertificate, see RFC 5280, clause 4.1 and 4.1.2, i.e
    //     see https://tools.ietf.org/html/rfc5280#section-4.1
    //     and https://tools.ietf.org/html/rfc5280#section-4.1.2
    return new DerSequence(List.of(
        // --- version         [0]  EXPLICIT Version DEFAULT v1
        //     see https://tools.ietf.org/html/rfc5280#section-4.1.2.1
        BerTlv.getInstance(0xa0, List.of(new DerInteger(BigInteger.TWO))),

        // --- serialNumber         CertificateSerialNumber
        //     CertificateSerialNumber ::= INTEGER
        //     see https://tools.ietf.org/html/rfc5280#section-4.1.2.2
        new DerInteger(BigInteger.valueOf(validityNotBefore.toInstant().getEpochSecond())),

        // --- signature            AlgorithmIdentifier
        //     see https://tools.ietf.org/html/rfc5280#section-4.1.2.3
        //     and https://tools.ietf.org/html/rfc5280#section-4.1.1.2
        signatureAlgorithm(privateKey.getParams()),

        // --- issuer               Name
        //     Name ::= CHOICE { -- only one possibility for now --
        //        rdnSequence  RDNSequence }
        //
        //     RDNSequence ::= SEQUENCE OF RelativeDistinguishedName
        //
        //     RelativeDistinguishedName ::=
        //        SET SIZE (1..MAX) OF AttributeTypeAndValue
        //
        //     AttributeTypeAndValue ::= SEQUENCE {
        //      type     AttributeType,
        //      value    AttributeValue }
        //
        //     AttributeType ::= OBJECT IDENTIFIER
        //
        //     AttributeValue ::= ANY -- DEFINED BY AttributeType
        //
        //     DirectoryString ::= CHOICE {
        //         teletexString           TeletexString (SIZE (1..MAX)),
        //         printableString         PrintableString (SIZE (1..MAX)),
        //         universalString         UniversalString (SIZE (1..MAX)),
        //         utf8String              UTF8String (SIZE (1..MAX)),
        //         bmpString               BMPString (SIZE (1..MAX)) }
        //     see https://tools.ietf.org/html/rfc5280#section-4.1.2.4
        rdnSequence(commonNameIssuer),

        // --- validity             Validity
        //     Validity ::= SEQUENCE {
        //        notBefore      Time,
        //        notAfter       Time }
        //
        //     Time ::= CHOICE {
        //        utcTime        UTCTime,
        //        generalTime    GeneralizedTime }
        //     see https://tools.ietf.org/html/rfc5280#section-4.1.2.5
        new DerSequence(List.of(
            new DerUtcTime(validityNotBefore, DerUtcTime.UtcTimeFormat.HH_MM_SS_Z),
            new DerUtcTime(validityNotBefore.plusYears(10), DerUtcTime.UtcTimeFormat.HH_MM_SS_Z)
        )),

        // --- subject              Name
        //     see https://tools.ietf.org/html/rfc5280#section-4.1.2.6
        rdnSequence(commonNameSubject),

        // --- subjectPublicKeyInfo SubjectPublicKeyInfo
        //     SubjectPublicKeyInfo  ::=  SEQUENCE  {
        //         algorithm            AlgorithmIdentifier,
        //         subjectPublicKey     BIT STRING  }
        //
        //     AlgorithmIdentifier  ::=  SEQUENCE  {
        //         algorithm               OBJECT IDENTIFIER,
        //         parameters              ANY DEFINED BY algorithm OPTIONAL  }
        //
        //     see https://tools.ietf.org/html/rfc5280#section-4.1.2.7
        //     and https://tools.ietf.org/html/rfc5280#section-4.1.1.2
        subjectPublicKeyInfo,

        // --- issuerUniqueID  [1]  IMPLICIT UniqueIdentifier OPTIONAL
        // --- subjectUniqueID [2]  IMPLICIT UniqueIdentifier OPTIONAL

        // --- extensions      [3]  EXPLICIT Extensions OPTIONAL
        //     Extensions  ::=  SEQUENCE SIZE (1..MAX) OF Extension
        //
        //     Extension  ::=  SEQUENCE  {
        //        extnID      OBJECT IDENTIFIER,
        //        critical    BOOLEAN DEFAULT FALSE,
        //        extnValue   OCTET STRING
        //                    -- contains the DER encoding of an ASN.1 value
        //                    -- corresponding to the extension type identified
        //                    -- by extnID
        //        }
        //     see https://tools.ietf.org/html/rfc5280#section-4.2.1.2
        BerTlv.getInstance(
            0xa3, // extensions      [3]  EXPLICIT Extensions
            List.of(// SEQUENCE
                new DerSequence(List.of(// subjectKeyIdentifier
                    new DerSequence(List.of(
                        new DerOid(AfiOid.subjectKeyIdentifier),
                        new DerOctetString(
                            new DerOctetString(
                                EafiHashAlgorithm.SHA_1.digest(pubKey)
                            ).toByteArray()
                        )
                    ))
                ))
            )
        )
    ));
  } // end method */

  /**
   * Miscellaneous.
   *
   * <p>This method is used for experiments.
   *
   * @param arguments to be considered
   */
  public static void misc(
      final ConcurrentLinkedQueue<String> arguments
  ) {
    LOGGER.atInfo().log("PKI_BASE_PATH: {}", claPkiBasePath);
  } // end method */
} // end class
