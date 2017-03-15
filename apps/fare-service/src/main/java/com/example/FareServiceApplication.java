package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@SpringBootApplication
public class FareServiceApplication {

	
	public static void main(String[] args) {
		SpringApplication.run(FareServiceApplication.class, args);
	}
}

@Configuration
@ConfigurationProperties(prefix = "fare.credentials")
class FareConfig {
	String user;
	String password;
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
}

@Configuration
@EnableWebSecurity
class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	FareConfig config;
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().
			anyRequest(). //
			fullyAuthenticated(). //
			and(). //
			httpBasic(). //
			and(). //
			csrf().disable();
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication().withUser(config.user).password(config.password).roles("USER");
	}
}
