package com.chatico.securitywebsocket.springsecuredsockets.transfer.user;


import com.chatico.securitywebsocket.springsecuredsockets.domain.User;
import org.springframework.security.core.GrantedAuthority;



import java.util.Collection;

public class CustomUserDetails extends User {

    private User user;

    public CustomUserDetails(User user, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
    }


    public User getUser() {
        return user;
    }
}