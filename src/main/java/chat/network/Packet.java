package chat.network;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Simple framed protocol: UTF-8 command fields followed by optional binary payload. */
public record Packet(String type,String a,String b,String c,byte[] data) {
 public static void write(DataOutputStream out, Packet p) throws IOException {
  out.writeUTF(p.type==null?"":p.type); out.writeUTF(p.a==null?"":p.a); out.writeUTF(p.b==null?"":p.b); out.writeUTF(p.c==null?"":p.c);
  byte[] d=p.data==null?new byte[0]:p.data; out.writeInt(d.length); out.write(d); out.flush();
 }
 public static Packet read(DataInputStream in) throws IOException {
  String t=in.readUTF(),a=in.readUTF(),b=in.readUTF(),c=in.readUTF(); int n=in.readInt();
  if(n<0||n>10*1024*1024) throw new IOException("Invalid payload size");
  byte[] d=in.readNBytes(n); if(d.length!=n) throw new EOFException("Incomplete payload");
  return new Packet(t,a,b,c,d);
 }
}