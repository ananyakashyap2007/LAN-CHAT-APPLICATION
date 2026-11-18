package chat.util;
import java.io.InputStream;
import java.util.Properties;
public final class Config {
 private static final Properties P = new Properties();
 static { try (InputStream in=Config.class.getClassLoader().getResourceAsStream("application.properties")) { if(in!=null) P.load(in); } catch(Exception e){ throw new ExceptionInInitializerError(e); } }
 public static String get(String k,String d){ return P.getProperty(k,d).trim(); }
 public static int port(){ return Integer.parseInt(get("server.port","5000")); }
 private Config(){}
}