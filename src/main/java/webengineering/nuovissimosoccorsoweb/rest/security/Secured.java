package webengineering.nuovissimosoccorsoweb.rest.security;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotazione per marcare i metodi REST che richiedono autenticazione JWT.
 * 
 * Uso:
 * @Secured
 * @GET
 * public Response getProtectedData() { ... }
 * 
 * @author YourName
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Secured {
}