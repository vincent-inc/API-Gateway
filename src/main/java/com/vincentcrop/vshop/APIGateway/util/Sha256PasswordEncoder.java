package com.vincentcrop.vshop.APIGateway.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class Sha256PasswordEncoder implements PasswordEncoder {
    public String encode(String rawPassword) {
        return DigestUtils.sha256Hex(rawPassword);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return DigestUtils.sha256Hex(rawPassword.toString());
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return DigestUtils.sha256Hex(rawPassword).equals(encodedPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return DigestUtils.sha256Hex(rawPassword.toString()).equals(encodedPassword);
    }

    public String patchPassword(String encodedOriginPassword, String newPassword) {
        String newEncodedPassword = this.encode(newPassword);
        if (encodedOriginPassword.equals(newEncodedPassword))
            return encodedOriginPassword;
        else
            return newEncodedPassword;
    }
}
