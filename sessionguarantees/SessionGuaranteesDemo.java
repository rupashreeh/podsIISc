import java.util.HashMap;
import java.util.Map;

class VersionVector {
    // A map to hold server and their logical clocks
    private Map<String, Integer> vector;

    public VersionVector() {
        vector = new HashMap<>();
    }

    // Get the clock value for a server, return 0 if the server is not present
    public int get(String server) {
        return vector.getOrDefault(server, 0);
    }

    // Set the clock value for a server
    public void set(String server, int clock) {
        vector.put(server, clock);
    }

    // Update this version vector to be the maximum of itself and another vector
    public void merge(VersionVector other) {
        for (String server : other.vector.keySet()) {
            int currentClock = this.get(server);
            int otherClock = other.get(server);
            this.set(server, Math.max(currentClock, otherClock));
        }
    }

    // Check if this version vector dominates (is greater or equal to) another
    public boolean dominates(VersionVector other) {
        for (String server : other.vector.keySet()) {
            if (this.get(server) < other.get(server)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return vector.toString();
    }
}

// Session Manager to track the session's read and write vectors
class SessionManager {
    private VersionVector readVector;
    private VersionVector writeVector;

    public SessionManager() {
        readVector = new VersionVector();
        writeVector = new VersionVector();
    }

    public VersionVector getReadVector() {
        return readVector;
    }

    public VersionVector getWriteVector() {
        return writeVector;
    }

    public void updateReadVector(VersionVector serverVector) {
        readVector.merge(serverVector);
    }

    public void updateWriteVector(String server, int clock) {
        writeVector.set(server, clock);
    }
}

// Server class that holds its own version vector and processes reads and writes
class Server {
    private String serverName;
    private VersionVector versionVector;
    private int clock;  // Logical clock

    public Server(String serverName) {
        this.serverName = serverName;
        this.versionVector = new VersionVector();
        this.clock = 0;
    }

    // Simulate reading from this server
    public VersionVector read(SessionManager sessionManager, boolean MR, boolean RYW) {
        if (MR && !versionVector.dominates(sessionManager.getReadVector())) {
            throw new IllegalStateException("Monotonic Reads guarantee violated.");
        }

        if (RYW && !versionVector.dominates(sessionManager.getWriteVector())) {
            throw new IllegalStateException("Read Your Writes guarantee violated.");
        }

        // Return a copy of the current version vector to the session manager
        return versionVector;
    }

    // Simulate writing to this server
    public void write(SessionManager sessionManager, boolean WFR, boolean MW) {
        if (WFR && !versionVector.dominates(sessionManager.getReadVector())) {
            throw new IllegalStateException("Writes Follow Reads guarantee violated.");
        }

        if (MW && !versionVector.dominates(sessionManager.getWriteVector())) {
            throw new IllegalStateException("Monotonic Writes guarantee violated.");
        }

        // Increment the clock and update the server's version vector
        clock++;
        versionVector.set(serverName, clock);
        sessionManager.updateWriteVector(serverName, clock);
    }

    public String getName() {
        return serverName;
    }

    @Override
    public String toString() {
        return serverName + " " + versionVector.toString();
    }
}

// Testing the guarantees
public class SessionGuaranteesDemo {

    public static void main(String[] args) {
        // Create servers
        Server serverA = new Server("A");
        Server serverB = new Server("B");

        // Create a session manager
        SessionManager session = new SessionManager();

        try {
            // Perform write on server A
            System.out.println("Writing on Server A...");
            serverA.write(session, false, true); // MW is true
            System.out.println(serverA);

            // Perform a read from server A with Monotonic Reads (MR) and Read Your Writes (RYW)
            System.out.println("Reading from Server A...");
            VersionVector result = serverA.read(session, true, true); // MR and RYW are true
            session.updateReadVector(result);
            System.out.println("Read vector: " + session.getReadVector());

            // Perform write on server B
            System.out.println("Writing on Server B...");
            serverB.write(session, true, true); // WFR and MW are true
            System.out.println(serverB);

        } catch (IllegalStateException e) {
            System.out.println("Session Guarantee Violated: " + e.getMessage());
        }
    }
}
