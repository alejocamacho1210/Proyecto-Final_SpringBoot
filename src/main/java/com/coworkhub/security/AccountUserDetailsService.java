package com.coworkhub.security;

import com.coworkhub.persistence.UserAccountRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final UserAccountRepository accounts;
    public AccountUserDetailsService(UserAccountRepository accounts) { this.accounts = accounts; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return accounts.findByUsernameIgnoreCase(username).map(AccountUserDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
    }
}
