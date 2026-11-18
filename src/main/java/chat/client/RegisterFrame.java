package chat.client;
import chat.network.Packet;import javax.swing.*;import java.awt.*;
public class RegisterFrame extends JFrame {
 JTextField user=new JTextField();JPasswordField pass=new JPasswordField(),confirm=new JPasswordField();
 public RegisterFrame(String host,int port){super("LAN CONNECT | Create Account");setSize(420,480);setLocationRelativeTo(null);JPanel p=new JPanel();p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createEmptyBorder(30,45,30,45));p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
  JLabel t=new JLabel("LAN CONNECT");t.setFont(new Font("Arial",Font.BOLD,26));t.setForeground(new Color(18,43,78));t.setAlignmentX(.5f);p.add(t);JLabel h=new JLabel("Create Account");h.setFont(new Font("Arial",Font.BOLD,20));h.setAlignmentX(.5f);p.add(h);p.add(Box.createVerticalStrut(25));
  LoginFrame.addField(p,"Username",user);LoginFrame.addField(p,"Password",pass);LoginFrame.addField(p,"Confirm Password",confirm);
  JButton create=LoginFrame.button("CREATE ACCOUNT");create.addActionListener(e->{if(user.getText().isBlank()||pass.getPassword().length==0){JOptionPane.showMessageDialog(this,"Complete all fields");return;}if(!java.util.Arrays.equals(pass.getPassword(),confirm.getPassword())){JOptionPane.showMessageDialog(this,"Passwords do not match");return;}
   try{Connection c=new Connection(host,port,pkt->SwingUtilities.invokeLater(()->{if(pkt.type().equals("OK")){JOptionPane.showMessageDialog(this,pkt.a());new LoginFrame().setVisible(true);dispose();}else if(pkt.type().equals("ERROR"))JOptionPane.showMessageDialog(this,pkt.a());}));c.send("REGISTER",user.getText().trim(),new String(pass.getPassword()),"",null);}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage());}
  });p.add(create);add(p);
 }
}