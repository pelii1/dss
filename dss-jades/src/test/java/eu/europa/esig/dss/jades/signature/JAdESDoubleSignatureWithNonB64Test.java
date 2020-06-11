package eu.europa.esig.dss.jades.signature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.enumerations.JWSSerializationType;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.jades.JAdESSignatureParameters;
import eu.europa.esig.dss.jades.JAdESUtils;
import eu.europa.esig.dss.jades.validation.AbstractJAdESTestValidation;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.spi.DSSUtils;

public class JAdESDoubleSignatureWithNonB64Test extends AbstractJAdESTestValidation {
	
	private static final String ORIGINAL_STRING = "Hello World!";
	
	private DSSDocument toBeSigned;
	private JAdESService service;
	
	@BeforeEach
	public void init() {
		toBeSigned = new InMemoryDocument(ORIGINAL_STRING.getBytes());
		
		service = new JAdESService(getOfflineCertificateVerifier());
		service.setTspSource(getGoodTsa());
	}

	@Override
	protected DSSDocument getSignedDocument() {
		DSSDocument signedDocument = getCompleteSerializationSignature(toBeSigned);
		// signedDocument.save("target/" + "signedDocument.json");
		
		try {
			 // avoid same second signature creation
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			fail(e);
		}

		DSSDocument doubleSignedDocument = getCompleteSerializationSignature(signedDocument);
		// doubleSignedDocument.save("target/" + "doubleSignedDocument.json");
		
		assertTrue(JAdESUtils.isJsonDocument(doubleSignedDocument));
		
		assertTrue(new String(DSSUtils.toByteArray(doubleSignedDocument)).contains(ORIGINAL_STRING));
		 
		return doubleSignedDocument;
	}
	
	private DSSDocument getCompleteSerializationSignature(DSSDocument documentToSign) {
		JAdESSignatureParameters signatureParameters = new JAdESSignatureParameters();
		signatureParameters.setSignatureLevel(SignatureLevel.JAdES_BASELINE_B);
		signatureParameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
		signatureParameters.setJwsSerializationType(JWSSerializationType.JSON_SERIALIZATION);
		signatureParameters.setSigningCertificate(getSigningCert());
		signatureParameters.setBase64UrlEncodedPayload(false);
		
		return sign(documentToSign, signatureParameters);
	}
	
	private DSSDocument sign(DSSDocument documentToSign, JAdESSignatureParameters signatureParameters) {
		ToBeSigned dataToSign = service.getDataToSign(documentToSign, signatureParameters);
		SignatureValue signatureValue = getToken().sign(dataToSign, signatureParameters.getDigestAlgorithm(), getPrivateKeyEntry());
		return service.signDocument(documentToSign, signatureParameters, signatureValue);
	}
	
	@Override
	protected void checkSignatureIdentifier(DiagnosticData diagnosticData) {
		super.checkSignatureIdentifier(diagnosticData);

		assertEquals(2, diagnosticData.getSignatureIdList().size());
	}
	
	@Override
	protected void checkBLevelValid(DiagnosticData diagnosticData) {
		super.checkBLevelValid(diagnosticData);
		
		List<SignatureWrapper> signatures = diagnosticData.getSignatures();
		SignatureWrapper signatureOne = signatures.get(0);
		SignatureWrapper signatureTwo = signatures.get(1);
		
		assertEquals(signatureOne.getDigestMatchers().size(), signatureTwo.getDigestMatchers().size());
		assertFalse(Arrays.equals(signatureOne.getDigestMatchers().get(0).getDigestValue(), signatureTwo.getDigestMatchers().get(0).getDigestValue()));
	}
	
	@Test
	public void signWithDifferentB64Test() {
		DSSDocument signedDocument = getCompleteSerializationSignature(toBeSigned);
		
		JAdESSignatureParameters signatureParameters = new JAdESSignatureParameters();
		signatureParameters.setSignatureLevel(SignatureLevel.JAdES_BASELINE_B);
		signatureParameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
		signatureParameters.setJwsSerializationType(JWSSerializationType.JSON_SERIALIZATION);
		signatureParameters.setSigningCertificate(getSigningCert());
		
		Exception exception = assertThrows(DSSException.class, () -> sign(signedDocument, signatureParameters));
		assertEquals("'b64' value shall be the same for all signatures!", exception.getMessage());
	}

	@Override
	protected String getSigningAlias() {
		return GOOD_USER;
	}

}
