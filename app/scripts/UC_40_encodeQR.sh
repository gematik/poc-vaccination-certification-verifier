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

# Description (short): Performs use case UC_40_encodeQR.
#        For more information see
#        a. ../README.sh and
#        b. CmdLine.ACTION_QR_ENCODE
# Usage: ./UC_40_encode_QR.sh prefix

# Assertions:
# ... a. The script for calling the app is created by the following gradle command:
#        ./gradlew build installDist
# ... b. From assertion a it follows that the script for running the application
#        is installed (relatively) to the folder with this script:
#        ../build/install/app/bin

# --- Define some constants
ACTION="--QR-encode"

# --- check command line parameter
if [ ! 1 -eq $# ]; then
  echo "  ERROR: one parameter shall be present: prefix"
  echo "         Usage: $0 prefix"
  exit 12
fi # end if
# ... exact one argument is present
prefix="$1"

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
  # echo "app present"
  "${APP}" $ACTION "${prefix}"
else
  echo "app absent"
fi # end else

echo "done"
