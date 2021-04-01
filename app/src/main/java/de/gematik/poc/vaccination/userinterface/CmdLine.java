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

package de.gematik.poc.vaccination.userinterface;

import com.gmail.alfred65fiedler.utils.AfiUtils;
import de.gematik.poc.vaccination.certvac.Checker;
import de.gematik.poc.vaccination.certvac.CreatorOfProof;
import de.gematik.poc.vaccination.certvac.InformationOfProof;
import de.gematik.poc.vaccination.certvac.InformationOfVaccination;
import de.gematik.poc.vaccination.pki.CborSigner;
import de.gematik.poc.vaccination.pki.PublicKeyInfrastructure;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a (rather simple) user interface for the Vaccination System.
 *
 * <p>In particular: Currently the Vaccination System consists of the following
 * sub-systems:
 * <ol>
 *   <li>PKI: Public key infrastructure for handling asymmetric key pairs
 *       and their PKI-certificates,
 *   <li>Vaccination center: A place where vaccination shots are performed and
 *       vaccination-certificates are issued,
 *   <li>Checkpoint: A place where vaccination-information is checked.
 * </ol>
 *
 * <p>Currently, in order to simplify things,  all the functionality is called from
 * {@link #main(String[])}-method with appropriate command-line parameters.
 */
public final class CmdLine { // NOPMD
  /**
   * Path used for storing information.
   */
  public static final Path BASE_PATH = Paths.get(System.getProperty("user.home"))
      .resolve(("vaccination.poc")); // */

  /**
   * Time zone used within the system, here UTC.
   */
  public static final ZoneId TIME_ZONE = ZoneId.of("Z"); // */

  /**
   * Action: Create information for Certificate of Vaccination.
   */
  public static final String ACTION_CEROVAC_CREATE = "--CoV-CreateCeroVac"; // */

  /**
   * Action: Create {@link InformationOfProof}.
   */
  public static final String ACTION_INFOPROOF_CREATE = "--IoP_Create"; // */

  /**
   * Action: Decode {@link InformationOfProof}.
   */
  public static final String ACTION_INFOPROOF_DECODE = "--IoP_Decode"; // */

  /**
   * Action: Sign {@link InformationOfProof}.
   */
  public static final String ACTION_INFOPROOF_SIGN = "--IoP_Sign"; // */

  /**
   * Action: Verify signature for {@link InformationOfProof}.
   */
  public static final String ACTION_INFOPROOF_VERIFY = "--IoP_Verify"; // */

  /**
   * Action: Create Root-CA.
   */
  public static final String ACTION_PKI_ROOT_CA = "--PKI-CreateRootCA"; // */

  /**
   * Action: Create certification authority (CA).
   */
  public static final String ACTION_PKI_CA = "--PKI-CreateCA"; // */

  /**
   * Action: Create a compact certificate for an existing entity.
   */
  public static final String ACTION_PKI_COMPACT = "--PKI-CreateCompact"; // */

  /**
   * Action: Create end-entity (EE).
   */
  public static final String ACTION_PKI_ENTITY = "--PKI-CreateEndEntity"; // */

  /**
   * Action: Encode text into QR-code.
   */
  public static final String ACTION_QR_ENCODE = "--QR-encode"; // */

  /**
   * Action: Decode QR-code into text.
   */
  public static final String ACTION_QR_DECODE = "--QR-decode"; // */

  /**
   * Logger.
   */
  public static final Logger LOGGER = getLogger(); // */

  /**
   * Default constructor.
   *
   * <p>The visibility is {@code private} because this is a utility class.
   * Thus instantiating of objects is prevented.
   */
  private CmdLine() {
    // intentionally empty
  } // end constructor */

  /**
   * Configures logging and creates an appropriate logger.
   *
   * @return logger
   */
  private static Logger getLogger() {
    // --- set log-level, an element from set {"trace", "debug", "info", "warn", "error" or "off"}
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");

    // --- set output target, either "System.out" or "System.err" or path to file
    System.setProperty(
        "org.slf4j.simpleLogger.logFile",
        new String[]{
            "System.out", // index = 0
            "System.err", // index = 1
            "build/log.txt" // index = 2
        }[0]
    );

    // --- set whether date and time is shown in output message, default is false
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");

    // --- set date and time format, see java/text/SimpleDateFormat
    System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "hh-mm-ss");

    return LoggerFactory.getLogger(CmdLine.class);
  } // end method */

  /**
   * Main-method used for all software based actions.
   *
   * <p>Currently the following functions are available:
   * <ol>
   *   <li>TODO
   * </ol>
   *
   * @param args command line arguments
   */
  public static void main(// NOPMD high complexity
      final String[] args
  ) {
    LOGGER.atInfo().log("start CmdLine");
    final long startTime = System.currentTimeMillis();

    try {
      // --- copy command line arguments to queue
      final ConcurrentLinkedQueue<String> arguments = new ConcurrentLinkedQueue<>(
          Arrays.asList(args)
      );

      if (arguments.isEmpty()) {
        // ... no command line arguments
        //     => show usage
        showUsage();
      } else {
        // ... at least one command line argument present

        final String mainAction = arguments.remove();
        switch (mainAction) {
          case ACTION_CEROVAC_CREATE:
            InformationOfVaccination.createCeroVacInfo(arguments);
            break;

          // Info of Proof actions _____________________________________________
          case ACTION_INFOPROOF_CREATE:
            InformationOfProof.create(arguments);
            break;

          case ACTION_INFOPROOF_DECODE:
            Checker.decodeMessage(arguments);
            break;

          case ACTION_INFOPROOF_SIGN:
            CreatorOfProof.signProof(arguments);
            break;

          case ACTION_INFOPROOF_VERIFY:
            Checker.verifySignature(arguments);
            break;

          // 2D-barcode actions ________________________________________________
          case ACTION_QR_DECODE:
            Checker.decodeQrCode(arguments);
            break;

          case ACTION_QR_ENCODE:
            CreatorOfProof.createBarcode(arguments);
            break;

          // PKI actions _______________________________________________________
          case ACTION_PKI_CA:
            PublicKeyInfrastructure.createCa(arguments);
            break;

          case ACTION_PKI_COMPACT:
            CborSigner.createCompactCertificate(arguments);
            break;

          case ACTION_PKI_ENTITY:
            PublicKeyInfrastructure.createEndEntity(arguments);
            break;

          case ACTION_PKI_ROOT_CA:
            PublicKeyInfrastructure.createRootCa(arguments);
            break;

          case "misc":
            // Action miscellaneous, i.e. other actions, experiments, hacking
            // TODO remove before release
            break;

          default:
            // ... first command line argument does not indicate a valid action
            LOGGER.info("first command line argument = {}", mainAction);

            showUsage();
            break;
        } // end switch (first argument)
      } // end else (at least one command line argument?)
    } catch (RuntimeException e) { // NOPMD avoid catching generic exceptions
      LOGGER.error("unexpected exception", e);
      System.exit(1);
    } catch (Exception e) { // NOPMD avoid catching generic exceptions
      LOGGER.error("unexpected exception", e);
      System.exit(1);
    } // end catch(Exception)

    final long endTime = System.currentTimeMillis();
    LOGGER.atInfo().log(
        "runtime: {}",
        AfiUtils.milliSeconds2Time(endTime - startTime)
    );
  } // end method */

  /**
   * Show usage.
   */
  public static void showUsage() {
    final String newLine = System.lineSeparator();

    LOGGER.atInfo().log(
        List.of(
            "PKI",
            ACTION_PKI_ROOT_CA,
            ACTION_PKI_CA,
            ACTION_PKI_ENTITY,
            "Other",
            ACTION_CEROVAC_CREATE,
            ACTION_INFOPROOF_CREATE,
            ACTION_QR_ENCODE,
            ACTION_QR_DECODE
        ).stream()
            .collect(Collectors.joining(
                " ..." + newLine + "  ", // delimiter
                newLine + newLine + "Usage:" + newLine + "  ", // prefix
                newLine + newLine      // suffix
            ))
    );
  } // end method */
} // end class
