# Vaccination System

## Overview

Proof of concept for a system issuing and verifying certifications of vaccination.

Currently, the system consists of the following components:

1. PKI: Public Key Infrastructure for generating key pairs and X.509 certificates
   as well as `java.security.KeyStore` storing those artifacts for later use in
   other components.
2. PoV, Point of Vaccination: Generates certificates of vaccination with lot of
   (personal) information
   (not yet present).
3. IoP, Information of Proof: A reduced certificate with proof about the health
   status in respect to certain diseases. This health status certificate
   contains just enough information to show to a verifier that an individual
   is no thread regarding certain diseases. Such a proof can rely on
   - a vaccination,
   - a negative test or
   - a recovery from a disease.
4. Checkpoint where the status of vaccination is checked
   (partially present).
   
## Use Cases

### Public Key Infrastructure

1. PKI_10_createRootCA, creates the root of a PKI structure. This Root-CA
   is used to sign X.509 certificates of other certification authorities.
1. PKI_20_createCA, creates certification authority (CA) and produces an
   X.509 certificate for this CA signed by Root-CA (which is created by
   use case `PKI_10_createRootCA`, see above). Such a CA is used to sign
   PKI certificates (X.509 and compact certificates) for end-entities.
1. PKI_30_createEE, creates an end-entity. Such an end-entity signs 
   certificates of vaccination and certificates with "information of proof".
1. PKI_32_createCompactCertificate, based on an X.509 certificate belonging
   to an end-entity this use case creates a so called "compact certificate".
   Such a compact certificate contains less information and requires thus
   less memory footprint than an X.509 certificate. Such compact certificates
   are used in situations when low memory footprint is desired.

### Certificate of Vaccination

1. UC_10_CeroVacInfo, Collect information about a vaccination, i.e.:
   - Who was inoculated,
   - which vaccine was used,
   - when did the vaccination happen,
   - who performed the inoculation
1. UC_20_EncodeCeroVacInfo, encode information about vaccination, i.e.:
   The information collected in use case `UC_10_CeroVacInfo` is encoded.
1. UC_30_SignCeroVacInfo: The encoded information (output from use case
   `UC_20_EncodeCeroVacInfo`) is digitally signed.
1. UC_40_Encode_QR-Code: Convert an octet string (output from use case
   `UC_30_SignCeroVacInfo`) into a QR-code.
1. UC_50_Decode_QR-Code: A QR-code (output from use case `UC_40_Encode_QR-Code`)
   is decoded such that the octet string (output from use case 
   `UC_30_SignCeroVacInfo`) is recovered.
1. UC_60_VerifySignature: The signature of a signed message is verified. If
   the verification is successful then the original message (è.g. output from
   use case `UC_20_EncodeCeroVacInfo`) is recovered.
1. UC_70_DecodeCeroVacInfo   


# Useful information
1. [QR-code tutorial](https://www.thonky.com/qr-code-tutorial/introduction)
