# LAN CONNECT – LAN Chat Application

A Java 17 + Swing + TCP + MySQL desktop LAN chat project. One computer runs the server and MySQL; other computers run clients and connect to the server's IPv4 address.

## Requirements
- JDK 17
- Maven 3.8+
- MySQL 8+
- VS Code with Extension Pack for Java (or another Java IDE)

## 1. Database setup (server computer)
1. Start MySQL.
2. Open MySQL Workbench or the MySQL command line.
3. Run `database/schema.sql`.
4. Edit `src/main/resources/application.properties` and set `db.username` and `db.password`.
5. Keep MySQL accessible only to the server application. Client computers do not connect to MySQL.

## 2. Build
Open this folder in VS Code. In its terminal:
```powershell
mvn clean package
```

## 3. Start server
On the computer hosting MySQL:
```powershell
mvn exec:java -Dexec.mainClass=chat.server.Server
```
The server listens on TCP port 5000 by default. Ensure Windows Firewall allows inbound TCP 5000 on the private network.

Find the server IPv4 address with:
```powershell
ipconfig
```
Use the IPv4 address of the active Wi-Fi/Ethernet adapter (example `192.168.1.10`).

## 4. Start a client
On the server computer or another computer on the same LAN:
```powershell
mvn exec:java -Dexec.mainClass=chat.client.Client
```
Enter the server IPv4 address and port 5000 on the login screen. Register an account, then sign in. Do not enter `localhost` on a different computer.

## Features implemented
- Server-side registration and BCrypt password hashing.
- Login through the server.
- Multi-client TCP connections and online-user updates.
- Group broadcast and private text messages.
- Current server-session message storage; previous messages are cleared at server startup.
- Server-side file storage and recipient-authorized downloads (PDF, TXT, DOCX, JPG, JPEG, PNG; max 10 MB).
- Swing login, registration and chat UI; network work is handled off the Swing event thread.

## File transfer notes
Files are sent as binary data using a length-prefixed frame, not as text. The server checks extension and size, creates a random stored name, and only permits the sender or intended recipient (or group participants) to download.

## Demo checklist
1. Start MySQL and the Java server.
2. Open two client windows (or use two computers on the same Wi-Fi).
3. Register two users and log in.
4. Send a group message and verify both clients receive it.
5. Send a private message and verify a third user does not receive it.
6. Upload a supported file and download it from the intended conversation.
7. Log out and confirm the online list updates.

## Limitations / deployment cautions
- This is a teaching/demo LAN application, not an internet-facing secure messenger. Use only on a trusted private LAN.
- Current-session messages are intentionally not restored after restart.
- Group file sharing is supported by using a null recipient; private files are restricted to the sender and selected recipient.
- Test across the actual campus/home network before the demonstration; some Wi-Fi routers enable client isolation.
- Update MySQL credentials before running. Never commit real credentials to a public repository.
