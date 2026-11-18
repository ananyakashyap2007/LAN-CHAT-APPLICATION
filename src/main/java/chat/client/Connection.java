package chat.client;
import chat.network.Packet;import java.net.*;import java.io.*;import java.util.function.Consumer;
public class Connection implements Closeable {
 private final Socket socket;private final DataInputStream in;private final DataOutputStream out;private volatile boolean running=true; private volatile Consumer<Packet> listener;
 public Connection(String host,int port,Consumer<Packet> listener)throws IOException{
  this.listener=listener; socket=new Socket();socket.connect(new InetSocketAddress(host,port),4000);
  in=new DataInputStream(new BufferedInputStream(socket.getInputStream()));out=new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
  Thread reader=new Thread(()->{try{while(running)this.listener.accept(Packet.read(in));}catch(Exception e){if(running)this.listener.accept(new Packet("ERROR","Connection lost: "+e.getMessage(),"","",null));}},"lan-reader");reader.setDaemon(true);reader.start();
 }
 public void setListener(Consumer<Packet> listener){this.listener=listener;}
 public synchronized void send(String t,String a,String b,String c,byte[] data)throws IOException{Packet.write(out,new Packet(t,a,b,c,data));}
 public void close(){running=false;try{send("LOGOUT","","","",null);}catch(Exception ignored){}try{socket.close();}catch(Exception ignored){}}
}