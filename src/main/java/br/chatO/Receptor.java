package br.chatO;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.*;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import org.json.*;

public class Receptor {

	private static String queueName;
	private static String queueNameFiles;
	private static String prompt;
	private static int firstTime;

	public static void showPrompt(){

		System.out.print(prompt);

	}

	public static void startReceiving(
		String host,
		String username,
		String password
		) throws Exception 
{
	
	ConnectionFactory factory = new ConnectionFactory();
	factory.setHost(host); 
	factory.setUsername(username); 
	factory.setPassword(password); 
	factory.setVirtualHost("/");
	Connection connection = factory.newConnection();
	Channel channel = connection.createChannel();

	queueName = username;
	queueNameFiles = queueName + "Files";
	//System.out.println(queueNameFiles);

	channel.queueDeclare(queueName, false,   false,     false,       null);
	channel.queueDeclare(queueNameFiles, false,   false,     false,       null);

	Consumer consumer = new DefaultConsumer(channel) {
		public void handleDelivery(
				String consumerTag, 
				Envelope envelope, 
				AMQP.BasicProperties properties, 
				byte[] body) throws IOException 
		{

			Message.Mensagem mensagem = Message.Mensagem
				.parseFrom(body);

			String emissor = mensagem.getEmissor();
			String data = mensagem.getData();
			String hora = mensagem.getHora();
			String grupo = mensagem.getGrupo();

			Message.Conteudo conteudo = mensagem.getConteudo();
			String tipo = conteudo.getTipo();
			String nome = conteudo.getNome();
			byte[] corpo = conteudo.getCorpo().toByteArray();

			if(tipo.equals("text")){
				String corpoStr = new String(corpo);
				String toPrint = "";
				if(grupo.isEmpty())
					toPrint  = "(" + data + " às " + hora + ")"+" "+emissor+" diz: " + corpoStr;
				else
					toPrint  = "(" + data + " às " + hora + ")"+" "+emissor+" no grupo "+grupo+" diz: " + corpoStr;
				System.out.println("\n" + toPrint);
				if(!emissor.equals(username))
					showPrompt();
				else if(0 < firstTime){
					showPrompt();
					firstTime--;
				}
			} else {
				String toPrint  = "(" + data + " às " + hora + ")"+" Arquivo "+nome+" recebido de "+emissor+" !";
				System.out.println(toPrint);

				File file = new File(nome);
				Files.write(file.toPath(), corpo); //nao bloqueante
			}
				

		}
	};

	Scanner sc = new Scanner(System.in);
	firstTime = 1;
	prompt = ">> ";
	String destination = "";
	String grupo = "";
	String tipoMime = "";
	String nome = "";
	while(true){

		channel.basicConsume(queueName, true,    consumer);
		channel.basicConsume(queueNameFiles, true,    consumer);
			showPrompt();
		String message = sc.nextLine();

		if(message.charAt(0) == '!'){

			if(message.equals("!exit"))
				break;

			String[] fPart = message.split(" ");

			/* Vamos verificar que os lugares do vetor
			 * fPart são de fato acessíveis */
			
			try {

				if(fPart[0].equals("!"))
					throw new Exception("O token \"!\" precisa vir acompanhado de um comando.");

			if(fPart[0].equals("!addGroup")){

				if(fPart.length < 2)
					throw new Exception("O nome do grupo não foi especificado.");

				channel.exchangeDeclare(
						fPart[1], 
						"fanout");

				channel.queueBind(username, fPart[1], "");
			} else if(fPart[0].equals("!addUser")){

				if(fPart.length < 3)
					throw new Exception("O nome do usuário ou do grupo não foi especificado.");

				String queue = fPart[1];
				String exchange = fPart[2];
				channel.queueBind(queue, exchange, "");

			} else if(fPart[0].equals("!delFromGroup")){

				if(fPart.length < 3)
					throw new Exception("O nome do usuário ou do grupo não foi especificado.");

				String queue = fPart[1];
				String exchange = fPart[2];
				channel.queueUnbind(queue, exchange, "");

			} else if(fPart[0].equals("!removeGroup")){

				if(fPart.length < 2)
					throw new Exception("O nome do grupo não foi especificado.");

				String exchange = fPart[1];
				channel.exchangeDelete(exchange);

				grupo = "";
				prompt = ">> ";

			} else if(fPart[0].equals("!listUsers")){

				if(fPart.length < 2)
					throw new Exception("O nome do grupo não foi especificado.");

				String exchange = fPart[1];

				String authHeadArgs = username + ":" + password;
				String authHeadName = "Authorization";
				String authHeadValue = "Basic "+java.util.Base64.getEncoder()
					.encodeToString(authHeadArgs.getBytes());

				String restResource = "http://"+host+":15672";
				Client client = ClientBuilder.newClient();
				Response response = client.target(restResource)
					.path("/api/exchanges/%2f/"+exchange+"/bindings/source")
					.request(MediaType.APPLICATION_JSON)
					.header(authHeadName, authHeadValue)
					.get();

				if(response.getStatus() == 200){
					String json = response.readEntity(String.class);

					List<String> users = new ArrayList<>();
					JSONArray obj = new JSONArray(json);

					for(int i=0;i<obj.length();i++)
						if(obj.getJSONObject(i)
								.getString("destination_type")
								.equals("queue"))
							users.add(obj
									.getJSONObject(i)
									.getString("destination"));

					for(int i=0;i<users.size()-1;i++)
						System.out.print(users.get(i).toString()+", ");
					System.out.println(users.get(users.size()-1));

				} else 
					System.out.println(response.getStatus());
			} else if(fPart[0].equals("!listGroups")){

				String authHeadArgs = username + ":" + password;
				String authHeadName = "Authorization";
				String authHeadValue = "Basic "+java.util.Base64.getEncoder()
					.encodeToString(authHeadArgs.getBytes());

				String restResource = "http://"+host+":15672";
				Client client = ClientBuilder.newClient();
				Response response = client.target(restResource)
					.path("/api/queues/%2f/"+username+"/bindings")
					.request(MediaType.APPLICATION_JSON)
					.header(authHeadName, authHeadValue)
					.get();

				if(response.getStatus() == 200){
					String json = response.readEntity(String.class);

					List<String> groups = new ArrayList<>();
					JSONArray obj = new JSONArray(json);
					for(int i=0;i<obj.length();i++)
						if(!obj.getJSONObject(i)
								.getString("source")
								.isEmpty())
							groups.add(obj.getJSONObject(i)
									.getString("source"));

					for(int i=0;i<groups.size()-1;i++)
						System.out.print(groups.get(i)+", ");
					System.out.println(groups.get(groups.size()-1));

				} else 
					System.out.println(response.getStatus());

			} else if(fPart[0].equals("!upload")){

				if(fPart.length < 2)
					throw new Exception("O caminho do arquivo não foi especificado.");

				String path = fPart[1];
				Path source = Paths.get(path);
				tipoMime = Files.probeContentType(source);
				System.out.println(tipoMime);
				String[] aux = path.split("/");
				nome = aux[aux.length-1];

				File file = new File(path);

				byte[] fileBytes = Files.readAllBytes(file.toPath());
				
				sendFileThread thread = new sendFileThread(
					host, 
					username, 
					password, 
					destination, 
					grupo,
					fileBytes,
					tipoMime,
					nome,
					path
				);

				thread.run();

			} else 
				System.out.println("Comando não reconhecido.");

			} catch(Exception e){
				System.out.println(e.getMessage());
				continue;
			}

		} else if(message.charAt(0) == '@' 
				|| message.charAt(0) == '#'){

			tipoMime = "text";
			nome = "Mensagem";
			prompt = message + ">> ";

			if(message.charAt(0) == '@'){

				destination = message.substring(1, message.length());
				grupo = "";

			}

			if(message.charAt(0) == '#'){
				grupo = message.substring(1, message.length());
			}

		} else {
			Emissor.sendMessage(
				host, 
				username, 
				password, 
				destination, 
				grupo,
				message,
				tipoMime,
				nome
			);

		}

	}

	sc.close();

}

  public static void main(String[] argv) throws Exception{

	  if(argv.length < 3){

		  String str = "Uso: java -jar <jarfile> <host_ip> <usuario> <senha>";
		  System.out.println(str);
		  return;

	  }

	  String host = argv[0];
	  String username = argv[1];
	  String password = argv[2];

	  startReceiving(host, username, password);
	  System.exit(0);

  }

}

class sendFileThread extends Thread {
		private String host; 
		private String username; 
		private String password; 
		private String destination; 
		private String grupo;
		private byte[] fileBytes;
		private String tipoMime;
		private String nome;
		private String path;

	public sendFileThread (
		String host, 
		String username, 
		String password, 
		String destination, 
		String grupo,
		byte[] fileBytes,
		String tipoMime,
		String nome,
		String path
	) {

		this.host = host;
		this.username = username;
		this.password = password;
		this.destination = destination;
		this.grupo = grupo;
		this.fileBytes = fileBytes;
		this.tipoMime = tipoMime;
		this.nome = nome;
		this.path = path;
	
	}

	public void run() {

		System.out.println("Enviando \"" +
				path + "\" para "+ destination);

		try {
			Emissor.sendFile(
				host, 
				username, 
				password, 
				destination + "Files", 
				grupo,
				fileBytes,
				tipoMime,
				nome
			);
		} catch(Exception e){}

		System.out.println("Arquivo \""+
				path+"\" foi enviado para "+ destination);

	}

}
