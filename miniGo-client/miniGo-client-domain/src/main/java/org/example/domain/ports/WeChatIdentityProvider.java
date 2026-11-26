package org.example.domain.ports;

import org.example.domain.valueobject.WeChatIdentity;

@FunctionalInterface
public interface WeChatIdentityProvider {

    WeChatIdentity exchange(String loginCode);
}
