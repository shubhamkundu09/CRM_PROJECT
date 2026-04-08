package com.crm.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class DecryptDeserializer extends JsonDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String encryptedValue = p.getValueAsString();

        if (encryptedValue == null || encryptedValue.trim().isEmpty()) {
            return null;
        }

        try {
            return CryptoUtil.decryptToLong(encryptedValue);
        } catch (Exception e) {
            throw new IOException("Failed to decrypt ID: " + encryptedValue, e);
        }
    }
}