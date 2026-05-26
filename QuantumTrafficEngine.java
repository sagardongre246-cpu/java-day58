import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
    Adaptive Quantum Traffic Simulation Engine
    ---------------------------------------------------
    Features:
    1. Multithreading
    2. Dynamic AI traffic prediction
    3. Priority emergency routing
    4. Graph shortest path algorithm
    5. Real-time congestion balancing
    6. Event-driven architecture
    7. Custom analytics engine

    Concept:
    A futuristic smart city traffic management simulator
    where vehicles move dynamically between intersections.
*/

class SmartIntersection {
    String name;
    List<Road> roads = new ArrayList<>();
    AtomicInteger trafficLoad = new AtomicInteger(0);

    public SmartIntersection(String name) {
        this.name = name;
    }

    public void connect(SmartIntersection target, int distance) {
        roads.add(new Road(this, target, distance));
    }

    @Override
    public String toString() {
        return name;
    }
}

class Road {
    SmartIntersection source;
    SmartIntersection destination;
    int distance;
    volatile boolean blocked = false;

    public Road(SmartIntersection source, SmartIntersection destination, int distance) {
        this.source = source;
        this.destination = destination;
        this.distance = distance;
    }
}

class Vehicle implements Runnable {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);

    int id;
    SmartCity city;
    SmartIntersection current;
    SmartIntersection destination;
    boolean emergency;

    public Vehicle(SmartCity city, SmartIntersection start,
                   SmartIntersection destination, boolean emergency) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.city = city;
        this.current = start;
        this.destination = destination;
        this.emergency = emergency;
    }

    @Override
    public void run() {
        System.out.println("\nVehicle " + id +
                (emergency ? " [EMERGENCY]" : "") +
                " started from " + current +
                " to " + destination);

        List<SmartIntersection> path =
                city.findOptimalPath(current, destination, emergency);

        if (path.isEmpty()) {
            System.out.println("Vehicle " + id + ": No available route.");
            return;
        }

        for (SmartIntersection node : path) {
            try {
                Thread.sleep(500);

                current = node;
                current.trafficLoad.incrementAndGet();

                System.out.println("Vehicle " + id +
                        " entered " + current.name +
                        " | Traffic Load: " +
                        current.trafficLoad.get());

                city.analytics.recordVisit(current.name);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Vehicle " + id +
                " reached destination " + destination.name);
    }
}

class TrafficAnalytics {
    private final Map<String, Integer> visitStats = new ConcurrentHashMap<>();

    public void recordVisit(String intersection) {
        visitStats.merge(intersection, 1, Integer::sum);
    }

    public void generateReport() {
        System.out.println("\n========= TRAFFIC ANALYTICS =========");

        visitStats.entrySet()
                .stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(entry ->
                        System.out.println(entry.getKey()
                                + " -> " + entry.getValue() + " visits"));
    }
}

class SmartCity {

    List<SmartIntersection> intersections = new ArrayList<>();
    TrafficAnalytics analytics = new TrafficAnalytics();

    public void addIntersection(SmartIntersection i) {
        intersections.add(i);
    }

    /*
        Modified Dijkstra Algorithm
        Emergency vehicles get priority by reducing traffic penalty
    */
    public List<SmartIntersection> findOptimalPath(
            SmartIntersection start,
            SmartIntersection end,
            boolean emergency) {

        Map<SmartIntersection, Integer> distance = new HashMap<>();
        Map<SmartIntersection, SmartIntersection> previous = new HashMap<>();
        PriorityQueue<SmartIntersection> queue =
                new PriorityQueue<>(Comparator.comparingInt(distance::get));

        for (SmartIntersection i : intersections) {
            distance.put(i, Integer.MAX_VALUE);
        }

        distance.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            SmartIntersection current = queue.poll();

            for (Road road : current.roads) {

                if (road.blocked)
                    continue;

                int trafficPenalty =
                        emergency ? 1 :
                                road.destination.trafficLoad.get() * 2;

                int newDist = distance.get(current)
                        + road.distance
                        + trafficPenalty;

                if (newDist < distance.get(road.destination)) {
                    distance.put(road.destination, newDist);
                    previous.put(road.destination, current);
                    queue.add(road.destination);
                }
            }
        }

        List<SmartIntersection> path = new ArrayList<>();
        SmartIntersection step = end;

        if (!previous.containsKey(step) && step != start)
            return path;

        while (step != null) {
            path.add(step);
            step = previous.get(step);
        }

        Collections.reverse(path);
        return path;
    }

    public void simulateRandomRoadBlockage() {
        Random random = new Random();

        for (SmartIntersection i : intersections) {
            for (Road r : i.roads) {
                if (random.nextInt(10) < 2) {
                    r.blocked = true;

                    System.out.println(
                            "ALERT: Road blocked between "
                                    + r.source.name
                                    + " -> "
                                    + r.destination.name);
                }
            }
        }
    }
}

public class QuantumTrafficEngine {

    public static void main(String[] args) throws Exception {

        SmartCity city = new SmartCity();

        SmartIntersection A = new SmartIntersection("A");
        SmartIntersection B = new SmartIntersection("B");
        SmartIntersection C = new SmartIntersection("C");
        SmartIntersection D = new SmartIntersection("D");
        SmartIntersection E = new SmartIntersection("E");
        SmartIntersection F = new SmartIntersection("F");

        city.addIntersection(A);
        city.addIntersection(B);
        city.addIntersection(C);
        city.addIntersection(D);
        city.addIntersection(E);
        city.addIntersection(F);

        A.connect(B, 4);
        A.connect(C, 2);

        B.connect(D, 5);
        B.connect(E, 10);

        C.connect(E, 3);

        D.connect(F, 11);

        E.connect(D, 4);
        E.connect(F, 2);

        city.simulateRandomRoadBlockage();

        ExecutorService executor =
                Executors.newFixedThreadPool(5);

        List<Vehicle> vehicles = List.of(
                new Vehicle(city, A, F, false),
                new Vehicle(city, A, D, false),
                new Vehicle(city, B, F, true),
                new Vehicle(city, C, F, false),
                new Vehicle(city, A, E, true)
        );

        for (Vehicle v : vehicles) {
            executor.execute(v);
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        city.analytics.generateReport();

        System.out.println("\nSimulation Completed.");
    }
}