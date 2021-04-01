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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import co.nstant.in.cbor.CborException;
import com.gmail.alfred65fiedler.crypto.AfiElcParameterSpec;
import com.gmail.alfred65fiedler.tlv.DerBitString;
import com.gmail.alfred65fiedler.tlv.DerInteger;
import com.gmail.alfred65fiedler.tlv.DerSequence;
import com.gmail.alfred65fiedler.utils.AfiBigInteger;
import com.gmail.alfred65fiedler.utils.AfiRng;
import com.gmail.alfred65fiedler.utils.AfiUtils;
import com.gmail.alfred65fiedler.utils.Hex;
import de.gematik.poc.vaccination.userinterface.CmdLine;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box test on {@link CborSigner}.
 */
final class TestCborSigner {
  /**
   * Random Number Generator.
   */
  private static final AfiRng RNG = new AfiRng(); // */

  /**
   * Message for unexpected exceptions.
   */
  private static final String UNEXPECTED = "unexpected exception"; // */

  /**
   * Common name of Root-CA.
   */
  private static final String CN_ROOT_CA = "cnRCA"; // */

  /**
   * Common name of CA.
   */
  private static final String CN_CA = "4711"; // */

  /**
   * Common names of end-entities.
   */
  private static final List<String> CN_EE = List.of(
      "cnEE1", "cnEE2", "cnEE3",
      "cnEEa", "cnEEb", "cnEEc"
  );

  /**
   * Method executed before other tests.
   */
  @BeforeAll
  static void setUpBeforeClass() throws
      CertificateException,
      IOException,
      InvalidAlgorithmParameterException,
      InvalidKeyException,
      KeyStoreException,
      NoSuchAlgorithmException,
      SignatureException,
      UnrecoverableKeyException {
    // --- set the base path for PKI stuff to temporary test directory
    final Path tempDir = CmdLine
        .BASE_PATH
        .resolve(("pki"))
        .resolveSibling("junit.pki")
        .resolve(String.format("%016x", System.nanoTime()));
    PublicKeyInfrastructure.claPkiBasePath = Files.createDirectories(tempDir);
    assertTrue(Files.exists(PublicKeyInfrastructure.claPkiBasePath));
    assertTrue(Files.isDirectory(PublicKeyInfrastructure.claPkiBasePath));

    // --- create cryptographic entities
    PublicKeyInfrastructure.createRootCa(new ConcurrentLinkedQueue<>(List.of(
        CN_ROOT_CA
    )));
    PublicKeyInfrastructure.createCa(new ConcurrentLinkedQueue<>(List.of(
        CN_CA, CN_ROOT_CA
    )));
    for (final String cn : CN_EE) {
      PublicKeyInfrastructure.createEndEntity(new ConcurrentLinkedQueue<>(List.of(// NOPMD new loop
          cn, CN_CA
      )));
    } // end for (cn...)
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
   * Test method for {@link CborSigner#aggregateCertificateElements(byte[], byte[])}.
   */
  @Test
  void test_aggregateCertificate__byteA_byteA() { // NOPMD '_' character in name of method
    // Assertions:
    // ... a. extractContent(byte[])-method works as expected

    // Test strategy:
    // --- a. manually defined input
    // --- b. randomly chosen input

    // --- a. manually defined input
    List.of(
        List.of("0123", "abcdef", "82-[(42-0123)(43-abcdef)]")
    ).forEach(input -> {
      try {
        final byte[] message = Hex.toByteArray(input.get(0));
        final byte[] signature = Hex.toByteArray(input.get(1));
        final String expected = Hex.extractHexDigits(input.get(2));
        assertEquals(
            expected,
            Hex.toHexDigits(CborSigner.aggregateCertificateElements(message, signature))
        );
      } catch (CborException e) {
        fail(UNEXPECTED, e);
      } // end catch (...)
    }); // end forEach(input -> ...)

    // --- b. randomly chosen input
    RNG.intsClosed(0, 100, 20)
        .parallel()
        .forEach(sizeMessage -> {
          final byte[] message = RNG.nextBytes(sizeMessage);

          RNG.intsClosed(0, 100, 20)
              .parallel()
              .forEach(sizeSignature -> {
                try {
                  final byte[] signature = RNG.nextBytes(sizeSignature);
                  final byte[] cbor = CborSigner.aggregateCertificateElements(message, signature);
                  final List<byte[]> list = CborSigner.extractCertificateElements(cbor);

                  assertEquals(
                      Hex.toHexDigits(message),
                      Hex.toHexDigits(list.get(0))
                  );
                  assertEquals(
                      Hex.toHexDigits(signature),
                      Hex.toHexDigits(list.get(1))
                  );
                } catch (CborException e) {
                  fail(UNEXPECTED, e);
                } // end catch (...)
              }); // end forEach(sizeSignature -> ...)
        }); // end forEach(sizeMessage -> ...)
  } // end method */

  /**
   * Test method for {@link CborSigner#compressSignature(DerBitString, int)}.
   */
  @Test
  void test_compressSignature__DerBitString_int() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. loop over all domain parameter predefined in AfiElcParameterSpec
    AfiElcParameterSpec.PREDEFINED.stream()
        .parallel() // for performance boost
        .forEach(domainParameter -> {
          final int tau = domainParameter.getTau();

          IntStream.range(0, 20).forEach(i -> {
            final BigInteger biR = new BigInteger(1, RNG.nextBytes(1, tau));
            final BigInteger biS = new BigInteger(1, RNG.nextBytes(1, tau));
            final DerBitString sigTlv = new DerBitString(
                new DerSequence(List.of(
                    new DerInteger(biR),
                    new DerInteger(biS)
                )).toByteArray()
            );
            final byte[] sigCompressed = CborSigner.compressSignature(sigTlv, tau);
            assertEquals(
                Hex.toHexDigits(AfiBigInteger.i2os(biR, tau))
                    + Hex.toHexDigits(AfiBigInteger.i2os(biS, tau)),
                Hex.toHexDigits(sigCompressed)
            );
          }); // end forEach(i -> ...)
        }); // end forEach(domainParameter -> ...)
  } // end method */

  /**
   * Test method for {@link CborSigner#expandSignature(byte[])}.
   */
  @Test
  void test_expandSignature__byteA() { // NOPMD '_' character in name of method
    // Test strategy:
    // --- a. happy cases over various lengths
    // --- b. ERROR: odd number of input-octets

    // --- a. happy cases over various lengths
    RNG.intsClosed(32, 64, 20)
        .parallel() // for performance boost
        .forEach(size -> {
          final byte[] octetsR = RNG.nextBytes(size);
          final byte[] octetsS = RNG.nextBytes(size);

          assertEquals(
              new DerBitString(
                  new DerSequence(List.of(
                      new DerInteger(new BigInteger(1, octetsR)),
                      new DerInteger(new BigInteger(1, octetsS))
                  )).toByteArray()
              ),
              CborSigner.expandSignature(AfiUtils.concatenate(octetsR, octetsS))
          );
        }); // end forEach(size -> ...)

    // --- b. ERROR: odd number of input-octets
    {
      RNG.intsClosed(32, 64, 20)
          .parallel()
          .forEach(size -> {
            final Throwable throwable = assertThrows(
                IllegalArgumentException.class,
                () -> CborSigner.expandSignature(RNG.nextBytes((size << 1) + 1))
            );
            assertEquals("odd number of octet in signature R || S", throwable.getMessage());
            assertNull(throwable.getCause());
          }); // end forEach(size -> ...)
    }
  } // end method */

  /**
   * Test method for {@link CborSigner#sign(byte[], String)}.
   */
  @Test
  void test_sign__byteA_String() { // NOPMD '_' character in name of method
    // Assertion:
    // ... a. verify(...)-method works as expected

    // Test strategy:
    // --- a. loop over all keys and sign a couple of messages
    final Set<String> cns = new HashSet<>(CN_EE);
    cns.add(CN_ROOT_CA);
    cns.add(CN_CA);
    cns.stream()
        .parallel()
        .forEach(cn -> {
          RNG.intsClosed(0, 100, 20).forEach(size -> {
            try {
              final byte[] message = RNG.nextBytes(size);
              final byte[] signature = CborSigner.sign(message, cn);

              assertTrue(CborSigner.verify(message, signature, cn));
            } catch (Exception e) { // NOPMD avoid catching generic exceptions
              fail(UNEXPECTED, e);
            } // end catch (Exception)
          }); // end forEach(size -> ...)
        }); // end forEach(cn -> ...)
  } // end method */
} // end class
