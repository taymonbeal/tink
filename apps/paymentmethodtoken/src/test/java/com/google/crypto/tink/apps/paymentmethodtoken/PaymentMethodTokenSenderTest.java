// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.crypto.tink.apps.paymentmethodtoken;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.crypto.tink.subtle.EllipticCurves;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@code PaymentMethodTokenSender}. */
@RunWith(JUnit4.class)
public class PaymentMethodTokenSenderTest {
  private static final String MERCHANT_PUBLIC_KEY_BASE64 =
      "BOdoXP+9Aq473SnGwg3JU1aiNpsd9vH2ognq4PtDtlLGa3Kj8TPf+jaQNPyDSkh3JUhiS0KyrrlWhAgNZKHYF2Y=";
  /**
   * Created with:
   *
   * <pre>
   * openssl pkcs8 -topk8 -inform PEM -outform PEM -in merchant-key.pem -nocrypt
   * </pre>
   */
  private static final String MERCHANT_PRIVATE_KEY_PKCS8_BASE64 =
      "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgCPSuFr4iSIaQprjj"
          + "chHPyDu2NXFe0vDBoTpPkYaK9dehRANCAATnaFz/vQKuO90pxsINyVNWojabHfbx"
          + "9qIJ6uD7Q7ZSxmtyo/Ez3/o2kDT8g0pIdyVIYktCsq65VoQIDWSh2Bdm";

  /** Sample Google provided JSON with its public signing keys. */
  private static final String GOOGLE_VERIFYING_PUBLIC_KEYS_JSON =
      "{\n"
          + "  \"keys\": [\n"
          + "    {\n"
          + "      \"keyValue\": \"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEPYnHwS8uegWAewQtlxizmLFynw"
          + "HcxRT1PK07cDA6/C4sXrVI1SzZCUx8U8S0LjMrT6ird/VW7be3Mz6t/srtRQ==\",\n"
          + "      \"protocolVersion\": \"ECv1\"\n"
          + "    },\n"
          + "    {\n"
          + "      \"keyValue\": \"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE/1+3HBVSbdv+j7NaArdgMyoSAM"
          + "43yRydzqdg1TxodSzA96Dj4Mc1EiKroxxunavVIvdxGnJeFViTzFvzFRxyCw==\",\n"
          + "      \"protocolVersion\": \"ECv2\"\n"
          + "    },\n"
          + "  ],\n"
          + "}";

  /**
   * Sample Google private signing key for the ECv1 protocolVersion.
   *
   * <p>Corresponds to the ECv1 private key of the key in {@link
   * #GOOGLE_VERIFYING_PUBLIC_KEYS_JSON}.
   */
  private static final String GOOGLE_SIGNING_EC_V1_PRIVATE_KEY_PKCS8_BASE64 =
      "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgZj/Dldxz8fvKVF5O"
          + "TeAtK6tY3G1McmvhMppe6ayW6GahRANCAAQ9icfBLy56BYB7BC2XGLOYsXKfAdzF"
          + "FPU8rTtwMDr8LixetUjVLNkJTHxTxLQuMytPqKt39Vbtt7czPq3+yu1F";

  /**
   * Sample Google private signing key for the ECv2 protocolVersion.
   *
   * <p>Corresponds to ECv2 private key of the key in {@link #GOOGLE_VERIFYING_PUBLIC_KEYS_JSON}.
   */
  private static final String GOOGLE_SIGNING_EC_V2_PRIVATE_KEY_PKCS8_BASE64 =
      "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgKvEdSS8f0mjTCNKev"
          + "aKXIzfNC5b4A104gJWI9TsLIMqhRANCAAT/X7ccFVJt2/6Ps1oCt2AzKhIAz"
          + "jfJHJ3Op2DVPGh1LMD3oOPgxzUSIqujHG6dq9Ui93Eacl4VWJPMW/MVHHIL";

  /**
   * Sample Google intermediate public signing key for the ECv2 protocolVersion.
   *
   * <p>Base64 version of the public key encoded in ASN.1 type SubjectPublicKeyInfo defined in the
   * X.509 standard.
   *
   * <p>The intermediate public key will be signed by {@link
   * #GOOGLE_SIGNING_EC_V2_PRIVATE_KEY_PKCS8_BASE64}.
   */
  private static final String GOOGLE_SIGNING_EC_V2_INTERMEDIATE_PUBLIC_KEY_X509_BASE64 =
      "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE/1+3HBVSbdv+j7NaArdgMyoSAM43yR"
          + "ydzqdg1TxodSzA96Dj4Mc1EiKroxxunavVIvdxGnJeFViTzFvzFRxyCw==";

  /**
   * Sample Google intermediate private signing key for the ECv2 protocolVersion.
   *
   * <p>Corresponds to private key of the key in {@link
   * #GOOGLE_SIGNING_EC_V2_INTERMEDIATE_PUBLIC_KEY_X509_BASE64}.
   */
  private static final String GOOGLE_SIGNING_EC_V2_INTERMEDIATE_PRIVATE_KEY_PKCS8_BASE64 =
      "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgKvEdSS8f0mjTCNKev"
          + "aKXIzfNC5b4A104gJWI9TsLIMqhRANCAAT/X7ccFVJt2/6Ps1oCt2AzKhIAz"
          + "jfJHJ3Op2DVPGh1LMD3oOPgxzUSIqujHG6dq9Ui93Eacl4VWJPMW/MVHHIL";

  private static final String RECIPIENT_ID = "someRecipient";

  @Test
  public void testV1WithPrecomputedKeys() throws Exception {
    PaymentMethodTokenSender sender =
        new PaymentMethodTokenSender.Builder()
            .senderSigningKey(GOOGLE_SIGNING_EC_V1_PRIVATE_KEY_PKCS8_BASE64)
            .recipientId(RECIPIENT_ID)
            .rawUncompressedRecipientPublicKey(MERCHANT_PUBLIC_KEY_BASE64)
            .build();

    PaymentMethodTokenRecipient recipient =
        new PaymentMethodTokenRecipient.Builder()
            .senderVerifyingKeys(GOOGLE_VERIFYING_PUBLIC_KEYS_JSON)
            .recipientId(RECIPIENT_ID)
            .addRecipientPrivateKey(MERCHANT_PRIVATE_KEY_PKCS8_BASE64)
            .build();

    String plaintext = "blah";
    assertEquals(plaintext, recipient.unseal(sender.seal(plaintext)));
  }

  @Test
  public void testV2WithPrecomputedKeys() throws Exception {
    PaymentMethodTokenSender sender =
        new PaymentMethodTokenSender.Builder()
            .protocolVersion(PaymentMethodTokenConstants.PROTOCOL_VERSION_EC_V2)
            .senderIntermediateSigningKey(
                GOOGLE_SIGNING_EC_V2_INTERMEDIATE_PRIVATE_KEY_PKCS8_BASE64)
            .senderIntermediateCert(
                new SenderIntermediateCertFactory.Builder()
                    .protocolVersion(PaymentMethodTokenConstants.PROTOCOL_VERSION_EC_V2)
                    .addSenderSigningKey(GOOGLE_SIGNING_EC_V2_PRIVATE_KEY_PKCS8_BASE64)
                    .senderIntermediateSigningKey(
                        GOOGLE_SIGNING_EC_V2_INTERMEDIATE_PUBLIC_KEY_X509_BASE64)
                    .expiration(Instant.now().plus(Duration.standardDays(1)).getMillis())
                    .build()
                    .create())
            .recipientId(RECIPIENT_ID)
            .rawUncompressedRecipientPublicKey(MERCHANT_PUBLIC_KEY_BASE64)
            .build();

    PaymentMethodTokenRecipient recipient =
        new PaymentMethodTokenRecipient.Builder()
            .protocolVersion(PaymentMethodTokenConstants.PROTOCOL_VERSION_EC_V2)
            .senderVerifyingKeys(GOOGLE_VERIFYING_PUBLIC_KEYS_JSON)
            .recipientId(RECIPIENT_ID)
            .addRecipientPrivateKey(MERCHANT_PRIVATE_KEY_PKCS8_BASE64)
            .build();

    String plaintext = "blah";
    assertEquals(plaintext, recipient.unseal(sender.seal(plaintext)));
  }

  @Test
  public void testShouldThrowWithUnsupportedProtocolVersion() throws Exception {
    try {
      new PaymentMethodTokenSender.Builder().protocolVersion("ECv99").build();
      fail("Should have thrown!");
    } catch (IllegalArgumentException expected) {
      assertEquals("invalid version: ECv99", expected.getMessage());
    }
  }

  @Test
  public void testShouldThrowIfSignedIntermediateSigningKeyIsSetForV1() throws Exception {
    try {
      new PaymentMethodTokenSender.Builder()
          .protocolVersion(PaymentMethodTokenConstants.PROTOCOL_VERSION_EC_V1)
          .senderSigningKey(GOOGLE_SIGNING_EC_V1_PRIVATE_KEY_PKCS8_BASE64)
          .senderIntermediateCert(newSignedIntermediateSigningKey())
          .build();
      fail("Should have thrown!");
    } catch (IllegalArgumentException expected) {
      assertEquals(
          "must not set signed sender's intermediate signing key using "
              + "Builder.senderIntermediateCert",
          expected.getMessage());
    }
  }

  @Test
  public void testShouldThrowIfIntermediateSigningKeyIsSetForV1() throws Exception {
    try {
      new PaymentMethodTokenSender.Builder()
          .protocolVersion(PaymentMethodTokenConstants.PROTOCOL_VERSION_EC_V1)
          .senderSigningKey(GOOGLE_SIGNING_EC_V1_PRIVATE_KEY_PKCS8_BASE64)
          .senderIntermediateSigningKey(GOOGLE_SIGNING_EC_V2_INTERMEDIATE_PRIVATE_KEY_PKCS8_BASE64)
          .build();
      fail("Should have thrown!");
    } catch (IllegalArgumentException expected) {
      assertEquals(
          "must not set sender's intermediate signing key using "
              + "Builder.senderIntermediateSigningKey",
          expected.getMessage());
    }
  }

  @Test
  public void testShouldThrowIfSenderSigningKeyIsSetForV2() throws Exception {
    try {
      new PaymentMethodTokenSender.Builder()
          .protocolVersion(PaymentMethodTokenConstants.PROTOCOL_VERSION_EC_V2)
          .senderSigningKey(GOOGLE_SIGNING_EC_V1_PRIVATE_KEY_PKCS8_BASE64)
          .build();
      fail("Should have thrown!");
    } catch (IllegalArgumentException expected) {
      assertEquals(
          "must not set sender's signing key using Builder.senderSigningKey",
          expected.getMessage());
    }
  }

  @Test
  public void testShouldThrowIfSignedIntermediateSigningKeyIsNotSetForV2() throws Exception {
    try {
      new PaymentMethodTokenSender.Builder()
          .protocolVersion(PaymentMethodTokenConstants.PROTOCOL_VERSION_EC_V2)
          // no calls to senderIntermediateCert
          .senderIntermediateSigningKey(GOOGLE_SIGNING_EC_V2_INTERMEDIATE_PRIVATE_KEY_PKCS8_BASE64)
          .build();
      fail("Should have thrown!");
    } catch (IllegalArgumentException expected) {
      assertEquals(
          "must set signed sender's intermediate signing key using "
              + "Builder.senderIntermediateCert",
          expected.getMessage());
    }
  }

  @Test
  public void testShouldThrowIfIntermediateSigningKeyIsNotSetForV2() throws Exception {
    try {
      new PaymentMethodTokenSender.Builder()
          .protocolVersion(PaymentMethodTokenConstants.PROTOCOL_VERSION_EC_V2)
          // no calls to senderIntermediateSigningKey
          .build();
      fail("Should have thrown!");
    } catch (IllegalArgumentException expected) {
      assertEquals(
          "must set sender's intermediate signing key using Builder.senderIntermediateSigningKey",
          expected.getMessage());
    }
  }

  @Test
  public void testShouldThrowIfSenderSigningKeyIsNotSetForV1() throws Exception {
    try {
      new PaymentMethodTokenSender.Builder()
          .protocolVersion(PaymentMethodTokenConstants.PROTOCOL_VERSION_EC_V1)
          // no calls to senderSigningKey
          .build();
      fail("Should have thrown!");
    } catch (IllegalArgumentException expected) {
      assertEquals(
          "must set sender's signing key using Builder.senderSigningKey", expected.getMessage());
    }
  }

  @Test
  public void testSendReceive() throws Exception {
    ECParameterSpec spec = EllipticCurves.getNistP256Params();
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    keyGen.initialize(spec);
    String senderId = "foo";
    String recipientId = "bar";

    KeyPair senderKey = keyGen.generateKeyPair();
    ECPublicKey senderPublicKey = (ECPublicKey) senderKey.getPublic();
    ECPrivateKey senderPrivateKey = (ECPrivateKey) senderKey.getPrivate();

    KeyPair recipientKey = keyGen.generateKeyPair();
    ECPublicKey recipientPublicKey = (ECPublicKey) recipientKey.getPublic();
    ECPrivateKey recipientPrivateKey = (ECPrivateKey) recipientKey.getPrivate();

    PaymentMethodTokenSender sender =
        new PaymentMethodTokenSender.Builder()
            .senderId(senderId)
            .senderSigningKey(senderPrivateKey)
            .recipientId(recipientId)
            .recipientPublicKey(recipientPublicKey)
            .build();

    PaymentMethodTokenRecipient recipient =
        new PaymentMethodTokenRecipient.Builder()
            .senderId(senderId)
            .addSenderVerifyingKey(senderPublicKey)
            .recipientId(recipientId)
            .addRecipientPrivateKey(recipientPrivateKey)
            .build();

    String plaintext = "blah";
    assertEquals(plaintext, recipient.unseal(sender.seal(plaintext)));
  }

  @Test
  public void testWithKeysGeneratedByRecipientKeyGen() throws Exception {
    ECParameterSpec spec = EllipticCurves.getNistP256Params();
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    keyGen.initialize(spec);
    String senderId = "foo";
    String recipientId = "bar";

    KeyPair senderKey = keyGen.generateKeyPair();
    ECPublicKey senderPublicKey = (ECPublicKey) senderKey.getPublic();
    ECPrivateKey senderPrivateKey = (ECPrivateKey) senderKey.getPrivate();

    // The keys here are generated by PaymentMethodTokenRecipientKeyGen.
    String recipientPrivateKey =
        "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAP5/1502pXYdMion22yiWK"
            + "GoTBJN/wAAfdjBU6puyEMw==";
    String recipientPublicKey =
        "BJ995jnw2Ppn4BMP/ZKtlTOOIBQC+/L3PDcFRjowZuCkRqUZ/kGWE8c+zimZNHOZPzLB"
            + "NVGJ3V8M/fM4g4o02Mc=";

    PaymentMethodTokenSender sender =
        new PaymentMethodTokenSender.Builder()
            .senderId(senderId)
            .senderSigningKey(senderPrivateKey)
            .recipientId(recipientId)
            .rawUncompressedRecipientPublicKey(recipientPublicKey)
            .build();

    PaymentMethodTokenRecipient recipient =
        new PaymentMethodTokenRecipient.Builder()
            .senderId(senderId)
            .addSenderVerifyingKey(senderPublicKey)
            .recipientId(recipientId)
            .addRecipientPrivateKey(recipientPrivateKey)
            .build();

    String plaintext = "blah";
    assertEquals(plaintext, recipient.unseal(sender.seal(plaintext)));
  }

  private String newSignedIntermediateSigningKey() throws GeneralSecurityException {
    return new SenderIntermediateCertFactory.Builder()
        .protocolVersion(PaymentMethodTokenConstants.PROTOCOL_VERSION_EC_V2)
        .addSenderSigningKey(GOOGLE_SIGNING_EC_V2_PRIVATE_KEY_PKCS8_BASE64)
        .senderIntermediateSigningKey(GOOGLE_SIGNING_EC_V2_INTERMEDIATE_PUBLIC_KEY_X509_BASE64)
        .expiration(Instant.now().plus(Duration.standardDays(1)).getMillis())
        .build()
        .create();
  }
}