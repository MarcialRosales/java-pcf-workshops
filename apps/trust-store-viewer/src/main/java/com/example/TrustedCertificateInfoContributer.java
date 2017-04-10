package com.example;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class TrustedCertificateInfoContributer implements InfoContributor {

	private static final Logger log = LoggerFactory.getLogger(TrustedCertificateInfoContributer.class);
	
	@Override
	public void contribute(Builder builder) {
		try {
			builder.withDetail("trustedCerts", readCertificates());
		} catch (NoSuchAlgorithmException e) {
			log.error("Unable to read certificates", e);
		} catch (KeyStoreException e) {
			log.error("Unable to read certificates", e);
		}

	}

	private List<JsonCertificate> readCertificates() throws NoSuchAlgorithmException, KeyStoreException {
		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		List<X509Certificate> x509Certificates = new ArrayList<>();
		trustManagerFactory.init((KeyStore) null);
		Arrays.asList(trustManagerFactory.getTrustManagers()).stream().forEach(t -> {
			x509Certificates.addAll(Arrays.asList(((X509TrustManager) t).getAcceptedIssuers()));
		});
		
		return x509Certificates.stream().map(c -> new JsonCertificate(c)).collect(Collectors.toList());
	}
	
	
}

class JsonCertificate {
	String serialNumber;
	String issuer;
	String notAfter;
	String notBefore;
	String subject;
	List<String> alternateSubject = new ArrayList<>();
	
	public  JsonCertificate(X509Certificate cert)  {
		this.serialNumber = String.valueOf(cert.getSerialNumber());
		this.issuer = cert.getIssuerDN().getName();
		this.notAfter = cert.getNotAfter().toString();
		this.notBefore = cert.getNotBefore().toString();
		this.subject = cert.getSubjectDN().getName();
		try {
			if (cert.getSubjectAlternativeNames() != null) {
				cert.getSubjectAlternativeNames().forEach(n -> alternateSubject.add(String.valueOf(n)));
			}
		}catch(CertificateParsingException e) {
			System.err.println(e);
		}
		
	}
	
	public List<String> getAlternateSubject() {
		return alternateSubject;
	}

	public void setAlternateSubject(List<String> alternateSubject) {
		this.alternateSubject = alternateSubject;
	}

	public String getSerialNumber() {
		return serialNumber;
	}
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	public String getIssuer() {
		return issuer;
	}
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getNotAfter() {
		return notAfter;
	}

	public void setNotAfter(String notAfter) {
		this.notAfter = notAfter;
	}

	public String getNotBefore() {
		return notBefore;
	}

	public void setNotBefore(String notBefore) {
		this.notBefore = notBefore;
	}

	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	
}
