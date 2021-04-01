#!/bin/bash
#
# Copyright (c) 2021 gematik GmbH
# 
# Licensed under the Apache License, Version 2.0 (the License);
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


# Description (short): Cleans storage directory from any content and prepares
#        file-system for usage by other scrips.
#        For more information, see ../README.md.
# Usage: ./clean.sh

# --- Define constants
# Note: Those constants SHALL be in line with
#       corresponding constants in the Java-code.
BASE_PATH="$HOME/vaccination.poc" # see CmdLine.java
PKI_BASE_PATH="${BASE_PATH}/pki"  # see PublicKeyInfrastructure.java

# see CertificateOfVaccination.java
PATH_UC10="${BASE_PATH}/UC_10_CeroVacInfo"
PATH_UC20="${BASE_PATH}/UC_20_EncodeCeroVacInfo"
PATH_UC30="${BASE_PATH}/UC_30_SignCeroVacInfo"
PATH_UC40="${BASE_PATH}/UC_40_Encode_QR-Code"
PATH_UC50="${BASE_PATH}/UC_50_Decode_QR-Code"
PATH_UC60="${BASE_PATH}/UC_60_VerifySignature"
PATH_UC70="${BASE_PATH}/UC_70_DecodeCeroVacInfo"

# --- do it
echo "remove old content (if present)"  && rm -rf "${BASE_PATH}" && \
echo "create pki-directory" && mkdir -p "${PKI_BASE_PATH}" && \
echo "create directory for UC_10" && mkdir -p "${PATH_UC10}" && \
echo "create directory for UC_20" && mkdir -p "${PATH_UC20}" && \
echo "create directory for UC_30" && mkdir -p "${PATH_UC30}" && \
echo "create directory for UC_40" && mkdir -p "${PATH_UC40}" && \
echo "create directory for UC_50" && mkdir -p "${PATH_UC50}" && \
echo "create directory for UC_60" && mkdir -p "${PATH_UC60}" && \
echo "create directory for UC_70" && mkdir -p "${PATH_UC70}" && \
echo "done"
