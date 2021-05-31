package eu.europa.esig.dss.cades.signature;

import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.signature.DocumentSignatureService;
import eu.europa.esig.dss.utils.Utils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// see DSS-2450
@Tag("slow")
public class CAdESLevelBWithPlainECDSATokenTest extends AbstractCAdESTestSignature {

    private static final String HELLO_WORLD = "Hello World";

    private DocumentSignatureService<CAdESSignatureParameters, CAdESTimestampParameters> service;
    private CAdESSignatureParameters signatureParameters;
    private DSSDocument documentToSign;

    private static Stream<DigestAlgorithm> data() {
        List<DigestAlgorithm> args = new ArrayList<>();

        for (DigestAlgorithm digestAlgo : DigestAlgorithm.values()) {
            SignatureAlgorithm ecCa = SignatureAlgorithm.getAlgorithm(EncryptionAlgorithm.ECDSA, digestAlgo);
            SignatureAlgorithm plainEcCa = SignatureAlgorithm.getAlgorithm(EncryptionAlgorithm.PLAIN_ECDSA, digestAlgo);
            if (ecCa != null && Utils.isStringNotBlank(ecCa.getOid()) && plainEcCa != null && Utils.isStringNotBlank(plainEcCa.getOid())) {
                args.add(digestAlgo);
            }
        }
        return args.stream();
    }

    @ParameterizedTest(name = "Combination {index} of ECDSA with {0}")
    @MethodSource("data")
    public void init(DigestAlgorithm digestAlgo) {
        documentToSign = new InMemoryDocument(HELLO_WORLD.getBytes());

        signatureParameters = new CAdESSignatureParameters();
        signatureParameters.setSigningCertificate(getSigningCert());
        signatureParameters.setCertificateChain(getCertificateChain());
        signatureParameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
        signatureParameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);
        signatureParameters.setDigestAlgorithm(digestAlgo);
        signatureParameters.setEncryptionAlgorithm(EncryptionAlgorithm.ECDSA);

        service = new CAdESService(getOfflineCertificateVerifier());

        super.signAndVerify();
    }

    @Override
    protected DSSDocument sign() {
        ToBeSigned dataToSign = service.getDataToSign(documentToSign, signatureParameters);

        // simulate a token returning PLAIN_ECDSA
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getAlgorithm(EncryptionAlgorithm.PLAIN_ECDSA, signatureParameters.getDigestAlgorithm());
        SignatureValue signatureValue = getToken().sign(dataToSign, signatureAlgorithm, getPrivateKeyEntry());
        assertEquals(signatureAlgorithm, signatureValue.getAlgorithm());
        assertTrue(service.isValidSignatureValue(dataToSign, signatureValue, getSigningCert()));

        return service.signDocument(documentToSign, signatureParameters, signatureValue);
    }

    @Override
    protected void checkEncryptionAlgorithm(DiagnosticData diagnosticData) {
        assertEquals(EncryptionAlgorithm.ECDSA, diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId())
                .getEncryptionAlgorithm());
    }

    @Override
    public void signAndVerify() {
    }

    @Override
    protected DocumentSignatureService<CAdESSignatureParameters, CAdESTimestampParameters> getService() {
        return service;
    }

    @Override
    protected CAdESSignatureParameters getSignatureParameters() {
        return signatureParameters;
    }

    @Override
    protected DSSDocument getDocumentToSign() {
        return documentToSign;
    }

    @Override
    protected String getSigningAlias() {
        return ECDSA_USER;
    }

}
