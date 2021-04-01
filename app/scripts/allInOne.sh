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

# Description: Performs all implemented use cases for creating and interpreting
#        QR-codes.
#        Intentionally, instead of calling the corresponding scripts the code
#        from those scripts is duplicated here. So, in case you want a new
#        data-set, just edit the attributes here and run this script.

############################################################################
##########          Define constants used in this script          ##########
############################################################################
# --- definitions for use cases creating PKI structure
commonNameRootCa="VaPoC-RootCA.gematik.2021-03-29"
commonNameCa="4711"
commonNameEe="VaPoC-EEa.gematik.2021-03-29"

# --- definitions for use case "create Information of Proof"
prefix="0815"
name="John Doe"
dayOfBirth="1968-05-27"
expirationDate="2021-08-27T15:46:39+00:00[Z]"

# Attempt to set SCRIPTS_HOME, i.e. the directory with this script
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done # end while (...)
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/.." >/dev/null
SCRIPTS_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP="${SCRIPTS_HOME}/build/install/app/bin/app"
if [ -f "${APP}" ] && [ -x "${APP}" ]; then
  # ... app is present
  #     => run the use cases

  # Note 1: By commenting or un-commenting JUST the following line, it is controlled
  #         whether or not PKI entities are created (i.e. RootCA, Ca, EE, ...).
  # Note 2: It is an error to create a PKI entity with the same commonName twice.
  #         If you want to re-create something, delete it first.
  #: '
  echo "create Root-CA" && \
  "${APP}" "--PKI-CreateRootCA"    "${commonNameRootCa}" && \
  echo "create CA" && \
  "${APP}" "--PKI-CreateCA"        "${commonNameCa}" "${commonNameRootCa}" && \
  echo "create EE" && \
  "${APP}" "--PKI-CreateEndEntity" "${commonNameEe}" "${commonNameCa}" && \
  echo "create compact certificate" && \
  "${APP}" "--PKI-CreateCompact"   "${commonNameEe}" "${commonNameCa}" && \
  # '
  echo "create Information of Proof" && \
  "${APP}" "--IoP_Create" "${prefix}" "${name}" "${dayOfBirth}" "${expirationDate}" 0 5 4 1 2 3 && \
  echo "sign Information of Proof" && \
  "${APP}" "--IoP_Sign" "${prefix}" "${commonNameEe}" && \
  echo "create QR-code" && \
  "${APP}" "--QR-encode" "${prefix}" && \
  echo "decode QR-code" && \
  "${APP}" "--QR-decode" "${prefix}" && \
  echo "verify signature" && \
  "${APP}" "--IoP_Verify" "${prefix}" && \
  echo "decode Information of Proof" && \
  "${APP}" "--IoP_Decode" "${prefix}"

else
  echo "app absent"
  exit 12
fi # end else

echo "done"
