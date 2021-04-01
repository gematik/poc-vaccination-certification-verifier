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

package de.gematik.poc.vaccination.pki; // NOPMD too many static imports

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.gmail.alfred65fiedler.crypto.AfiElcParameterSpec;
import com.gmail.alfred65fiedler.crypto.AfiElcUtils;
import com.gmail.alfred65fiedler.tlv.BerTlv;
import com.gmail.alfred65fiedler.tlv.DerBitString;
import com.gmail.alfred65fiedler.tlv.DerInteger;
import com.gmail.alfred65fiedler.tlv.DerOctetString;
import com.gmail.alfred65fiedler.tlv.DerOid;
import com.gmail.alfred65fiedler.tlv.DerSequence;
import com.gmail.alfred65fiedler.utils.AfiOid;
import com.gmail.alfred65fiedler.utils.AfiRng;
import com.gmail.alfred65fiedler.utils.AfiUtils;
import com.gmail.alfred65fiedler.utils.Hex;
import de.gematik.poc.vaccination.userinterface.CmdLine;
import de.gematik.poc.vaccination.utils.Utils;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests on {@link PublicKeyInfrastructure}.
 */
// Note 1: Spotbugs claims "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
//         Short message: Possible null pointer dereference due to return value of called method
//         That finding is suppressed because paths are so long that null never occurs.
// Note 2: Spotbugs claims "REC_CATCH_EXCEPTION",
//         Short message: Exception is caught when Exception is not thrown
//         That finding is suppressed because the list of possible exceptions is rather long.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
    "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", // see note 1
    "REC_CATCH_EXCEPTION"                     // see note 2
}) // */
final class TestPublicKeyInfrastructure { // NOPMD too many methods
  /**
   * Random Number Generator.
   */
  private static final AfiRng RNG = new AfiRng(); // */

  /**
   * Message for unexpected exceptions.
   */
  private static final String UNEXPECTED = "unexpected exception"; // */

  /**
   * Message in exceptions.
   */
  private static final String ENTITY_EXISTS = "entity already exists"; // */

  /**
   * Method executed before other tests.
   */
  @BeforeAll
  static void setUpBeforeClass() throws IOException {
    // --- set the base path for PKI stuff to temporary test directory
    final Path tempDir = CmdLine
        .BASE_PATH
        .resolve(("pki"))
        .resolveSibling("junit.pki")
        .resolve(String.format("%016x", System.nanoTime()));
    PublicKeyInfrastructure.claPkiBasePath = Files.createDirectories(tempDir);
    assertTrue(Files.exists(PublicKeyInfrastructure.claPkiBasePath));
    assertTrue(Files.isDirectory(PublicKeyInfrastructure.claPkiBasePath));
  } // end method */

  /**
   * Method executed after other tests.
   */
  @AfterAll
  static void tearDownAfterClass() {
    // intentionally empty
  } // end method */

  /**
   * Method executed before each test.
   */
  @BeforeEach
  void setUp() {
    // intentionally empty
  } // end method */

  /**
   * Method executed after each test.
   */
  @AfterEach
  void tearDown() {
    // intentionally empty
  } // end method */

  /**
   * Test method for {@link PublicKeyInfrastructure#createCa(ConcurrentLinkedQueue)}.
   */
  @Test
  void test_createCa__Queue() { // NOPMD '_' character in name of method
    // Assertions:
    // ... a. createRootCa(...)-method works as expected

    // Test strategy:
    // --- a. create one CA, check created artifacts
    // --- b. ERROR: too few arguments
    // --- c. ERROR: CA already exists

    try {
      // --- create RootCA
      final String rootCa = "caRCA";
      assertTrue(
          PublicKeyInfrastructure.createRootCa(new ConcurrentLinkedQueue<>(List.of(
              rootCa
          )))
      );

      // --- a. create CA, check created artifacts
      final String commonNameA = "cnA";
      assertTrue(
          PublicKeyInfrastructure.createCa(new ConcurrentLinkedQueue<>(List.of(
              commonNameA, rootCa
          )))
      );
      checkEntity(
          PublicKeyInfrastructure
              .claPkiBasePath
              .resolve(rootCa)
              .resolve(commonNameA),
          AfiElcParameterSpec.brainpoolP384r1
      );

      // --- b. ERROR: too few arguments
      assertFalse(
          PublicKeyInfrastructure.createCa(new ConcurrentLinkedQueue<>(List.of(
              commonNameA
          )))
      );

      // --- c. ERROR: CA already exists
      {
        final Throwable throwable = assertThrows(
            IllegalArgumentException.class,
            () -> PublicKeyInfrastructure.createCa(new ConcurrentLinkedQueue<>(List.of(
                commonNameA, rootCa
            )))
        );
        assertEquals(ENTITY_EXISTS, throwable.getMessage());
        assertNull(throwable.getCause());
      }
    } catch (Exception e) { // NOPMD generic exceptions, spotbugs: REC_CATCH_EXCEPTION
      fail(UNEXPECTED, e);
    } // end catch(Exception)
  } // end method */

  /**
   * Test method for {@link PublicKeyInfrastructure#createDirectory(Path)}.
   */
  @Test
  void test_createDirectory__Path() throws IOException { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. happy case directly in PKI_BASE_PATH
    // --- b. happy case in directory created in step a.
    // --- c. ERROR: directory already exists
    // --- d. ERROR: file exists
    // --- e. ERROR: parent absent

    // --- a. happy case directly in PKI_BASE_PATH
    final Path directoryA = PublicKeyInfrastructure.claPkiBasePath.resolve("a");
    PublicKeyInfrastructure.createDirectory(directoryA);
    assertTrue(Files.isDirectory(directoryA));

    // --- b. happy case in directory created in step a.
    final Path directoryB = directoryA.resolve("dir.b");
    PublicKeyInfrastructure.createDirectory(directoryB);
    assertTrue(Files.isDirectory(directoryB));

    // --- c. ERROR: directory already exists
    // --- d. ERROR: file exists
    final Path fileD = directoryA.resolve("file.d");
    Files.write(fileD, AfiUtils.EMPTY_OS);
    assertTrue(Files.isRegularFile(fileD));
    List.of(
        directoryA,
        directoryB,
        fileD
    ).forEach(path -> {
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> PublicKeyInfrastructure.createDirectory(path)
      );
      assertEquals(ENTITY_EXISTS, throwable.getMessage());
      assertNull(throwable.getCause());
    }); // end forEach(path -> ...)

    // --- e. ERROR: parent absent
    {
      final Path directoryE = fileD.resolve("dir.e");
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> PublicKeyInfrastructure.createDirectory(directoryE)
      );
      assertEquals("CA absent", throwable.getMessage());
      assertNull(throwable.getCause());
    }
  } // end method */

  /**
   * Test method for {@link PublicKeyInfrastructure#createEndEntity(ConcurrentLinkedQueue)}.
   */
  @Test
  void test_createEndEntity__Queue() { // NOPMD '_' character in name of method
    // Assertions:
    // ... a. createRootCa(...)-method works as expected
    // ... b. createCa(...)-method works as expected

    // Test strategy:
    // --- a. create one EndEntity, check created artifacts
    // --- b. ERROR: too few arguments
    // --- c. ERROR: EndEntity already exists

    try {
      // --- create RootCA
      final String rootCa = "eeRCA";
      assertTrue(
          PublicKeyInfrastructure.createRootCa(new ConcurrentLinkedQueue<>(List.of(
              rootCa
          )))
      );

      // --- create CA
      final String cnCa = "CA";
      assertTrue(
          PublicKeyInfrastructure.createCa(new ConcurrentLinkedQueue<>(List.of(
              cnCa, rootCa
          )))
      );

      // --- a. create one EndEntity, check created artifacts
      final String commonNameA = "cnA";
      assertTrue(
          PublicKeyInfrastructure.createEndEntity(new ConcurrentLinkedQueue<>(List.of(
              commonNameA, cnCa
          )))
      );
      checkEntity(
          PublicKeyInfrastructure
              .claPkiBasePath
              .resolve(rootCa)
              .resolve(cnCa)
              .resolve(commonNameA),
          AfiElcParameterSpec.brainpoolP256r1
      );

      // --- b. ERROR: too few arguments
      assertFalse(
          PublicKeyInfrastructure.createEndEntity(new ConcurrentLinkedQueue<>(List.of(
              commonNameA
          )))
      );

      // --- c. ERROR: EndEntity already exists
      {
        final Throwable throwable = assertThrows(
            IllegalArgumentException.class,
            () -> PublicKeyInfrastructure.createCa(new ConcurrentLinkedQueue<>(List.of(
                commonNameA, cnCa
            )))
        );
        assertEquals(ENTITY_EXISTS, throwable.getMessage());
        assertNull(throwable.getCause());
      }
    } catch (Exception e) { // NOPMD generic exceptions, spotbugs: REC_CATCH_EXCEPTION
      fail(UNEXPECTED, e);
    } // end catch(Exception)
  } // end method */

  /**
   * Test method for {@link PublicKeyInfrastructure#createRootCa(ConcurrentLinkedQueue)}.
   */
  @Test
  void test_createRootCa__Queue() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. create one RootCA, check created artifacts
    // --- b. ERROR: empty arguments
    // --- c. ERROR: RootCA already exists

    try {
      // --- a. create one RootCA, check created artifacts
      final String commonNameA = "cnA";
      assertTrue(
          PublicKeyInfrastructure.createRootCa(new ConcurrentLinkedQueue<>(List.of(
              commonNameA
          )))
      );
      checkEntity(
          PublicKeyInfrastructure
              .claPkiBasePath
              .resolve(commonNameA),
          AfiElcParameterSpec.brainpoolP512r1
      );

      // --- b. ERROR: empty arguments
      assertFalse(
          PublicKeyInfrastructure.createRootCa(new ConcurrentLinkedQueue<>(new ArrayList<>()))
      );

      // --- c. ERROR: RootCA already exists
      {
        final Throwable throwable = assertThrows(
            IllegalArgumentException.class,
            () -> PublicKeyInfrastructure.createRootCa(new ConcurrentLinkedQueue<>(List.of(
                commonNameA
            )))
        );
        assertEquals(ENTITY_EXISTS, throwable.getMessage());
        assertNull(throwable.getCause());
      }
    } catch (Exception e) { // NOPMD generic exceptions, spotbugs: REC_CATCH_EXCEPTION
      fail(UNEXPECTED, e);
    } // end catch(Exception)
  } // end method */

  /**
   * Test method for {@link PublicKeyInfrastructure#signEcdsa(byte[], ECPrivateKey)}.
   */
  @Test
  void test_signEcdsa__byteA_EcPrivateKey() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. loop over all domain parameter predefined in AfiElcParameterSpec
    AfiElcParameterSpec.PREDEFINED.stream()
        .parallel() // for performance boost
        .forEach(domainParameter -> {
          final int tau = domainParameter.getTau();
          final int maxBitLength = tau << 3;

          final String algorithm;
          if (32 == tau) { // NOPMD literal in conditional statement
            algorithm = "SHA256withECDSA";
          } else if (48 == tau) { // NOPMD literal in conditional statement
            algorithm = "SHA384withECDSA";
          } else {
            algorithm = "SHA512withECDSA";
          } // end else

          RNG.intsClosed(0, 100, 20).forEach(length -> {
            try {
              // --- create key pair
              final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
              keyPairGenerator.initialize(domainParameter);
              final KeyPair keyPair = keyPairGenerator.generateKeyPair();
              final ECPrivateKey prk = (ECPrivateKey) keyPair.getPrivate();
              final ECPublicKey  puk = (ECPublicKey)  keyPair.getPublic();
              final byte[] message = RNG.nextBytes(length);

              // --- create signature
              final DerBitString sigA = PublicKeyInfrastructure.signEcdsa(
                  message,
                  prk
              );

              // --- check R and S within created signature
              final DerSequence sigB = (DerSequence) BerTlv.getInstance(sigA.getDecoded());
              final List<BerTlv> sigC = sigB.getDecoded();
              assertEquals(2, sigC.size());

              final BigInteger biR = ((DerInteger) sigC.get(0)).getDecoded();
              final BigInteger biS = ((DerInteger) sigC.get(1)).getDecoded();

              assertTrue(biR.bitLength() <= maxBitLength);
              assertTrue(biS.bitLength() <= maxBitLength);

              // --- verify signature
              final Signature verifier = Signature.getInstance(algorithm);
              verifier.initVerify(puk);
              verifier.update(message);
              assertTrue(verifier.verify(sigA.getDecoded()));
            } catch (Exception e) { // NOPMD generic exceptions, spotbugs: REC_CATCH_EXCEPTION
              fail(UNEXPECTED, e);
            } // end catch (...)
          }); // end forEach(length -> ...)
        }); // end forEach(domainParameter -> ...)
  } // end method */

  /**
   * Test method for {@link PublicKeyInfrastructure#verifyEcdsa(byte[], DerBitString, ECPublicKey)}.
   */
  @Test
  void test_verifyEcdsa__byteA__DerBitString_EcPublicKey() { // NOPMD '_' character in name
    // Test strategy:
    // --- a. loop over all domain parameter predefined in AfiElcParameterSpec
    AfiElcParameterSpec.PREDEFINED.stream()
        .parallel() // for performance boost
        .forEach(domainParameter -> {
          final int tau = domainParameter.getTau();

          final String algorithm;
          if (32 == tau) { // NOPMD literal in conditional statement
            algorithm = "SHA256withECDSA";
          } else if (48 == tau) { // NOPMD literal in conditional statement
            algorithm = "SHA384withECDSA";
          } else {
            algorithm = "SHA512withECDSA";
          } // end else

          RNG.intsClosed(0, 100, 20).forEach(length -> {
            try {
              // --- create key pair
              final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
              keyPairGenerator.initialize(domainParameter);
              final KeyPair keyPair = keyPairGenerator.generateKeyPair();
              final ECPrivateKey prk = (ECPrivateKey) keyPair.getPrivate();
              final ECPublicKey  puk = (ECPublicKey)  keyPair.getPublic();
              final byte[] message = RNG.nextBytes(length);

              // --- create signature
              final Signature signer = Signature.getInstance(algorithm);
              signer.initSign(prk);
              signer.update(message);
              final DerBitString signature = new DerBitString(signer.sign());

              // --- verify signature
              assertTrue(PublicKeyInfrastructure.verifyEcdsa(message, signature, puk));
            } catch (Exception e) { // NOPMD generic exceptions, spotbugs: REC_CATCH_EXCEPTION
              fail(UNEXPECTED, e);
            } // end catch (...)
          }); // end forEach(length -> ...)
        }); // end forEach(domainParameter -> ...)
  } // end method */

  @Test
  void createEntity() { // NOPMD '_' character in name of method
    // FIXME
  } // end method */

  @Test
  void createKeyStore() { // NOPMD '_' character in name of method
    // FIXME
  } // end method */

  @Test
  void getCommonName() { // NOPMD '_' character in name of method
    // FIXME
  } // end method */

  @Test
  void getPath() { // NOPMD '_' character in name of method
    // FIXME
  } // end method */

  @Test
  void getPrivateKey() { // NOPMD '_' character in name of method
    // FIXME
  } // end method */

  @Test
  void misc() { // NOPMD '_' character in name of method
    // FIXME
  } // end method */

  /**
   * Checks artifacts of an entity.
   *
   * <p>Entity here is one of:
   * <ol>
   *   <li>RootCA, has only self-signed X.509 certificate.
   *   <li>CA, i.e. Certificate Authority with an X.509 certificate issued by
   *       another CA, typically the RootCA.
   *   <li>End-Entity, i.e. an entity with an X.509 certificate issued by
   *       a CA.
   * </ol>
   *
   * @param directory     where entity is expected
   * @param parameterSpec expected domain parameter
   */
  private void checkEntity(
      final Path directory,
      final AfiElcParameterSpec parameterSpec
  ) {
    // Assertions:
    // ... a. getPrivateKey(...)-method works as expected

    // Test strategy
    // --- a. check whether expected artifacts exists
    checkEntityA(directory);
    
    // --- b. check private key
    checkEntityB(directory, parameterSpec);
    
    // --- c. check public key
    checkEntityC(directory, parameterSpec);
    
    // --- d. check self signed X.509
    // --- e. check x.509 from CA
    // --- f. check compact certificate
    // FIXME
  } // end method */

  /**
   * Checks artifacts of an entity.
   *
   * <p>Entity here is one of:
   * <ol>
   *   <li>RootCA, has only self-signed X.509 certificate.
   *   <li>CA, i.e. Certificate Authority with an X.509 certificate issued by
   *       another CA, typically the RootCA.
   *   <li>End-Entity, i.e. an entity with an X.509 certificate issued by
   *       a CA.
   * </ol>
   *
   * @param directory where entity is expected
   */
  private void checkEntityA(
      final Path directory
  ) {
    final String commonName = directory
        .getFileName() // spotbugs: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
        .toString();

    try {
      // --- a. check whether expected artifacts exists
      // --- check if directory exists
      assertTrue(Files.isDirectory(directory));

      // --- check if all artifacts exist
      List.of(
          PublicKeyInfrastructure.SUFFIX_KEYSTORE_PRIVATE,
          PublicKeyInfrastructure.SUFFIX_KEYSTORE_X509,
          PublicKeyInfrastructure.SUFFIX_PRIVATE_KEY + Utils.EXTENSION_BIN,
          PublicKeyInfrastructure.SUFFIX_PRIVATE_KEY + Utils.EXTENSION_BIN_TEXT,
          PublicKeyInfrastructure.SUFFIX_PRIVATE_KEY + Utils.EXTENSION_BIN_EXPLANATION,
          PublicKeyInfrastructure.SUFFIX_PUBLIC_KEY + Utils.EXTENSION_BIN,
          PublicKeyInfrastructure.SUFFIX_PUBLIC_KEY + Utils.EXTENSION_BIN_TEXT,
          PublicKeyInfrastructure.SUFFIX_PUBLIC_KEY + Utils.EXTENSION_BIN_EXPLANATION,
          PublicKeyInfrastructure.SUFFIX_SELF_SIGNED + Utils.EXTENSION_BIN,
          PublicKeyInfrastructure.SUFFIX_SELF_SIGNED + Utils.EXTENSION_BIN_TEXT,
          PublicKeyInfrastructure.SUFFIX_SELF_SIGNED + Utils.EXTENSION_BIN_EXPLANATION,
          PublicKeyInfrastructure.SUFFIX_X509 + Utils.EXTENSION_BIN,
          PublicKeyInfrastructure.SUFFIX_X509 + Utils.EXTENSION_BIN_TEXT,
          PublicKeyInfrastructure.SUFFIX_X509 + Utils.EXTENSION_BIN_EXPLANATION
      ).forEach(name -> {
        assertTrue(Files.isRegularFile(directory.resolve(commonName + name)));
      }); // end forEach(name -> ...)
    } catch (Exception e) { // NOPMD generic exceptions, spotbugs: REC_CATCH_EXCEPTION
      fail(UNEXPECTED, e);
    } // end catch(Throwable)
  } // end method */

  /**
   * Checks artifacts of an entity.
   *
   * <p>Entity here is one of:
   * <ol>
   *   <li>RootCA, has only self-signed X.509 certificate.
   *   <li>CA, i.e. Certificate Authority with an X.509 certificate issued by
   *       another CA, typically the RootCA.
   *   <li>End-Entity, i.e. an entity with an X.509 certificate issued by
   *       a CA.
   * </ol>
   *
   * @param directory     where entity is expected
   * @param parameterSpec expected domain parameter
   */
  private void checkEntityB(
      final Path directory,
      final AfiElcParameterSpec parameterSpec
  ) {
    final String commonName = directory
        .getFileName() // spotbugs: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
        .toString();

    try {
      // --- b. check private key
      final DerSequence prkTlv = (DerSequence) BerTlv.getInstance(Files.readAllBytes(
          directory.resolve(
              commonName
                  + PublicKeyInfrastructure.SUFFIX_PRIVATE_KEY
                  + Utils.EXTENSION_BIN
          )
      ));
      
      // b.1 check TLV-structure
      // b.2 check against private key from key-store

      // b.1 check TLV-structure
      assertEquals(// check text output
          prkTlv.toString(" ", "|  "),
          Files.readString(directory.resolve(
              commonName
                  + PublicKeyInfrastructure.SUFFIX_PRIVATE_KEY
                  + Utils.EXTENSION_BIN_TEXT
          ))
      );
      assertEquals(// check tree structure output
          prkTlv.toStringTree(),
          Files.readString(directory.resolve(
              commonName
                  + PublicKeyInfrastructure.SUFFIX_PRIVATE_KEY
                  + Utils.EXTENSION_BIN_EXPLANATION
          ))
      );

      // check TLV-structure
      final List<BerTlv> valueField = prkTlv.getDecoded();
      assertEquals(0, ((DerInteger) valueField.get(0)).getDecoded().intValueExact());
      final List<BerTlv> value2 = ((DerSequence) valueField.get(1)).getDecoded();
      assertEquals(AfiOid.ecPublicKey, ((DerOid) value2.get(0)).getDecoded());
      assertEquals(parameterSpec.getOid(), ((DerOid) value2.get(1)).getDecoded());
      final byte[] octetA = ((DerOctetString) valueField.get(2)).getDecoded();
      final List<BerTlv> value3 = ((DerSequence) BerTlv.getInstance(octetA)).getDecoded();
      assertEquals(1, ((DerInteger) value3.get(0)).getDecoded().intValueExact());
      final byte[] octetB = ((DerOctetString) value3.get(1)).getDecoded();

      // b.2 check against private key from key-store
      final KeyStore keyStorePrivate = KeyStore.getInstance(
          directory
              .resolve(commonName + PublicKeyInfrastructure.SUFFIX_KEYSTORE_PRIVATE)
              .toFile(),
          PublicKeyInfrastructure.KEYSTORE_PASSWORD
      );
      final ECPrivateKey prk = (ECPrivateKey) keyStorePrivate.getKey(
          commonName, PublicKeyInfrastructure.KEYSTORE_PASSWORD
      );
      assertNotNull(prk);
      assertEquals(
          new BigInteger(1, octetB),
          prk.getS()
      );
    } catch (Exception e) { // NOPMD generic exceptions, spotbugs: REC_CATCH_EXCEPTION
      fail(UNEXPECTED, e);
    } // end catch(Throwable)
  } // end method */

  /**
   * Checks artifacts of an entity.
   *
   * <p>Entity here is one of:
   * <ol>
   *   <li>RootCA, has only self-signed X.509 certificate.
   *   <li>CA, i.e. Certificate Authority with an X.509 certificate issued by
   *       another CA, typically the RootCA.
   *   <li>End-Entity, i.e. an entity with an X.509 certificate issued by
   *       a CA.
   * </ol>
   *
   * @param directory where entity is expected
   * @param parameterSpec expected domain parameter
   */
  private void checkEntityC(
      final Path directory,
      final AfiElcParameterSpec parameterSpec
  ) {
    final String commonName = directory
        .getFileName() // spotbugs: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
        .toString();

    try {
      // --- c. check public key
      final DerSequence pukTlv = (DerSequence) BerTlv.getInstance(Files.readAllBytes(
          directory.resolve(
              commonName
                  + PublicKeyInfrastructure.SUFFIX_PUBLIC_KEY
                  + Utils.EXTENSION_BIN
          )
      ));
      
      // c.1 check TLV-structure
      assertEquals(// check text output
          pukTlv.toString(" ", "|  "),
          Files.readString(directory.resolve(
              commonName
                  + PublicKeyInfrastructure.SUFFIX_PUBLIC_KEY
                  + Utils.EXTENSION_BIN_TEXT
          ))
      );
      assertEquals(// check tree structure output
          pukTlv.toStringTree(),
          Files.readString(directory.resolve(
              commonName
                  + PublicKeyInfrastructure.SUFFIX_PUBLIC_KEY
                  + Utils.EXTENSION_BIN_EXPLANATION
          ))
      );

      // check TLV-structure
      final List<BerTlv> valueField = pukTlv.getDecoded();
      final DerSequence oids = (DerSequence) valueField.get(0);
      final List<BerTlv> value2 = oids.getDecoded();
      assertEquals(AfiOid.ecPublicKey, ((DerOid) value2.get(0)).getDecoded());
      assertEquals(parameterSpec.getOid(), ((DerOid) value2.get(1)).getDecoded());
      final byte[] octetsPuk = ((DerBitString) valueField.get(1)).getDecoded();

      // c.2 check against public key from key-store
      final KeyStore keyStorePublic = KeyStore.getInstance(
          directory
              .resolve(commonName + PublicKeyInfrastructure.SUFFIX_KEYSTORE_X509)
              .toFile(),
          PublicKeyInfrastructure.KEYSTORE_PASSWORD
      );
      final X509Certificate x509 = (X509Certificate) keyStorePublic.getCertificate(
          commonName
      );
      assertNotNull(x509);
      final ECPublicKey puk = (ECPublicKey) x509.getPublicKey();
      assertEquals(parameterSpec, AfiElcParameterSpec.getInstance(puk.getParams()));
      assertEquals(
          Hex.toHexDigits(octetsPuk),
          Hex.toHexDigits(AfiElcUtils.p2osUncompressed(puk.getW(), parameterSpec))
      );
    } catch (Exception e) { // NOPMD generic exceptions, spotbugs: REC_CATCH_EXCEPTION
      fail(UNEXPECTED, e);
    } // end catch(Throwable)
  } // end method */
} // end class
