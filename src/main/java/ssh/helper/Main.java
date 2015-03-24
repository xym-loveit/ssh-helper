package ssh.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.common.util.NoCloseInputStream;
import org.apache.sshd.common.util.NoCloseOutputStream;

/**
 * 简易通过ssh远程登录linux，并执行命令的工具
 * @author guor
 * @date 2014年11月6日 下午7:58:19
 */
public class Main {
	Map<Integer, Server> servers = new HashMap<Integer, Server>();

	public Main() {
		try {
			XMLConfiguration config = new XMLConfiguration("server.xml");
			ConfigurationNode root = config.getRootNode();
			int len = root.getChildrenCount();// 服务器个数
			System.out.println("----------------------服务器列表-----------------------");
			for (int i = 0; i < len; i++) {
				ConfigurationNode node = root.getChild(i);
				int count = node.getAttributeCount();
				Map<String, String> attrs = new HashMap<String, String>();
				for (int j = 0; j < count; j++) {
					attrs.put(node.getAttribute(j).getName(), node.getAttribute(j).getValue().toString());
				}
				String name = attrs.get("name");
				String host = attrs.get("host");
				String user = attrs.get("user");
				String passwd = attrs.get("passwd");
				int port = Integer.valueOf(attrs.get("port"));
				System.out.println("\t" + i + ": host: " + host + " name: " + name);
				servers.put(i, new Server(name, host, user, passwd, port));
			}
			System.out.println("------------------------------------------------------");
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		Main m = new Main();
		System.out.print("请选择登录服务器编号：");
		Scanner scan = new Scanner(System.in);
		try {
			int no = scan.nextInt();
			while (no < 0 || no >= m.servers.size()) {
				System.out.println("错误的编号，请重新输入！");
				no = scan.nextInt();
			}
			m.login(no);
		} finally {
			scan.close();
		}
	}

	private void login(int no) throws Exception {
		Server server = servers.get(no);
		if (server == null) {
			return;
		}
		SshClient client = SshClient.setUpDefaultClient();
		try {
			client.start();
			ClientSession session = client.connect(server.user, server.host, server.port).await().getSession();
			try {
				session.addPasswordIdentity(server.passwd);
				session.auth().verify();

				ClientChannel channel = session.createShellChannel();
				try {
					channel.setIn(new NoCloseInputStream(System.in));
					channel.setOut(new NoCloseOutputStream(System.out));
					channel.setErr(new NoCloseOutputStream(System.err));
				} finally {
					channel.open().await();
					channel.waitFor(ClientChannel.CLOSED, 0);
				}
			} finally {
				session.close(false);
			}
		} finally {
			client.stop();
		}
	}
}

class Server {
	String name;

	String host;

	String user;

	String passwd;

	int port;

	public Server(String name, String host, String user, String passwd, int port) {
		this.name = name;
		this.host = host;
		this.user = user;
		this.passwd = passwd;
		this.port = port;
	}
}
