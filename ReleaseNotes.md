# Release 0.2.0
Currently, the system consists of the following components:

 1.  PKI: Public Key Infrastructure for generating key pairs and X.509 certificates as well as `java.security.KeyStore` storing those artifacts for later use in other components.
 2.  PoV, Point of Vaccination: Generates certificates of vaccination with lot of (personal) information (not yet present).
 3.  IoP, Information of Proof: A reduced certificate with proof about the health status in respect to certain diseases. This health status certificate contains just enough information to show to a verifier that an individual is no thread regarding certain diseases. Such a proof can rely on
		-   a vaccination,
		-   a negative test or
		-   a recovery from a disease.
 4.  Checkpoint where the status of vaccination is checked (partially present).

