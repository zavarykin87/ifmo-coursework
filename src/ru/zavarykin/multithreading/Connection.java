package ru.zavarykin.multithreading;

import org.w3c.dom.ls.LSOutput;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Iterator;

public class Connection implements AutoCloseable {
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Thread reader;  // нить которая будет прослушивать каждое новое соединение
    private Message markerMessage;

    public Message getMarkerMessage() {
        return markerMessage;
    }

    public void setMarkerMessage(Message markerMessage) {
        this.markerMessage = markerMessage;
    }

    // конструктор для серверной части
    public Connection(Socket socket) throws IOException {
       this.socket = socket;
       output = new ObjectOutputStream(this.socket.getOutputStream());
       input = new ObjectInputStream(this.socket.getInputStream());
       reader = new Thread(()->{
           while (!reader.isInterrupted()){
               Message message = readMessage();
               setMarkerMessage(message);
               if(message.getText().equalsIgnoreCase("exit") || this.socket.isClosed()){
                   Server.connections.remove(this); // соединение удаляется из списка
               }
               else {
                   try {
                       Server.messages.put(message); // складывает входящие сообщения в очередь
                       System.out.println(message);
                   } catch (InterruptedException | NullPointerException e) {
                       close();
                   }
               }
           }
       });
       reader.start();
    }
    // консруктор соединения для клиентской части
    public Connection(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        output = new ObjectOutputStream(this.socket.getOutputStream());
        input = new ObjectInputStream(this.socket.getInputStream());
        reader = new Thread(()->{
            while (!reader.isInterrupted()){
                System.out.println("Входящее сообщение: " + readMessage());
            }
        });
        reader.start();
    }
    // отправка сообщений
    public void sendMessage(Message message) {
        message.setDateTime();
        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException e) {
            close();
        }
    }
    // чтение сообщений
    public Message readMessage() {
        Message message = null;
        try {
            message = (Message) input.readObject();
        } catch (IOException | ClassNotFoundException e) {
            close();
        }
        return message;
    }
    // закрыть поток и сокет
    public synchronized void close() {
        reader.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
