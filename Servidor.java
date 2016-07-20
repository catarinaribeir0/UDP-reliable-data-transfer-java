package trabalhoredes;

/**
 *
 * @authors Catarina Ribeiro, Leonardo Cavalcante, Leonardo Portugal, Victor
 * Meireles
 *
 */
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.Arrays;
  
public class Servidor { 
    static final int CABECALHO = 4;
    static final int TAMANHO_PACOTE = 1000 + CABECALHO;
    static final int PORTA_SERVIDOR = 8002;
    static final int PORTA_ACK = 8003;
 
    //construtor
    public Servidor(int portaEntrada, int portaDestino, String caminho) {
        DatagramSocket socketEntrada, socketSaida;
        System.out.println("Servidor: porta de entrada: " + portaEntrada + ", " + "porta de destino: " + portaDestino + ".");
 
        int ultimoNumSeq = -1;
        int proxNumSeq = 0;  //proximo numero de sequencia
        boolean transferenciaCompleta = false;  //flag caso a transferencia nao for completa
 
        //criando sockets
        try {
            socketEntrada = new DatagramSocket(portaEntrada);
            socketSaida = new DatagramSocket();
            System.out.println("Servidor Conectado...");
            try {
                byte[] recebeDados = new byte[TAMANHO_PACOTE];
                DatagramPacket recebePacote = new DatagramPacket(recebeDados, recebeDados.length);
 
                FileOutputStream fos = null;
 
                while (!transferenciaCompleta) {
                    int i = 0;
                    socketEntrada.receive(recebePacote);
                    InetAddress enderecoIP = recebePacote.getAddress();
 
                    int numSeq = ByteBuffer.wrap(Arrays.copyOfRange(recebeDados, 0, CABECALHO)).getInt();
                    System.out.println("Servidor: Numero de sequencia recebido " + numSeq);
 
                    //se o pacote for recebido em ordem
                    if (numSeq == proxNumSeq) {
                        //se for ultimo pacote (sem dados), enviar ack de encerramento
                        if (recebePacote.getLength() == CABECALHO) {
                            byte[] pacoteAck = gerarPacote(-2);     //ack de encerramento
                            socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
                            transferenciaCompleta = true;
                            System.out.println("Servidor: Todos pacotes foram recebidos! Arquivo criado!");
                        } else {
                            proxNumSeq = numSeq + TAMANHO_PACOTE - CABECALHO;  //atualiza proximo numero de sequencia
                            byte[] pacoteAck = gerarPacote(proxNumSeq);
                                socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
                                System.out.println("Servidor: Ack enviado " + proxNumSeq);
                          }
 
                        //se for o primeiro pacote da transferencia 
                        if (numSeq == 0 && ultimoNumSeq == -1) {
                            //cria arquivo    
                            File arquivo = new File(caminho);
                            if (!arquivo.exists()) {
                                arquivo.createNewFile();
                            }
                            fos = new FileOutputStream(arquivo);
                        }
                        //escreve dados no arquivo
                        fos.write(recebeDados, CABECALHO, recebePacote.getLength() - CABECALHO);
 
                        ultimoNumSeq = numSeq; //atualiza o ultimo numero de sequencia enviado
                    } else {    //se pacote estiver fora de ordem, mandar duplicado
                        byte[] pacoteAck = gerarPacote(ultimoNumSeq);
                        socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
                        System.out.println("Servidor: Ack duplicado enviado " + ultimoNumSeq);
                    }
 
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            } finally {
                socketEntrada.close();
                socketSaida.close();
                System.out.println("Servidor: Socket de entrada fechado!");
                System.out.println("Servidor: Socket de saida fechado!");
            }
        } catch (SocketException e1) {
            e1.printStackTrace();
        }
    }
    //fim do construtor
 
    //gerar pacote de ACK
    public byte[] gerarPacote(int numAck) {
        byte[] numAckBytes = ByteBuffer.allocate(CABECALHO).putInt(numAck).array();
        ByteBuffer bufferPacote = ByteBuffer.allocate(CABECALHO);
        bufferPacote.put(numAckBytes);
        return bufferPacote.array();
    }
 
    public static void main(String[] args) {
        Scanner teclado = new Scanner(System.in);
        System.out.println("----------------------------------------------SERVIDOR----------------------------------------------");
        System.out.print("Digite o diretorio do arquivo a ser criado. (Ex: C:/Users/Diego/Documents/): ");
        String diretorio = teclado.nextLine();
        System.out.print("Digite o nome do arquivo a ser criado: (Ex: letra.txt): ");
        String nome = teclado.nextLine();
 
        Servidor servidor = new Servidor(PORTA_SERVIDOR, PORTA_ACK, diretorio + nome);
    }
}