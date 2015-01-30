import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * THis class is used to interact with RPlay
 * @author LeanderK
 * @version 1.0
 */
public class RPlay implements AutoCloseable{
    private final String password;
    private final String name;
    private LaunchThread launchThread;
    // declare the work queue
    private ExecutorService workQueue = Executors.newCachedThreadPool();
    private List<Consumer<Socket>> onConnect = new ArrayList<Consumer<Socket>>();

    public RPlay(String name, String password) {
        this.password = password;
        this.name = name;
        if (password == null) {
            launchThread = new LaunchThread(name);
        } else {
            launchThread = new LaunchThread(name, password);
        }
        launchThread.setOnConnectCallback(new Consumer<Socket>() {
            @Override
            public void accept(Socket socket) {
                fireOnConnectObserver(socket);
            }
        });
    }

    public RPlay(String name) {
        this(name, null);
    }

    public void setWorkQueue(ExecutorService workQueue) {
        this.workQueue = workQueue;
    }

    /**
     * starts Airplay
     */
    public void start() {
        workQueue.execute(launchThread);
    }

    /**
     * adds an observer which will fire when RPlay connects to an Socket.
     * <p>
     * the argument will be the Socket.
     * </p>
     * @param observer the observer to add
     */
    public void addOnConnectObserver(Consumer<Socket> observer) {
        if (!onConnect.contains(observer))
            onConnect.add(observer);
    }

    /**
     * removes an Observer
     * @param observer the observer to remove
     * @return true if removed, false if not
     */
    public boolean removeObConnectObserver(Observer observer) {
        return onConnect.remove(observer);
    }

    private void fireOnConnectObserver(final Socket socket) {
        for (final Consumer<Socket> observer : onConnect) {
            workQueue.execute(new Runnable() {
                @Override
                public void run() {
                    observer.accept(socket);
                }
            });
        }
    }

    /**
     * closes RPlay
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        launchThread.stopThread();
    }
}
