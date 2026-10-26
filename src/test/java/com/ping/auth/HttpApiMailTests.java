package com.ping.auth;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.mail.*;
import static org.assertj.core.api.Assertions.*;
/** Brevo-compatible HTTPS mail adapter, exercised against a loopback stub (no external request). */
class HttpApiMailTests {
 HttpServer server;final AtomicReference<String> body=new AtomicReference<>(),key=new AtomicReference<>();int status=201;
 @BeforeEach void start()throws Exception{server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/v3/smtp/email",exchange->{
  key.set(exchange.getRequestHeaders().getFirst("api-key"));body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
  exchange.sendResponseHeaders(status,-1);exchange.close();});server.start();}
 @AfterEach void stop(){server.stop(0);}
 HttpApiMail mail(){return new HttpApiMail("http://127.0.0.1:"+server.getAddress().getPort()+"/v3/smtp/email","clave-sintetica","PING demo");}
 SimpleMailMessage message(){var m=new SimpleMailMessage();m.setFrom("remitente@ejemplo.test");m.setTo("persona@ejemplo.test");m.setSubject("Verifica tu cuenta de PING");
  m.setText("Confirma tu correo. Enlace \"personal\":\nhttps://ping.example/account/verify?token=abc");return m;}
 @Test void sendsOneJsonMessageWithTheKeyOnlyInTheHeader(){
  mail().send(message());
  assertThat(key.get()).isEqualTo("clave-sintetica");
  assertThat(body.get()).contains("\"sender\":{\"name\":\"PING demo\",\"email\":\"remitente@ejemplo.test\"}","\"to\":[{\"email\":\"persona@ejemplo.test\"}]",
   "\"textContent\":\"Confirma tu correo. Enlace \\\"personal\\\":\\nhttps://ping.example/account/verify?token=abc\"").doesNotContain("clave-sintetica");
 }
 @Test void apiRejectionIsAMailFailureSoTheAppShowsItsCommonMessage(){
  status=401;
  assertThatThrownBy(()->mail().send(message())).isInstanceOf(MailSendException.class).hasMessageNotContaining("clave-sintetica");
 }
 @Test void configurationIsValidatedAtStartup(){
  assertThatThrownBy(()->new HttpApiMail("https://api.brevo.com/v3/smtp/email","","PING")).isInstanceOf(IllegalStateException.class);
  assertThatThrownBy(()->new HttpApiMail("http://api.brevo.com/v3/smtp/email","k","PING")).isInstanceOf(IllegalStateException.class);
 }
 @Test void controlCharactersAreEscaped(){assertThat(HttpApiMail.json("a\u0001b\\c")).isEqualTo("\"a\\u0001b\\\\c\"");}
}
