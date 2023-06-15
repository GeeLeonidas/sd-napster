package br.dev.gee.sdnapster.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;

public class MD5 {
    final byte[] hashValue;

    public MD5(@Nonnull byte[] binaryData) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        hashValue = md.digest(binaryData);
    }

    @Override
    public int hashCode() {
        return hashValue.hashCode();
    }

    @Override
    public boolean equals(Object arg0) {
        try {
            MD5 other = (MD5) arg0;
            return other.hashValue.equals(this.hashValue);
        } catch (Exception exception) {
            return false;
        }
    }
}
