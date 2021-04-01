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

/**
 * Module information.
 */
module de.gematik.poc.vaccination {
  // --- exports of this module
  exports de.gematik.poc.vaccination.userinterface;

  // --- requirements of this module
  requires com.github.spotbugs.annotations;             // null-annotations, e.g. package-info.java
  requires transitive com.gmail.alfred65fiedler.crypto; // cryptography
  requires transitive com.gmail.alfred65fiedler.tlv;    // TLV objects
  requires org.slf4j;                                   // logging as substitute for System.out.print

  // QR-code
  requires com.google.zxing;
  requires com.google.zxing.javase;

  requires java.desktop; // for handling images on a desktop PC, on Android this might be different

  // CBOR-support from c-rack
  requires co.nstant.in.cbor;
} // end module
