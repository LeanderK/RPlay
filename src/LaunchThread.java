import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


/**
 * LaunchThread class which starts services
 * @author bencall
 *
 */
public class LaunchThread extends Thread {
	private BonjourEmitter emitter;
	private	ServerSocket servSock = null;
	private String name;
	private String password;
	private boolean stopThread = false;
    private Consumer<Socket> onConnectCallback = null;
    private ExecutorService threadPool = Executors.newFixedThreadPool(1);

	/**
	 * Constructor
	 * @param name
	 */
	public LaunchThread(String name){
		super();
		this.name = name;
	}
	
	/**
	 * Constructor
	 * @param name
	 */
	public LaunchThread(String name, String pass){
		super();
		this.name = name;
		this.password = pass;
	}
	
	private byte[] getHardwareAdress() {
		byte[] hwAddr = null;
		
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while(networkInterfaces.hasMoreElements())
			{
				NetworkInterface network = networkInterfaces.nextElement();
				System.out.println("network: " + network);
				if(network.getHardwareAddress() != null) 
				{
					hwAddr = network.getHardwareAddress();
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		return hwAddr;
	}
	
	
	private String getStringHardwareAdress(byte[] hwAddr) {
	    StringBuilder sb = new StringBuilder();
	    
	    for (byte b : hwAddr)
	      sb.append(String.format("%02x", b));
	      
	    return sb.toString();
	}
	
	
	public void run() {
		System.out.println("starting service...");
		
		// Setup safe shutdown
		Runtime.getRuntime().addShutdownHook(new Thread() {
   			@Override
   			public void run() {
   				System.out.println("shutting down...");
   				
   				LaunchThread.this.stopThread();
   				
   				try {
					LaunchThread.this.emitter.stop();
	    			LaunchThread.this.servSock.close();
	    			
	    			System.out.println("service stopped.");
   				} catch (IOException e) {
   					//
   				}
   			}
  		});
				
		int port = 5000;
		
		try {
			// DNS Emitter (Bonjour)
			byte[] hwAddr = getHardwareAdress();
			try {
				// Check if password is set
				if(password == null)
					emitter = new BonjourEmitter(name, getStringHardwareAdress(hwAddr), port, false);
				else
					emitter = new BonjourEmitter(name, getStringHardwareAdress(hwAddr), port, true);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			
			System.out.println("announced [" + name + " @ " + getStringHardwareAdress(hwAddr) + "]");
			
			// We listen for new connections
			try {
				servSock = new ServerSocket(port);
			} catch (IOException e) {
				System.out.println("port busy, using default.");
				servSock = new ServerSocket();
			}
			
			servSock.setSoTimeout(1000);
			
			System.out.println("service started.");
						
			while (!stopThread) {
				try {
					Socket socket = servSock.accept();
					System.out.println("accepted connection from " + socket.toString());
                    if (onConnectCallback != null)
                        onConnectCallback.accept(socket);

					// Check if password is set
					if(password == null)
						new RTSPResponder(hwAddr, socket).start();
					else
						new RTSPResponder(hwAddr, socket, password).start();
				} catch(SocketTimeoutException e) {
					//
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
			
		} finally {
			try {
				emitter.stop(); 
				servSock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void stopThread() {
		stopThread = true;
	}

    /**
     * sets the Callback for accepting a new Connection
     * @param onConnectCallback the Callback
     */
    public void setOnConnectCallback(Consumer<Socket> onConnectCallback) {
        this.onConnectCallback = onConnectCallback;
    }
}
