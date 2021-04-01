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

package de.gematik.poc.vaccination.utils;

import com.gmail.alfred65fiedler.tlv.BerTlv;
import de.gematik.poc.vaccination.userinterface.CmdLine;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class contains useful functionality.
 */
public final class Utils { // NOPMD strange name for a utility class
  /**
   * File name extension for binary files.
   */
  public static final String EXTENSION_BIN = ".bin"; // */

  /**
   * File name extension for binary files.
   */
  public static final String EXTENSION_BIN_TEXT = "-bin.txt"; // */

  /**
   * File name extension for binary files.
   */
  public static final String EXTENSION_BIN_EXPLANATION = "-bin_explanation.txt"; // */

  /**
   * Path used by {@code UC_10_CeroVacInfo} to store information.
   */
  public static final Path PATH_UC10 = CmdLine.BASE_PATH.resolve("UC_10_CeroVacInfo"); // */
  /**
   * Path used by {@code UC_20_EncodeCeroVacInfo} to store information.
   */
  public static final Path PATH_UC20 = CmdLine.BASE_PATH.resolve("UC_20_EncodeCeroVacInfo"); // */
  /**
   * Path used by {@code UC_30_SignCeroVacInfo} to store information.
   */
  public static final Path PATH_UC30 = CmdLine.BASE_PATH.resolve("UC_30_SignCeroVacInfo"); // */
  /**
   * Path used by {@code UC_40_Encode_QR-Code} to store information.
   */
  public static final Path PATH_UC40 = CmdLine.BASE_PATH.resolve("UC_40_Encode_QR-Code"); // */
  /**
   * Path used by {@code UC_50_Decode_QR-Code} to store information.
   */
  public static final Path PATH_UC50 = CmdLine.BASE_PATH.resolve("UC_50_Decode_QR-Code"); // */
  /**
   * Path used by {@code UC_60_VerifySignature} to store information.
   */
  public static final Path PATH_UC60 = CmdLine.BASE_PATH.resolve("UC_60_VerifySignature"); // */
  /**
   * Path used by {@code UC_70_DecodeCeroVacInfo} to store information.
   */
  public static final Path PATH_UC70 = CmdLine.BASE_PATH.resolve("UC_70_DecodeCeroVacInfo"); // */

  /**
   * Private default constructor, prevents class from instantiating.
   */
  private Utils() {
    // intentionally empty
  } // end constructor */

  /**
   * Export a TLV-structure.
   *
   * @param storageDirectory directory where artefacts are stored
   * @param fileNamePrefix   prefix of fileName
   * @param object           {@link BerTlv} object to be exported
   *
   * @throws IOException if underlying methods do so
   */
  public static void exportTlv(
      final Path storageDirectory,
      final String fileNamePrefix,
      final BerTlv object
  ) throws IOException {
    // --- export in binary format
    Files.write(
        storageDirectory.resolve(fileNamePrefix + EXTENSION_BIN),
        object.toByteArray()
    );

    // --- export TLV-structure as ASCII-text without explanations
    Files.write(
        storageDirectory.resolve(fileNamePrefix + EXTENSION_BIN_TEXT),
        object.toString(" ", "|  ").getBytes(StandardCharsets.UTF_8)
    );

    // --- export TLV-structure as ASCII-text with explanation
    Files.write(
        storageDirectory.resolve(fileNamePrefix + EXTENSION_BIN_EXPLANATION),
        object.toStringTree().getBytes(StandardCharsets.UTF_8)
    );
  } // end method */
} // end class
